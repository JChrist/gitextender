package gr.jchrist.gitextender;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsImplUtil;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitFetcher;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jchrist
 * @since 2015/06/27
 */
public class GitExtenderUpdateAll extends AnAction {
    public static final String GROUP_ID = "Git Extender";
    private GitExtenderSettings gitExtenderSettings;

    public static void showErrorNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.ERROR);
    }

    public static void showInfoNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.INFORMATION);
    }

    public static void showNotification(@NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notifications.Bus.notify(new Notification(GROUP_ID, title, content, type));
    }

    @Nullable
    private static GitRepositoryManager getGitRepositoryManager(@NotNull Project project) {
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
                showErrorNotification("Update Failed", "Git Extender failed to retrieve the project");
                return;
            }

            GitRepositoryManager manager = getGitRepositoryManager(project);

            if (manager == null) {
                showErrorNotification("Update Failed", "Git Extender could not initialize the project's repository manager");
                return;
            }

            List<GitRepository> repositoryList = manager.getRepositories();
            manager.updateAllRepositories();
            if (repositoryList.isEmpty()) {
                showErrorNotification("Update Failed", "Git Extender could not find any repositories in the current project");
                return;
            }

            updateRepositories(project, repositoryList);
        } catch (Exception | Error e) {
            showErrorNotification("Git Extender Update Failed", "Git Extender failed to update the project due to exception: " + e);
        }
    }

    private void updateRepositories(@NotNull Project project, @NotNull List<GitRepository> repositories) {
        //get an access token for changing the repositories
        final AccessToken accessToken = DvcsUtil.workingTreeChangeStarted(project);

        final AtomicInteger countDown = new AtomicInteger(repositories.size());

        //get the settings to find out the selected options
        gitExtenderSettings = GitExtenderSettings.getInstance();

        for (final GitRepository repo : repositories) {
            final String repoName = VcsImplUtil.getShortVcsRootName(repo.getProject(), repo.getRoot());
            new Task.Backgroundable(repo.getProject(), "Updating " + repoName, false) {
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        updateRepository(repo, indicator, repoName);
                    } finally {
                        if (countDown.decrementAndGet() <= 0) {
                            //the last task finished should clean up, release project changes and show info notification
                            DvcsUtil.workingTreeChangeFinished(repo.getProject(), accessToken);
                            showInfoNotification("Update Completed", "Git Extender updated all projects");
                        }
                    }
                }
            }.queue();
        }
    }

    private void updateRepository(
            @NotNull final GitRepository repo,
            @NotNull ProgressIndicator indicator,
            @NotNull final String repoName
    ) {
        //check if repo is valid for updating
        if (!canRepoBeUpdated(repo, repoName)) {
            return;
        }

        final Project project = repo.getProject();

        //find git service
        Git git = ServiceManager.getService(project, Git.class);

        final String currBranch = repo.getCurrentBranchName();

        //check if we require stashing
        boolean repoChanges;
        try {
            repoChanges = hasChanges(repo);
        } catch (Exception e) {
            showErrorNotification("Git Extender update failed",
                    "Git repo: " + repoName + "<br>" +
                            "Error: Git Extender failed to update git repo, " +
                            "because it could not identify if there were any staged/unstaged changes. " +
                            "The exception was: " + e.getMessage());
            return;
        }

        if (repoChanges) {
            //there are changes that we need to stash
            GitCommandResult saveResult = git.stashSave(repo, "GitExtender_Stashing");
            if (!saveResult.success()) {
                showErrorNotification("Git Extender failed to stash changes",
                        "Git repo: " + repoName + "<br>" +
                                "Error: " + saveResult.getErrorOutputAsJoinedString());
                return;
            }
        }

        try {

            //fetch and prune remote
            //git fetch origin
            boolean fetchResult = new GitFetcher(project, indicator, true)
                    .fetchRootsAndNotify(Collections.singleton(repo), null, true);

            if (!fetchResult) {
                //git fetcher will have displayed the error
                return;
            }

            //update the repo after fetch, in order to re-sync locals and remotes
            repo.update();

            for (final GitBranchTrackInfo info : repo.getBranchTrackInfos()) {
                final BranchUpdater updater = new BranchUpdater(git, repo, repoName, info, gitExtenderSettings);

                BranchUpdateResult result = updater.update();

                //let's see if we had any errors
                if (!result.isSuccess()) {
                    //where was the error?
                    //in checkout ?
                    if (result.isCheckoutError()) {
                        String error = "Local branch: " + updater.getLocalBranchName() + "<br>" +
                                "Git repo:" + repoName + "<br>" +
                                "Error: " + result.checkoutResult.getErrorOutputAsJoinedString();
                        showErrorNotification("Git Extender failed to checkout branch", error);
                    } else if (result.isMergeError()) {
                        String error = "Local branch: " + updater.getLocalBranchName() + "<br>" +
                                "Remote branch: " + updater.getRemoteBranchName() + "<br>" +
                                "Git repo:" + repoName + "<br>";
                        if (Boolean.TRUE.equals(gitExtenderSettings.getAttemptMergeAbort())) {
                            error += "Merge was ";
                            if (result.isAbortSucceeded()) {
                                error += "aborted.";
                            } else {
                                error += "NOT aborted! You will need to resolve the merge conflicts!";
                            }
                        } else {
                            error += "Please perform the merge (which may need conflict resolution) " +
                                    "manually for this branch, " +
                                    "by checking it out, merging the changes and resolving any conflicts";
                        }
                        showErrorNotification("Git Extender failed to merge branch with fast-forward only", error);
                    }
                }
            }

            //now that we're finished, checkout again the branch that the developer had before
            GitCommand checkout = GitCommand.CHECKOUT;
            GitLineHandler checkoutHandler = new GitLineHandler(project, repo.getRoot(), checkout);
            checkoutHandler.addParameters(currBranch);
            git.runCommand(checkoutHandler);

            //update the repo after all updates, in order to re-sync locals / remotes and changed files
            repo.update();
        } finally {
            //now unstash, if we had stashed any changes
            if (repoChanges) {
                git.stashPop(repo);
            }
        }
    }

    private boolean canRepoBeUpdated(@NotNull GitRepository repo, String repoName) {
        if (repo.isRebaseInProgress()) {
            showErrorNotification("Repository cannot be updated",
                    "Repository " + repoName + " cannot be updated, because there is a rebase in progress");
            return false;
        }

        if (repo.getRemotes().isEmpty()) {
            showErrorNotification("Repository cannot be updated",
                    "Repository " + repoName + " cannot be updated, because there there are no remotes");
            return false;
        }

        return true;
    }

    private boolean hasChanges(GitRepository repo) throws VcsException {
        return GitUtil.hasLocalChanges(true, repo.getProject(), repo.getRoot()) ||
                GitUtil.hasLocalChanges(false, repo.getProject(), repo.getRoot());
    }
}
