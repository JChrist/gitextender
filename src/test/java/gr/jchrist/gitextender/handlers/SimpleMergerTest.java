package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gr.jchrist.gitextender.TestingUtil.error;
import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class SimpleMergerTest {
    private final String remoteBranchName = "test_remote_branch_name";
    @Mocked Git git;
    @Mocked Project project;
    @Mocked VirtualFile root;
    private SimpleMerger simpleMerger;

    @Before
    public void before() throws Exception {
        simpleMerger = new SimpleMerger(git, project, root, remoteBranchName);
    }

    @Test
    public void successfulMerge(final @Mocked GitLineHandler mergeHandler) throws Exception {
        new Expectations() {{
            new GitLineHandler(project, root, GitCommand.MERGE); result = mergeHandler;
            git.runCommand(mergeHandler); result = success;
        }};

        simpleMerger.mergeAbortIfFailed();

        assertThat(simpleMerger.getMergeResult()).isSameAs(success);
        assertThat(simpleMerger.getAbortResult()).isNull();

        new Verifications() {{
            mergeHandler.addParameters(remoteBranchName);
            mergeHandler.addParameters("--abort"); times = 0;
        }};
    }

    @Test
    public void successfulAbort(
            final @Mocked GitLineHandler handler
    ) throws Exception {
        new Expectations() {{
            new GitLineHandler(project, root, GitCommand.MERGE); result = handler;
            git.runCommand(handler); result = error; result = success;
        }};

        simpleMerger.mergeAbortIfFailed();

        assertThat(simpleMerger.getMergeResult())
                .as("unexpected merge result")
                .isSameAs(error);
        assertThat(simpleMerger.getAbortResult())
                .as("unexpected abort result")
                .isSameAs(success);

        new VerificationsInOrder() {{
            //new GitLineHandler(project, root, GitCommand.MERGE);
            handler.addParameters(remoteBranchName);
            //new GitLineHandler(project, root, GitCommand.MERGE);
            handler.addParameters("--abort");
        }};
    }

    @Test
    public void errorInAbort(
            final @Mocked GitLineHandler handler
    ) throws Exception {
        new Expectations() {{
            new GitLineHandler(project, root, GitCommand.MERGE); result = handler;
            git.runCommand(handler); result = error;
        }};

        simpleMerger.mergeAbortIfFailed();

        assertThat(simpleMerger.getMergeResult())
                .as("unexpected merge result")
                .isSameAs(error);
        assertThat(simpleMerger.getAbortResult())
                .as("unexpected abort result")
                .isSameAs(error);

        new VerificationsInOrder() {{
            //new GitLineHandler(project, root, GitCommand.MERGE);
            handler.addParameters(remoteBranchName);
            //new GitLineHandler(project, root, GitCommand.MERGE);
            handler.addParameters("--abort");
        }};
    }
}