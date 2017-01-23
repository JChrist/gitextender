package gr.jchrist.gitextender.handlers;

import gr.jchrist.gitextender.BranchUpdateResult;
import org.jetbrains.annotations.NotNull;

public abstract class AfterMergeHandler {
    protected final MergeState mergeState;

    public AfterMergeHandler(@NotNull MergeState mergeState) {
        this.mergeState = mergeState;
    }

    @NotNull
    public static AfterMergeHandler getInstance(
            @NotNull BranchUpdateResult branchUpdateResult,
            @NotNull MergeState mergeState) {
        return (branchUpdateResult.isMergeSuccess() ?
                new AfterSuccessfulMergeHandler(mergeState) :
                new AfterFailedMergeHandler(mergeState));
    }

    public abstract void afterMerge();
}
