package gr.jchrist.gitextender.configuration;

import gr.jchrist.gitextender.TestingUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
public class ProjectStateTest {
    private TestingUtil.BaseTest base;
    private ProjectState projectState;

    @Before
    public void setUp() throws Exception {
        base = TestingUtil.getBaseTest();
        base.setUp();
        projectState = ProjectState.getInstance(base.getFixture().getProject());
    }

    @After
    public void after() throws Exception {
        base.tearDown();
    }

    @Test
    public void getInstance() throws Exception {
        ProjectState ps = ProjectState.getInstance(base.getFixture().getProject());
        assertThat(ps)
                .as("unexpected instance retrieved")
                .isNotNull();
    }

    @Test
    public void testGetState() throws Exception {
        ProjectState.State state = projectState.getState();
        assertThat(state).as("unexpected state returned")
                .isNotNull();
    }

    @Test
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

    @Test
    public void testGetProjectId() throws Exception {
        final int projectId = 1244221;
        assertThat(projectState.getState()).isNotNull();
        projectState.getState().projectId = projectId;
        assertThat(projectState.getProjectId())
                .as("invalid project id")
                .isEqualTo(projectId);
    }

    @Test
    public void testSetProjectId() throws Exception {
        final int projectId = 4354313;
        assertThat(projectState.getState()).isNotNull();

        projectState.getState().projectId = projectId;
        projectState.setProjectId(projectId);
        assertThat(projectState.getProjectId()).isEqualTo(projectId);
    }

    @Test
    public void testGetLastMergedBranch() throws Exception {
        final String lastMergedBranch = "feature/testfeaturehere";
        assertThat(projectState.getState()).isNotNull();

        projectState.getState().lastMergedBranch = lastMergedBranch;
        assertThat(projectState.getLastMergedBranch()).isEqualTo(lastMergedBranch);
    }

    @Test
    public void testSetLastMergedBranch() throws Exception {
        final String lastMergedBranch = "feature/testfeaturehere";
        projectState.setLastMergedBranch(lastMergedBranch);
        assertThat(projectState.getLastMergedBranch()).isEqualTo(lastMergedBranch);
    }
}