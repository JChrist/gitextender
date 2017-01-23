package gr.jchrist.gitextender.handlers;

import org.jetbrains.annotations.NotNull;

public class AfterFailedMergeHandler extends AfterMergeHandler {
    public AfterFailedMergeHandler(@NotNull MergeState mergeState) {
        super(mergeState);
    }

    @Override
    public void afterMerge() {
        mergeState.getLocalHistoryAction().finish();
    }
}
