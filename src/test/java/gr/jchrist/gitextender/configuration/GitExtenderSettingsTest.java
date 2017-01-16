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
public class GitExtenderSettingsTest {
    private TestingUtil.BaseTest base;

    @Before
    public void before() throws Exception {
        base = TestingUtil.getBaseTest();
        base.setUp();
    }

    @After
    public void after() throws Exception {
        base.tearDown();
    }

    @Test
    public void getInstance() throws Exception {
        GitExtenderSettings ges = GitExtenderSettings.getInstance();
        assertThat(ges).isNotNull();

        GitExtenderSettings state = ges.getState();
        assertThat(state).isSameAs(ges);
    }

    @Test
    public void testLoadState() throws Exception {
        GitExtenderSettings ges = new GitExtenderSettings();
        GitExtenderSettings loaded = new GitExtenderSettings();
        loaded.attemptMergeAbort = true;
        ges.attemptMergeAbort = false;

        ges.loadState(loaded);
        assertThat(ges)
                .as("unexpected settings after loading")
                .isEqualToComparingFieldByField(loaded);
    }

    @Test
    public void testGetAttemptMergeAbort() throws Exception {
        GitExtenderSettings ges = new GitExtenderSettings();
        ges.attemptMergeAbort = true;
        assertThat(ges.getAttemptMergeAbort())
                .as("get failure")
                .isTrue();
    }

    @Test
    public void testSetAttemptMergeAbort() throws Exception {
        GitExtenderSettings ges = new GitExtenderSettings();
        ges.setAttemptMergeAbort(true);
        assertThat(ges.attemptMergeAbort)
                .as("set failure")
                .isTrue();
    }
}