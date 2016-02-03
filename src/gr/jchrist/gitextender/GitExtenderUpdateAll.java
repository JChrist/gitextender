package gr.jchrist.gitextender;

import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import git4idea.GitPlatformFacade;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.commands.*;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitFetcher;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jchrist
 * @since 2015/06/27
 */
public class GitExtenderUpdateAll extends AnAction {
    public static final String GROUP_ID = "Git Extender";

    public static void showErrorNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.ERROR);
    }

    public static void showInfoNotification(@NotNull String title, @NotNull String content) {
        showNotification(title, content, NotificationType.INFORMATION);
    }

    public static void showNotification(@NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notifications.Bus.notify(new Notification(GROUP_ID, title, content, type));
    }

    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            Project project = event.getProject();
            if (project == null) {
                showErrorNotification("Update Failed", "Git Extender failed to retrieve the project");
                return;
            }

            GitPlatformFacade gitPlatformFacade = ServiceManager.getService(project, GitPlatformFacade.class);
            //VcsRepositoryManager vcsManager = ServiceManager.getService(project, VcsRepositoryManager.class);
            VcsRepositoryManager vcsManager = project.getComponent(VcsRepositoryManager.class);
            if (vcsManager == null) {
                showErrorNotification("Update Failed", "Git Extender could not find any repository manager in the current project");
                return;
            }

            GitRepositoryManager manager = new GitRepositoryManager(project, gitPlatformFacade, vcsManager);
            List<GitRepository> repositoryList = manager.getRepositories();
            manager.updateAllRepositories();
            if (repositoryList.isEmpty()) {
                showErrorNotification("Update Failed", "Git Extender could not find any repositories in the current project");
                return;
            }

            updateRepositories(repositoryList);
        } catch (Exception | Error e) {
            showErrorNotification("Git Extender Update Failed", "Gi Extender failed to update the project due to exception: " + e);
        }
    }

    private void updateRepositories(@NotNull List<GitRepository> repositories) {
        final AtomicInteger countDown = new AtomicInteger(repositories.size());
        for (final GitRepository repo : repositories) {
            new Task.Backgroundable(repo.getProject(), "Updating " + repo.getPresentableUrl(), false) {
                public void run(@NotNull ProgressIndicator indicator) {
                    updateRepository(repo, indicator);
                    if (countDown.decrementAndGet() <= 0) {
                        showInfoNotification("Update Completed", "Git Extender updated all projects");
                    }
                }
            }.queue();
        }
    }

    private void updateRepository(@NotNull GitRepository repo, @NotNull ProgressIndicator indicator) {
        //check if repo is valid for updating
        if (!canRepoBeUpdated(repo)) {
            return;
        }

        //find git service
        Git git = ServiceManager.getService(repo.getProject(), Git.class);

        String currBranch = repo.getCurrentBranchName();
        GitCommandResult result;

        //check if we require stashing
        boolean stagedChanges;
        boolean unstagedChanges;
        try {
            stagedChanges = GitUtil.hasLocalChanges(true, repo.getProject(), repo.getRoot());
            unstagedChanges = GitUtil.hasLocalChanges(false, repo.getProject(), repo.getRoot());
        } catch (Exception e) {
            showErrorNotification("Git Extender update failed",
                    "Git repo: " + repo.getPresentableUrl() + "<br>" +
                            "Error: Git Extender failed to update git repo, " +
                            "because it could not identify if there were any staged/unstaged changes. " +
                            "The exception was: " + e.getMessage());
            return;
        }

        if (stagedChanges || unstagedChanges) {
            //there are changes that we need to stash
            result = git.stashSave(repo, "GitExtender_Stashing");
            if (!result.success()) {
                showErrorNotification("Git Extender failed to stash changes",
                        "Git repo: " + repo.getPresentableUrl() + "<br>" +
                                "Error: " + result.getErrorOutputAsJoinedString());
                return;
            }
        }

        try {
            //fetch and prune remote
            //git fetch origin
            boolean fetchResult = new GitFetcher(repo.getProject(), indicator, true)
                    .fetchRootsAndNotify(Collections.singleton(repo), null, true);

            if (!fetchResult) {
                //git fetcher will have displayed the error
                return;
            }

            //update the repo after fetch, in order to re-sync locals and remotes
            repo.update();

            for (GitBranchTrackInfo info : repo.getBranchTrackInfos()) {
                GitRemoteBranch remoteBranch = info.getRemoteBranch();
                //checkout branch
                GitCommand checkout = GitCommand.CHECKOUT;
                GitLineHandler checkoutHandler = new GitLineHandler(repo.getProject(), repo.getRoot(), checkout);
                checkoutHandler.addParameters(info.getLocalBranch().getName());
                result = git.runCommand(checkoutHandler);
                if (!result.success()) {
                    showErrorNotification("Git Extender failed to checkout branch",
                            "Local branch: " + info.getLocalBranch().getName() + "<br>" +
                                    "Git repo:" + repo.getPresentableUrl() + "<br>" +
                                    "Error: " + result.getErrorOutputAsJoinedString());
                    continue;
                }

                //do NOT rebase from remote after all :) just merge with -ff only
                GitCommand merge = GitCommand.MERGE;
                GitLineHandler mergeHandler = new GitLineHandler(repo.getProject(), repo.getRoot(), merge);
                mergeHandler.addParameters("--ff-only");
                mergeHandler.addParameters(remoteBranch.getNameForLocalOperations());
                result = git.runCommand(mergeHandler);
                if (!result.success()) {
                    showErrorNotification("Git Extender failed to merge (with fast-forward only) branch",
                            "Local branch: " + info.getLocalBranch().getName() + "<br>" +
                                    "Remote branch: " + remoteBranch.getNameForLocalOperations() + "<br>" +
                                    "Git repo:" + repo.getPresentableUrl() + "<br>" +
                                    "Please perform the merge (which needs conflict resolution) manually for this branch, " +
                                    "by checking it out, merging the changes and resolving the conflicts");
                    //merge failed, nothing was done, just carry on to other branches
                }
            }

            //now that we're finished, checkout again the branch that the developer had before
            GitCommand checkout = GitCommand.CHECKOUT;
            GitLineHandler checkoutHandler = new GitLineHandler(repo.getProject(), repo.getRoot(), checkout);
            checkoutHandler.addParameters(currBranch);
            git.runCommand(checkoutHandler);

            //update the repo after all updates, in order to re-sync locals / remotes and changed files
            repo.update();
        } finally {
            //now unstash, if we had stashed any changes
            if (stagedChanges || unstagedChanges) {
                git.stashPop(repo, new GitLineHandlerListener() {
                    public void onLineAvailable(String line, Key outputType) {
                    }

                    public void processTerminated(int exitCode) {
                    }

                    public void startFailed(Throwable exception) {
                    }
                });
            }
        }
    }

    private boolean canRepoBeUpdated(@NotNull GitRepository repo) {
        String repoName = repo.getPresentableUrl();
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
}
