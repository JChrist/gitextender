package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import mockit.Expectations;
import mockit.Mocked;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class FastForwardOnlyMergerTest {
    private final String remoteBranchName = "test_remote_branch_name";
    @Mocked Git git;
    @Mocked Project project;
    @Mocked VirtualFile root;
    private FastForwardOnlyMerger fastForwardOnlyMerger;

    @Before
    public void before() throws Exception {
        fastForwardOnlyMerger = new FastForwardOnlyMerger(git, project, root, remoteBranchName);
    }

    @Test
    public void mergeFastForward(final @Mocked GitLineHandler mergeHandler) throws Exception {
        new Expectations() {{
            new GitLineHandler(project, root, GitCommand.MERGE); result = mergeHandler;
            git.runCommand(mergeHandler); result = success;
        }};
        assertThat(fastForwardOnlyMerger.mergeFastForward())
                .as("unexpected result from ff merge")
                .isSameAs(success);

        new VerificationsInOrder() {{
            mergeHandler.addParameters("--ff-only");
            mergeHandler.addParameters(remoteBranchName);
        }};
    }
}