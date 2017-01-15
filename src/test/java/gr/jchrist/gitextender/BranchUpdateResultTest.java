package gr.jchrist.gitextender;

import git4idea.commands.GitCommandResult;
import junit.framework.TestCase;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchUpdateResultTest extends TestCase {
    private static final GitCommandResult error =
            new GitCommandResult(false, 1,
                    Collections.singletonList("test error output"),
                    Collections.singletonList("error"),
                    new Exception("test exception"));
    private static final GitCommandResult success =
            new GitCommandResult(true, 0,
                    Collections.emptyList(),
                    Collections.singletonList("success"),
                    null);

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

    public void testCheckoutCheck() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult();
        bur.checkoutResult = success;
        assertThat(bur.isCheckoutSuccess()).isTrue();
        assertThat(bur.isCheckoutError()).isFalse();

        bur.checkoutResult = error;
        assertThat(bur.isCheckoutSuccess()).isFalse();
        assertThat(bur.isCheckoutError()).isTrue();
    }

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

    public void testAbortCheck() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult();

        assertThat(bur.isAbortSucceeded()).isFalse();

        bur.abortResult = error;
        assertThat(bur.isAbortSucceeded()).isFalse();

        bur.abortResult = success;
        assertThat(bur.isAbortSucceeded()).isTrue();
    }

    public void testGets() throws Exception {
        BranchUpdateResult bur = new BranchUpdateResult(success, success, success, success);
        assertThat(bur.getCheckoutResult()).isSameAs(success);
        assertThat(bur.getMergeFastForwardResult()).isSameAs(success);
        assertThat(bur.getMergeSimpleResult()).isSameAs(success);
        assertThat(bur.getAbortResult()).isSameAs(success);
    }
}