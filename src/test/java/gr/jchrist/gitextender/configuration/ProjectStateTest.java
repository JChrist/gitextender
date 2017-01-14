package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
public class ProjectStateTest extends LightPlatformCodeInsightFixtureTestCase {
    private ProjectState projectState;

    public void setUp() throws Exception {
        super.setUp();
        projectState = ProjectState.getInstance(myFixture.getProject());
    }

    public void testGetInstance() throws Exception {
        ProjectState ps = ProjectState.getInstance(myFixture.getProject());
        assertThat(ps)
                .as("unexpected instance retrieved")
                .isNotNull();
    }

    public void testGetState() throws Exception {
        ProjectState.State state = projectState.getState();
        assertThat(state).as("unexpected state returned")
                .isNotNull();
    }

    public void testLoadState() throws Exception {
        ProjectState.State loaded = new ProjectState.State();
        loaded.projectId = 5533232;
        loaded.lastMergedBranch = "123123sadasdsad";
        projectState.loadState(loaded);

        assertThat(projectState.getState())
                .as("unexpected loaded state")
                .isNotNull()
                .isEqualToComparingFieldByField(loaded);
    }

    public void testGetProjectId() throws Exception {
        final int projectId = 1244221;
        assertThat(projectState.getState()).isNotNull();
        projectState.getState().projectId = projectId;
        assertThat(projectState.getProjectId())
                .as("invalid project id")
                .isEqualTo(projectId);
    }

    public void testSetProjectId() throws Exception {
        final int projectId = 4354313;
        assertThat(projectState.getState()).isNotNull();

        projectState.getState().projectId = projectId;
        projectState.setProjectId(projectId);
        assertThat(projectState.getProjectId()).isEqualTo(projectId);
    }

    public void testGetLastMergedBranch() throws Exception {
        final String lastMergedBranch = "feature/testfeaturehere";
        assertThat(projectState.getState()).isNotNull();

        projectState.getState().lastMergedBranch = lastMergedBranch;
        assertThat(projectState.getLastMergedBranch()).isEqualTo(lastMergedBranch);
    }

    public void testSetLastMergedBranch() throws Exception {
        final String lastMergedBranch = "feature/testfeaturehere";
        projectState.setLastMergedBranch(lastMergedBranch);
        assertThat(projectState.getLastMergedBranch()).isEqualTo(lastMergedBranch);
    }
}