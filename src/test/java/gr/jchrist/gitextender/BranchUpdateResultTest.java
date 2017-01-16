package gr.jchrist.gitextender;

import org.junit.Test;

import static gr.jchrist.gitextender.TestingUtil.error;
import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

public class BranchUpdateResultTest {
    @Test
    public void testIsSuccess() throws Exception {
        //success by fast forward
        assertThat(new BranchUpdateResult(success, success, null, null).isSuccess()).isTrue();
        //success by simple merge
        assertThat(new BranchUpdateResult(success, error, success, null).isSuccess()).isTrue();

        //error in checkout
        assertThat(new BranchUpdateResult(error, null, null, null).isSuccess()).isFalse();

        //error in both merges with abort
        assertThat(new BranchUpdateResult(error, error, error, success).isSuccess()).isFalse();

        //error in both merges with no abort result
        assertThat(new BranchUpdateResult(error, error, error, null).isSuccess()).isFalse();

        //error in both merges with error abort result
        assertThat(new BranchUpdateResult(error, error, error, error).isSuccess()).isFalse();
    }

    @Test
    public void testCheckoutCheck() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult();
        bur.checkoutResult = success;
        assertThat(bur.isCheckoutSuccess()).isTrue();
        assertThat(bur.isCheckoutError()).isFalse();

        bur.checkoutResult = error;
        assertThat(bur.isCheckoutSuccess()).isFalse();
        assertThat(bur.isCheckoutError()).isTrue();
    }

    @Test
    public void testMergeCheck() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult();
        bur.mergeFastForwardResult = success;
        bur.mergeSimpleResult = null;
        assertThat(bur.isMergeSuccess()).isTrue();
        assertThat(bur.isMergeError()).isFalse();

        bur.mergeFastForwardResult = error;
        bur.mergeSimpleResult = success;
        assertThat(bur.isMergeSuccess()).isTrue();
        assertThat(bur.isMergeError()).isFalse();

        bur.mergeFastForwardResult = error;
        bur.mergeSimpleResult = error;
        assertThat(bur.isMergeSuccess()).isFalse();
        assertThat(bur.isMergeError()).isTrue();

        bur.mergeFastForwardResult = null;
        bur.mergeSimpleResult = null;
        assertThat(bur.isMergeSuccess()).isFalse();
        assertThat(bur.isMergeError()).isTrue();
    }

    @Test
    public void testAbortCheck() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult();

        assertThat(bur.isAbortSucceeded()).isFalse();

        bur.abortResult = error;
        assertThat(bur.isAbortSucceeded()).isFalse();

        bur.abortResult = success;
        assertThat(bur.isAbortSucceeded()).isTrue();
    }

    @Test
    public void testGets() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult(success, success, success, success);
        assertThat(bur.getCheckoutResult()).isSameAs(success);
        assertThat(bur.getMergeFastForwardResult()).isSameAs(success);
        assertThat(bur.getMergeSimpleResult()).isSameAs(success);
        assertThat(bur.getAbortResult()).isSameAs(success);
    }
}