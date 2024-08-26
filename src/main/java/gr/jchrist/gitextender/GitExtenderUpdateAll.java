package gr.jchrist.gitextender;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.Repository;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.platform.ide.progress.TasksKt;
import com.intellij.vcsUtil.VcsImplUtil;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.configuration.GitExtenderSettingsHandler;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import gr.jchrist.gitextender.dialog.SelectModuleDialog;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author jchrist
 * @since 2015/06/27
 */
public class GitExtenderUpdateAll extends AnAction {
    private static final Logger logger = Logger.getInstance(GitExtenderUpdateAll.class);
    protected CountDownLatch updateCountDown;
    protected AtomicBoolean executingFlag = new AtomicBoolean(false);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (!this.executingFlag.compareAndSet(false, true)) {
            logger.warn("executing flag was true, another update action should be already running");
            return;
        }
        try {
            Project project = event.getProject();
            if (project == null) {
                logger.warn("received event without project");
                throw new CancelUpdateException("Update Failed", "Git Extender failed to retrieve the project");
            }

            GitRepositoryManager manager = getGitRepositoryManager(project);

            if (manager == null) {
                throw new CancelUpdateException("Update Failed", "Git Extender could not initialize the project's repository manager");
            }

            List<GitRepository> repositoryList = manager.getRepositories();
            if (repositoryList.isEmpty()) {
                logger.info("no git repositories in project");
                throw new CancelUpdateException("Update Failed", "Git Extender could not find any repositories in the current project");
            }

            ProjectSettingsHandler projectSettingsHandler = new ProjectSettingsHandler(project);
            boolean proceedToUpdate = showSelectModuleDialog(projectSettingsHandler, repositoryList);
            if (!proceedToUpdate) {
                logger.debug("update cancelled");
                throw new CancelUpdateException("Update Canceled", "update was canceled", NotificationType.INFORMATION);
            }

            List<GitRepository> reposToUpdate = getSelectedGitRepos(repositoryList,
                    projectSettingsHandler.loadSelectedModules());

            if (reposToUpdate.isEmpty()) {
                logger.debug("no modules selected in dialog");
                throw new CancelUpdateException("Update Canceled", "Update was canceled due to no repositories selected", NotificationType.INFORMATION);
            }

            updateRepositories(project, reposToUpdate);
        } catch (CancelUpdateException e) {
            logger.debug("cancel update exception received", e);
            NotificationUtil.showNotification(e.notificationTitle, e.notificationMessage, e.notificationType);
            executingFlag.set(false);
        } catch (Exception | Error e) {
            logger.warn("error updating project due to exception", e);
            NotificationUtil.showErrorNotification("Update Failed", "Git Extender failed to update the project due to exception: " + e);
            executingFlag.set(false);
        }
    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        // Set the availability based on whether we are already executing or not
        anActionEvent.getPresentation().setEnabled(!this.executingFlag.get());
    }

    @Nullable
    public static GitRepositoryManager getGitRepositoryManager(@NotNull Project project) {
        try {
            return GitRepositoryManager.getInstance(project);
        } catch (Exception e) {
            logger.warn("exception caught while trying to get git repository manager", e);
            return null;
        }
    }

    private boolean showSelectModuleDialog(@NotNull ProjectSettingsHandler projectSettingsHandler,
                                           @NotNull List<GitRepository> repositories) {
        if (repositories.size() <= 1) {
            logger.debug("single git repo in project, no need for displaying module chooser dialog");
            return true;
        }

        List<String> repos = repositories.stream()
                .map(GitExtenderUpdateAll::getRepoName)
                .sorted()
                .collect(Collectors.toList());
        SelectModuleDialog selectModuleDialog = new SelectModuleDialog(projectSettingsHandler, repos);
        return selectModuleDialog.showAndGet();
    }

    private void updateRepositories(@NotNull Project project, @NotNull List<GitRepository> repositories) {
        //get an access token for changing the repositories
        final AccessToken accessToken = DvcsUtil.workingTreeChangeStarted(project);

        //get the settings to find out the selected options
        GitExtenderSettingsHandler settingsHandler = new GitExtenderSettingsHandler();
        final GitExtenderSettings settings = settingsHandler.loadSettings();
        this.updateCountDown = new CountDownLatch(repositories.size());
        final var es = Executors.newFixedThreadPool(Math.min(repositories.size(), 3),
                new ThreadFactoryBuilder().setNameFormat("gitextender-repo-updater-%d").build());
        repositories.forEach(repo ->
                es.submit(new BackgroundableRepoUpdateTask(repo, VcsImplUtil.getShortVcsRootName(repo.getProject(), repo.getRoot()),
                        settings, updateCountDown)));
        TasksKt.withBackgroundProgress(project, "GitExtender Update All Repos", false, (cs, cont) -> {
            try {
                logger.debug("waiting for update to finish - " + Thread.currentThread().getName());
                //10 minutes should be extreme enough to account for ALL updates
                this.updateCountDown.await(10, TimeUnit.MINUTES);
            } catch (Exception e) {
                logger.warn("error awaiting update latch!", e);
            }
            try {
                es.shutdownNow();
            } catch (Exception e) {
                logger.warn("error shutting down executor", e);
            }
            logger.debug("update finished - " + Thread.currentThread().getName());
            //the last task finished should clean up, release project changes and show info notification
            accessToken.finish();

            // refresh vfs roots
            GitUtil.refreshVfsInRoots(repositories.stream().map(Repository::getRoot).collect(Collectors.toList()));

            NotificationUtil.showInfoNotification("Update Completed", "Git Extender updated all projects");
            executingFlag.set(false);
            return cont;
        }, new Continuation<>() {
            @Override
            public @NotNull CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NotNull Object o) {}
        });
    }

    protected static List<GitRepository> getSelectedGitRepos(List<GitRepository> repos, List<String> selectedModules) {
        if (repos.size() <= 1) {
            return repos;
        }

        return repos.stream()
                .filter(repo -> selectedModules.contains(getRepoName(repo)))
                .collect(Collectors.toList());
    }

    protected static String getRepoName(GitRepository repo) {
        return VcsImplUtil.getShortVcsRootName(repo.getProject(), repo.getRoot());
    }

    public static class CancelUpdateException extends Exception {
        protected final String notificationTitle;
        protected final String notificationMessage;
        protected final NotificationType notificationType;

        public CancelUpdateException(String notificationTitle, String notificationMessage) {
            this(notificationTitle, notificationMessage, NotificationType.ERROR);
        }

        public CancelUpdateException(
                String notificationTitle, String notificationMessage, NotificationType notificationType) {
            super(notificationMessage);
            this.notificationTitle = notificationTitle;
            this.notificationMessage = notificationMessage;
            this.notificationType = notificationType;
        }

        @Override public Throwable fillInStackTrace() { return this; }
    }
}
