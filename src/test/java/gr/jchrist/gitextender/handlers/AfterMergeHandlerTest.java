package gr.jchrist.gitextender.handlers;

import gr.jchrist.gitextender.BranchUpdateResult;
import org.junit.Test;

import static gr.jchrist.gitextender.TestingUtil.error;
import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

public class AfterMergeHandlerTest {
    @Test
    public void afterSuccessfulMerge() throws Exception {
        MergeState mergeState = new MergeState();
        BranchUpdateResult branchUpdateResult = new BranchUpdateResult(
                success, success, null, null);
        assertThat(AfterMergeHandler.getInstance(branchUpdateResult, mergeState))
                .as("unexpected after merge handler for successful merge result")
                .isInstanceOf(AfterSuccessfulMergeHandler.class);
    }

    @Test
    public void afterFailureMerge() throws Exception {
        MergeState mergeState = new MergeState();
        BranchUpdateResult branchUpdateResult = new BranchUpdateResult(
                success, error, error, null);

        assertThat(AfterMergeHandler.getInstance(branchUpdateResult, mergeState))
                .as("unexpected after merge handler for successful merge result")
                .isInstanceOf(AfterFailedMergeHandler.class);
    }
}