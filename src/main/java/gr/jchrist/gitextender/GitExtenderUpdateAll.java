package gr.jchrist.gitextender;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.vcsUtil.VcsImplUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.configuration.GitExtenderSettingsHandler;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import gr.jchrist.gitextender.dialog.SelectModuleDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author jchrist
 * @since 2015/06/27
 */
public class GitExtenderUpdateAll extends AnAction {
    private static final Logger logger = Logger.getInstance(GitExtenderUpdateAll.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            Project project = event.getProject();
            if (project == null) {
                logger.warn("received event without project");
                NotificationUtil.showErrorNotification("Update Failed", "Git Extender failed to retrieve the project");
                return;
            }

            GitRepositoryManager manager = getGitRepositoryManager(project);

            if (manager == null) {
                NotificationUtil.showErrorNotification("Update Failed", "Git Extender could not initialize the project's repository manager");
                return;
            }

            List<GitRepository> repositoryList = manager.getRepositories();
            manager.updateAllRepositories();
            if (repositoryList.isEmpty()) {
                logger.info("no git repositories in project");
                NotificationUtil.showErrorNotification("Update Failed", "Git Extender could not find any repositories in the current project");
                return;
            }

            ProjectSettingsHandler projectSettingsHandler = new ProjectSettingsHandler(project);
            boolean proceedToUpdate = showSelectModuleDialog(projectSettingsHandler, repositoryList);
            if(!proceedToUpdate) {
                logger.debug("update cancelled");
                NotificationUtil.showInfoNotification("Update Canceled", "update was canceled");
                return;
            }

            List<GitRepository> reposToUpdate = getSelectedGitRepos(repositoryList,
                    projectSettingsHandler.loadSelectedModules());

            if (reposToUpdate.isEmpty()) {
                logger.debug("no modules selected in dialog");
                NotificationUtil.showInfoNotification("Update Canceled", "update was canceled");
                return;
            }

            updateRepositories(project, reposToUpdate);
        } catch (Exception | Error e) {
            logger.warn("error updating project due to exception", e);
            NotificationUtil.showErrorNotification("Git Extender Update Failed", "Git Extender failed to update the project due to exception: " + e);
        }
    }

    @Nullable
    public static GitRepositoryManager getGitRepositoryManager(@NotNull Project project) {
        try {
            VcsRepositoryManager vcsManager = project.getComponent(VcsRepositoryManager.class);
            if (vcsManager == null) {
                logger.warn("no vcs manager returned for project: " + project);
                return null;
            }

            return new GitRepositoryManager(project, vcsManager);
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

        final AtomicInteger countDown = new AtomicInteger(repositories.size());

        //get the settings to find out the selected options
        GitExtenderSettingsHandler settingsHandler = new GitExtenderSettingsHandler();
        final GitExtenderSettings settings = settingsHandler.loadSettings();
        repositories.forEach(repo ->
                new BackgroundableRepoUpdateTask(repo,
                        VcsImplUtil.getShortVcsRootName(repo.getProject(), repo.getRoot()),
                        settings, countDown, accessToken)
                        .queue());
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
}
