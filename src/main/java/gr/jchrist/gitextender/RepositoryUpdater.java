package gr.jchrist.gitextender;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRepository;
import git4idea.update.GitFetcher;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.handlers.CheckoutHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class RepositoryUpdater {
    public static final String REBASE_IN_PROGRESS = "Repository $repo cannot be updated, because there is a rebase in progress";
    public static final String NO_REMOTES = "Repository $repo cannot be updated, because there are no remotes";
    public static final String NO_CURRENT_BRANCH = "Git repo: $repo<br>" +
            "Error: Git Extender failed to update git repo, " +
            "because it could not identify what the current checked out branch is.";
    public static final String NO_STASH = "Git repo: $repo<br>Error: ";
    public static final String FAILED_HAS_CHANGES = "Git repo: $repo<br>" +
            "Error: Git Extender failed to update git repo, " +
            "because it could not identify if there were any staged/unstaged changes. " +
            "The exception was: ";
    private final GitRepository repo;
    private final ProgressIndicator indicator;
    private final String repoName;
    private final GitExtenderSettings settings;

    public RepositoryUpdater(
            @NotNull GitRepository repo,
            @NotNull ProgressIndicator indicator,
            @NotNull String repoName,
            @NotNull GitExtenderSettings settings
    ) {
        this.repo = repo;
        this.indicator = indicator;
        this.repoName = repoName;
        this.settings = settings;
    }

    public static String getMessage(String base, String repoName) {
        return base.replace("$repo", repoName);
    }

    public static String getMessage(String base, String repoName, String error) {
        return base.replace("$repo", repoName) + error;
    }

    public void updateRepository() {
        //check if repo is valid for updating
        if (!canRepoBeUpdated(repo)) {
            return;
        }

        final Project project = repo.getProject();

        //find git service
        final Git git = ServiceManager.getService(project, Git.class);

        final String currBranch = repo.getCurrentBranchName();
        if (currBranch == null) {
            NotificationUtil.showErrorNotification("Git Extender update failed",
                    getMessage(NO_CURRENT_BRANCH, repoName));
            return;
        }

        //check if we require stashing
        final boolean repoChanges;
        try {
            repoChanges = hasChanges(repo);
        } catch (Exception e) {
            NotificationUtil.showErrorNotification("Git Extender update failed",
                    getMessage(FAILED_HAS_CHANGES, repoName, e.getMessage()));
            return;
        }

        if (repoChanges) {
            //there are changes that we need to stash
            GitCommandResult saveResult = git.stashSave(repo, "GitExtender_Stashing");
            if (!saveResult.success()) {
                NotificationUtil.showErrorNotification("Git Extender failed to stash changes",
                        getMessage(NO_STASH, repoName, saveResult.getErrorOutputAsJoinedString()));
                return;
            }
        }

        // keeping a flag whether we tried to abort a merge and failed
        // in such a situation, we must stop whatever we were doing ASAP, without making any other change whatsoever.
        boolean failureToAbort = false;
        try {

            //fetch and prune remote
            //git fetch origin
            final boolean fetchResult = new GitFetcher(project, indicator, true)
                    .fetchRootsAndNotify(Collections.singleton(repo), null, true);

            if (!fetchResult) {
                //git fetcher will have displayed the error
                return;
            }

            //update the repo after fetch, in order to re-sync locals and remotes
            repo.update();

            for (final GitBranchTrackInfo info : repo.getBranchTrackInfos()) {
                final BranchUpdater updater = new BranchUpdater(git, repo, repoName, info, settings);

                BranchUpdateResult result = updater.update();

                //let's see if we had any errors
                if (!result.isSuccess()) {
                    //where was the error?
                    //in checkout ?
                    if (result.isCheckoutError()) {
                        String error = "Local branch: " + updater.getLocalBranchName() + "<br>" +
                                "Git repo:" + repoName + "<br>" +
                                "Checkout Error: " + result.checkoutResult.getErrorOutputAsJoinedString();
                        NotificationUtil.showErrorNotification("Git Extender failed to checkout branch", error);
                    } else if (result.isMergeError()) {
                        String error = "Local branch: " + updater.getLocalBranchName() + "<br>" +
                                "Remote branch: " + updater.getRemoteBranchName() + "<br>" +
                                "Git repo:" + repoName + "<br>" +
                                "Merge error: ";
                        if (Boolean.FALSE.equals(settings.getAttemptMergeAbort()) || result.isAbortSucceeded()) {
                            if (result.isAbortSucceeded()) {
                                error += "Merge was aborted.<br>";
                            }
                            error += "Please perform the merge (which may need conflict resolution) " +
                                    "manually for this branch, " +
                                    "by checking it out, merging the changes and resolving any conflicts";
                        } else {
                            error += "Merge was NOT aborted! You will need to resolve the merge conflicts!";
                            if (repoChanges) {
                                error += " The changes you had on branch: " + currBranch + " were stashed, " +
                                        "but not un-stashed, due to this merge error. " +
                                        "After resolving the merge conflict, " +
                                        "you can revert to the branch you were on " +
                                        "(e.g. using <em>git checkout " + currBranch + "</em>) " +
                                        "and then pop the stash (e.g. <em>git stash pop</em>)";
                            }
                            failureToAbort = true;
                        }
                        NotificationUtil.showErrorNotification(
                                "Git Extender failed to merge branch" +
                                        (Boolean.FALSE.equals(settings.getAttemptMergeAbort()) ?
                                                " with fast-forward only" : ""),
                                error);
                        if (failureToAbort) {
                            return;
                        }
                    }
                }
            }

            CheckoutHandler checkoutHandler = new CheckoutHandler(git, project, repo.getRoot(), currBranch);
            //now that we're finished, checkout again the branch that the developer had before
            checkoutHandler.checkout();

            //update the repo after all updates, in order to re-sync locals / remotes and changed files
            repo.update();
        } finally {
            //now unstash, if we had stashed any changes
            if (repoChanges && !failureToAbort) {
                git.stashPop(repo);
            }
        }
    }

    private boolean canRepoBeUpdated(@NotNull GitRepository repo) {
        if (repo.isRebaseInProgress()) {
            NotificationUtil.showErrorNotification("Repository cannot be updated", getMessage(REBASE_IN_PROGRESS, repoName));
            return false;
        }

        if (repo.getRemotes().isEmpty()) {
            NotificationUtil.showErrorNotification("Repository cannot be updated", getMessage(NO_REMOTES, repoName));
            return false;
        }

        return true;
    }

    private boolean hasChanges(GitRepository repo) throws VcsException {
        return GitUtil.hasLocalChanges(true, repo.getProject(), repo.getRoot()) ||
                GitUtil.hasLocalChanges(false, repo.getProject(), repo.getRoot());
    }
}
