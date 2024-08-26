package gr.jchrist.gitextender;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.fetch.GitFetchResult;
import git4idea.fetch.GitFetchSupport;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.handlers.CheckoutHandler;
import gr.jchrist.gitextender.handlers.DeleteHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RepositoryUpdater {
    private static final Logger logger = Logger.getInstance(RepositoryUpdater.class);
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
    public static final String EXCEPTION_DURING_UPDATE = "Git repo: $repo<br>" +
            "Error: Git Extender failed to update git repo, due to an exception during update. " +
            "The exception was: ";
    private final GitRepository repo;
    private final String repoName;
    private final GitExtenderSettings settings;

    public RepositoryUpdater(
            @NotNull GitRepository repo,
            @NotNull String repoName,
            @NotNull GitExtenderSettings settings
    ) {
        this.repo = repo;
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
        final Git git = ApplicationManager.getApplication().getService(Git.class);

        final String currBranch = repo.getCurrentBranchName();
        if (currBranch == null) {
            logger.warn("no current branch in repository: " + repo);
            NotificationUtil.showErrorNotification("Git Extender update failed",
                    getMessage(NO_CURRENT_BRANCH, repoName));
            return;
        }

        //check if we require stashing
        final boolean repoChanges;
        try {
            repoChanges = hasChanges(repo);
        } catch (Exception e) {
            logger.warn("error trying to find if repo has changes: " + repo, e);
            NotificationUtil.showErrorNotification("Git Extender update failed",
                    getMessage(FAILED_HAS_CHANGES, repoName, e.getMessage()));
            return;
        }

        if (repoChanges) {
            //there are changes that we need to stash
            GitCommandResult saveResult = git.stashSave(repo, "GitExtender_Stashing");
            if (!saveResult.success()) {
                String errOut = saveResult.getErrorOutputAsJoinedString();
                logger.warn("error trying to stash local repo changes: " + errOut);
                NotificationUtil.showErrorNotification("Git Extender failed to stash changes",
                        getMessage(NO_STASH, repoName, errOut));
                return;
            }
        }

        // keeping a flag whether we tried to abort a merge and failed
        // in such a situation, we must stop whatever we were doing ASAP, without making any other change whatsoever.
        boolean failureToAbort = false;
        try {
            /*
            //todo locking like this is invalid
            logger.info("write-locking vcs for repo: " + repo + " from thread:" + Thread.currentThread().getName());
            boolean locked = repo.getVcs().getCommandLock().writeLock().tryLock(0, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new Exception("could not lock repo " + repo +
                        " for update from thread: " + Thread.currentThread().getName());
            }
            */
            //mark here the branch track info prior to fetching/pruning
            final Set<GitBranchTrackInfo> preInfos = new HashSet<>(repo.getBranchTrackInfos());

            //fetch and prune remote
            //git fetch origin
            GitFetchSupport gfs = GitFetchSupport.fetchSupport(project);
            GitFetchResult gfr = gfs.fetchAllRemotes(Collections.singletonList(repo));
            gfr.showNotification();

            //update the repo after fetch, in order to re-sync locals and remotes
            repo.update();

            if (settings.getPruneLocals()) {
                pruneLocals(git, preInfos);
            }

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
                } else {
                    //todo perhaps when we have a successful result we need to wait for the update info tree
                    //generation to happen (invoked to happen through the dispatch thread) before moving on
                }
            }

            CheckoutHandler checkoutHandler = new CheckoutHandler(git, project, repo.getRoot());
            //now that we're finished, checkout again the branch that the developer had before
            checkoutHandler.checkout(currBranch);

            //update the repo after all updates, in order to re-sync locals / remotes and changed files
            repo.update();
        }  catch (Exception e) {
            logger.warn("exception trying to update repo: " + repo + " from thread:" + Thread.currentThread().getName(), e);
            NotificationUtil.showErrorNotification("Git Extender update failed", getMessage(EXCEPTION_DURING_UPDATE, repoName, e.getMessage()));
        } finally {
            //now unstash, if we had stashed any changes
            if (repoChanges && !failureToAbort) {
                try {
                    git.stashPop(repo);
                } catch (Exception e) {
                    logger.warn("error trying to pop stash for repo: " + repo, e);
                }
            }
            /*
            //todo locking like this is invalid
            logger.info("write-unlocking vcs for repo: " + repo);
            try {
                repo.getVcs().getCommandLock().writeLock().unlock();
            } catch (Exception e) {
                logger.warn("exception trying to unlock repo " + repo + " from thread:" +
                        Thread.currentThread().getName(), e);
            }*/
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

    protected boolean pruneLocals(@NotNull Git git, @NotNull Collection<GitBranchTrackInfo> preFetch) {
        Collection<GitBranchTrackInfo> infos = repo.getBranchTrackInfos();
        if (preFetch.size() == infos.size()) {
            return false;
        }

        Set<GitBranchTrackInfo> afterFetch = new HashSet<>(infos);
        Set<GitBranchTrackInfo> remainingLocals = new HashSet<>();
        Set<GitBranchTrackInfo> markedForDeletion = new HashSet<>();

        for (GitBranchTrackInfo pre : preFetch) {
            boolean mark = true;
            for (GitBranchTrackInfo after : afterFetch) {
                if (pre.getLocalBranch().getName().equals(after.getLocalBranch().getName())) {
                    mark = false;
                    break;
                }
            }
            if (mark) {
                markedForDeletion.add(pre);
            } else {
                remainingLocals.add(pre);
            }
        }

        if (remainingLocals.isEmpty()) {
            //this is strange, if we proceeded to remove all remotely-pruned local branches, nothing would be left
            logger.warn("not deleting local branches with pruned remotes, as no branches would be left in: " + repo);
            return false;
        }

        if (markedForDeletion.isEmpty()) {
            return false;
        }

        //check if the current branch is also marked for deletion, so as to switch to another (defaulting to master)
        GitLocalBranch currentBranch = repo.getCurrentBranch();
        markedForDeletion.stream().filter(ti -> ti.getLocalBranch().equals(currentBranch))
                .findAny().ifPresent(ti -> {
            //current branch is marked for deletion, so try to switch to main.
            // if that fails, switch to the first possible local
            CheckoutHandler checkoutHandler = new CheckoutHandler(git, repo.getProject(), repo.getRoot());
            String branchToSwitchTo = null;
            for (GitBranchTrackInfo localBranch : remainingLocals) {
                if ("master".equals(localBranch.getLocalBranch().getName()) ||
                        "main".equals(localBranch.getLocalBranch().getName())) {
                    //this is where we want to go
                    branchToSwitchTo = localBranch.getLocalBranch().getName();
                    break;
                }
            }

            if (branchToSwitchTo == null) {
                //find the first local branch that is not marked for deletion, in order to switch to that
                branchToSwitchTo = remainingLocals.iterator().next().getLocalBranch().getName();
            }

            checkoutHandler.checkout(branchToSwitchTo);
        });

        //now delete all marked local branches
        DeleteHandler dh = new DeleteHandler(git, repo.getProject(), repo.getRoot());
        for (GitBranchTrackInfo del : markedForDeletion) {
            dh.delete(del.getLocalBranch().getName());
        }

        return true;
    }

    private boolean hasChanges(GitRepository repo) throws Exception {
        return GitUtil.hasLocalChanges(true, repo.getProject(), repo.getRoot()) ||
                GitUtil.hasLocalChanges(false, repo.getProject(), repo.getRoot());
    }
}
