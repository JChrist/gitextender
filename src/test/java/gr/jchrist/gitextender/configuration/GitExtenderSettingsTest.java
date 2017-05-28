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
    public void testGetAttemptMergeAbort() throws Exception {
        GitExtenderSettings ges = new GitExtenderSettings(false);
        assertThat(ges.getAttemptMergeAbort()).as("get failure").isFalse();
        ges.setAttemptMergeAbort(true);
        assertThat(ges.getAttemptMergeAbort()).as("get failure").isTrue();
    }
}