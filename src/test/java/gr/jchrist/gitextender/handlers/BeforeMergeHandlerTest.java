package gr.jchrist.gitextender.handlers;

import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRevisionNumber;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class BeforeMergeHandlerTest {
    private final String repo = "repo";
    private final String local = "local";
    private final String remote = "remote";
    @Mocked Project project;
    @Mocked VirtualFile root;
    private BeforeMergeHandler handler;

    @Before
    public void before() throws Exception {
        handler = new BeforeMergeHandler(project, root, repo, local, remote);
    }

    @Test
    public void beforeMerge(
            @Mocked final GitRevisionNumber start,
            @Mocked final LocalHistory localHistory,
            @Mocked final Label before,
            @Mocked final LocalHistoryAction localHistoryAction
    ) throws Exception {
        final String expectedLabel = handler.getBeforeLocalHistoryLabel();
        new Expectations() {{
            GitRevisionNumber.resolve(project, root, "HEAD"); result = start;
            localHistory.putSystemLabel(project, expectedLabel); result = before;
            localHistory.startAction(expectedLabel); result = localHistoryAction;
        }};
        MergeState state = handler.beforeMerge();

        assertThat(state)
                .as("unexpected merge state returned from before merge handler")
                .isEqualToComparingFieldByField(
                        new MergeState(project, root, repo, local, remote,
                                start, before, localHistoryAction,
                                state.getUpdatedFiles()));

        assertThat(state.getUpdatedFiles().isEmpty())
                .as("expected initial state of updated files to be empty")
                .isTrue();

    }

    @Test
    public void beforeMergeFailedToGetRevision(
            @Mocked final GitRevisionNumber start,
            @Mocked final LocalHistory localHistory,
            @Mocked final Label before,
            @Mocked final LocalHistoryAction localHistoryAction
    ) throws Exception {
        final String expectedLabel = handler.getBeforeLocalHistoryLabel();
        new Expectations() {{
            GitRevisionNumber.resolve(project, root, "HEAD");
            result = new Exception("test exception trying to get revision number");
            localHistory.putSystemLabel(project, expectedLabel); result = before;
            localHistory.startAction(expectedLabel); result = localHistoryAction;
        }};
        MergeState state = handler.beforeMerge();

        assertThat(state)
                .as("unexpected merge state returned from before merge handler")
                .isEqualToComparingFieldByField(
                        new MergeState(project, root, repo, local, remote,
                                null, before, localHistoryAction,
                                state.getUpdatedFiles()));

        assertThat(state.getUpdatedFiles().isEmpty())
                .as("expected initial state of updated files to be empty")
                .isTrue();
    }
}