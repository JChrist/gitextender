package gr.jchrist.gitextender;

import git4idea.commands.GitCommandResult;

public class BranchUpdateResult {
    protected GitCommandResult checkoutResult;
    protected GitCommandResult mergeFastForwardResult;
    protected GitCommandResult mergeSimpleResult;
    protected GitCommandResult abortResult;

    public BranchUpdateResult() {
    }

    public BranchUpdateResult(
            GitCommandResult checkoutResult, GitCommandResult mergeFastForwardResult,
            GitCommandResult mergeSimpleResult, GitCommandResult abortResult) {
        this.checkoutResult = checkoutResult;
        this.mergeFastForwardResult = mergeFastForwardResult;
        this.mergeSimpleResult = mergeSimpleResult;
        this.abortResult = abortResult;
    }

    public boolean isSuccess() {
        //we're successful if we checked out and
        //we merged either by fast-forward, or with simple merge
        return isCheckoutSuccess() && isMergeSuccess();
    }

    public boolean isCheckoutSuccess() {
        return checkoutResult != null && checkoutResult.success();
    }

    public boolean isCheckoutError() {
        return !isCheckoutSuccess();
    }

    public boolean isMergeSuccess() {
        return isMergeFastForwardSuccess() || isMergeSimpleSuccess();
    }

    public boolean isMergeError() {
        return !isMergeSuccess();
    }

    public boolean isMergeFastForwardSuccess() {
        return mergeFastForwardResult != null && mergeFastForwardResult.success();
    }

    public boolean isMergeSimpleSuccess() {
        return mergeSimpleResult != null && mergeSimpleResult.success();
    }

    public boolean isAbortSucceeded() {
        return abortResult != null && abortResult.success();
    }

    public GitCommandResult getCheckoutResult() {
        return checkoutResult;
    }

    public GitCommandResult getMergeFastForwardResult() {
        return mergeFastForwardResult;
    }

    public GitCommandResult getMergeSimpleResult() {
        return mergeSimpleResult;
    }

    public GitCommandResult getAbortResult() {
        return abortResult;
    }
}
