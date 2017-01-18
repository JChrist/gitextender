package gr.jchrist.gitextender;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.vcsUtil.VcsImplUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jchrist
 * @since 2015/06/27
 */
public class GitExtenderUpdateAll extends AnAction {
    @Nullable
    public static GitRepositoryManager getGitRepositoryManager(@NotNull Project project) {
        try {
            VcsRepositoryManager vcsManager = project.getComponent(VcsRepositoryManager.class);
            if (vcsManager == null) {
                return null;
            }

            return new GitRepositoryManager(project, vcsManager);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            Project project = event.getProject();
            if (project == null) {
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
                NotificationUtil.showErrorNotification("Update Failed", "Git Extender could not find any repositories in the current project");
                return;
            }

            updateRepositories(project, repositoryList);
        } catch (Exception | Error e) {
            NotificationUtil.showErrorNotification("Git Extender Update Failed", "Git Extender failed to update the project due to exception: " + e);
        }
    }

    private void updateRepositories(@NotNull Project project, @NotNull List<GitRepository> repositories) {
        //get an access token for changing the repositories
        final AccessToken accessToken = DvcsUtil.workingTreeChangeStarted(project);

        final AtomicInteger countDown = new AtomicInteger(repositories.size());

        //get the settings to find out the selected options
        final GitExtenderSettings settings = GitExtenderSettings.getInstance();
        repositories.forEach(repo -> {
            final String repoName = VcsImplUtil.getShortVcsRootName(repo.getProject(), repo.getRoot());

            new Task.Backgroundable(repo.getProject(), "Updating " + repoName, false) {
                public void run(@NotNull ProgressIndicator indicator) {
                    RepositoryUpdater repositoryUpdater = new RepositoryUpdater(repo, indicator, repoName, settings);
                    try {
                        repositoryUpdater.updateRepository();
                    } finally {
                        if (countDown.decrementAndGet() <= 0) {
                            //the last task finished should clean up, release project changes and show info notification
                            DvcsUtil.workingTreeChangeFinished(repo.getProject(), accessToken);
                            NotificationUtil.showInfoNotification("Update Completed", "Git Extender updated all projects");
                        }
                    }
                }
            }.queue();
        });
    }
}
