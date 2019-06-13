package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class DeleteHandlerTest {
    private final String local = "local";
    @Mocked Git git;
    @Mocked Project project;
    @Mocked VirtualFile root;

    @Test
    public void delete(@Mocked final GitLineHandler del) throws Exception {
        new Expectations() {{
            new GitLineHandler(project, root, GitCommand.BRANCH); result = del;
            git.runCommand(del); result = success;
        }};
        DeleteHandler handler = new DeleteHandler(git, project, root);
        assertThat(handler.delete(local)).isSameAs(success);

        new Verifications() {{
            del.addParameters(DeleteHandler.DELETE_FLAG, local);
        }};
    }
}