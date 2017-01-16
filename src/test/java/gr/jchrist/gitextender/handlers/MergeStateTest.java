package gr.jchrist.gitextender.handlers;

import com.intellij.history.Label;
import com.intellij.history.LocalHistoryAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRevisionNumber;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class MergeStateTest {
    private final String repo = "repo";
    private final String local = "local";
    private final String remote = "remote";
    @Mocked
    Project project;
    @Mocked
    VirtualFile root;
    @Mocked
    GitRevisionNumber start;
    @Mocked
    Label before;
    @Mocked
    LocalHistoryAction localHistoryAction;
    @Mocked
    UpdatedFiles updatedFiles;
    private MergeState mergeState;

    @Test
    public void mergeStateCreation() throws Exception {
        mergeState = new MergeState(project, root, repo, local, remote, start,
                before, localHistoryAction, updatedFiles);

        assertThat(mergeState.getProject()).isSameAs(project);
        assertThat(mergeState.getRoot()).isSameAs(root);
        assertThat(mergeState.getRepoName()).isSameAs(repo);
        assertThat(mergeState.getLocalBranchName()).isSameAs(local);
        assertThat(mergeState.getRemoteBranchName()).isSameAs(remote);
        assertThat(mergeState.getStart()).isSameAs(start);
        assertThat(mergeState.getBefore()).isSameAs(before);
        assertThat(mergeState.getLocalHistoryAction()).isSameAs(localHistoryAction);
        assertThat(mergeState.getUpdatedFiles()).isSameAs(updatedFiles);
    }
}