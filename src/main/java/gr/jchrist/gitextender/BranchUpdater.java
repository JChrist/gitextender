package gr.jchrist.gitextender;

import com.intellij.openapi.diagnostic.Logger;
import git4idea.commands.Git;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.handlers.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author jchrist
 * @since 2016/03/27
 */
public class BranchUpdater {
    private static final Logger logger = Logger.getInstance(BranchUpdater.class);

    private final Git git;
    private final GitRepository repo;

    private final String repoName;
    private final String remoteBranchName;
    private final String localBranchName;
    private final BranchUpdateResult branchUpdateResult;
    private final GitExtenderSettings gitExtenderSettings;
    private MergeState mergeState;

    public BranchUpdater(
            Git git, GitRepository repo, String repoName,
            GitBranchTrackInfo info, GitExtenderSettings gitExtenderSettings) {
        this.git = git;
        this.repo = repo;
        this.repoName = repoName;

        this.remoteBranchName = info.getRemoteBranch().getNameForLocalOperations();
        this.localBranchName = info.getLocalBranch().getName();
        this.branchUpdateResult = new BranchUpdateResult();
        this.gitExtenderSettings = gitExtenderSettings;
    }

    @NotNull
    public BranchUpdateResult update() {
        checkout();
        if (!branchUpdateResult.isCheckoutSuccess()) {
            return this.branchUpdateResult;
        }

        merge();
        return this.branchUpdateResult;
    }

    protected void checkout() {
        branchUpdateResult.checkoutResult =
                new CheckoutHandler(git, repo.getProject(), repo.getRoot(), localBranchName)
                        .checkout();
    }

    protected void merge() {
        //let's prepare for the merge: find what files we have now
        prepareMerge();

        mergeFastForwardOnly();
        if (!branchUpdateResult.isMergeSuccess()) {
            // this is controlled with a user setting flag,
            // because it might be dangerous in case abort fails
            if (Boolean.TRUE.equals(gitExtenderSettings.getAttemptMergeAbort())) {
                logger.info("fast-forward merge failed, attempting simple merge");
                mergeAbortIfFailed();
            } else {
                logger.info("fast-forward merge failed, NOT attempting simple merge");
            }
        }

        if (branchUpdateResult.isMergeSuccess()) {
            afterMerge();
        }
    }

    protected void mergeFastForwardOnly() {
        branchUpdateResult.mergeFastForwardResult =
                new FastForwardOnlyMerger(git, repo.getProject(), repo.getRoot(), remoteBranchName)
                        .mergeFastForward();
    }

    protected void mergeAbortIfFailed() {
        SimpleMerger simpleMerger = new SimpleMerger(git, repo.getProject(), repo.getRoot(), remoteBranchName);
        simpleMerger.mergeAbortIfFailed();
        branchUpdateResult.mergeSimpleResult = simpleMerger.getMergeResult();
        branchUpdateResult.abortResult = simpleMerger.getAbortResult();
    }

    protected void prepareMerge() {
        mergeState =
                new BeforeMergeHandler(repo.getProject(), repo.getRoot(),
                        repoName, localBranchName, remoteBranchName)
                        .beforeMerge();
    }

    protected void afterMerge() {
        AfterMergeHandler.getInstance(branchUpdateResult, mergeState).afterMerge();
    }

    @NotNull
    public String getLocalBranchName() {
        return localBranchName;
    }

    @NotNull
    public String getRemoteBranchName() {
        return remoteBranchName;
    }

    @NotNull
    public BranchUpdateResult getBranchUpdateResult() {
        return branchUpdateResult;
    }
}
