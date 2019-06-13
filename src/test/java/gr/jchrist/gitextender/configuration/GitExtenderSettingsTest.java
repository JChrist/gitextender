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
        GitExtenderSettings ges = new GitExtenderSettings();
        assertThat(ges.getAttemptMergeAbort()).as("get failure").isFalse();
        ges.setAttemptMergeAbort(true);
        assertThat(ges.getAttemptMergeAbort()).as("get failure").isTrue();
    }

    @Test
    public void testEquals() throws Exception {
        GitExtenderSettings settings = new GitExtenderSettings();
        GitExtenderSettings settings2 = new GitExtenderSettings();
        assertThat(settings.equals(settings2)).isTrue();
        assertThat(settings.equals(new Object())).isFalse();
        assertThat(settings.equals(new GitExtenderSettings())).isTrue();
        assertThat(settings.equals(new GitExtenderSettings(true, true))).isFalse();
    }

    @Test
    public void testHashcode() throws Exception {
        assertThat(new GitExtenderSettings().hashCode()).isZero();
        assertThat(new GitExtenderSettings(true, false).hashCode()).isOne();
        assertThat(new GitExtenderSettings(false, true).hashCode()).isEqualTo(10);
        assertThat(new GitExtenderSettings(true, true).hashCode()).isEqualTo(11);
    }

    @Test
    public void testToString() throws Exception {
        assertThat(new GitExtenderSettings().toString()).contains("attemptMergeAbort=false").contains("pruneLocals=false");
        assertThat(new GitExtenderSettings(true, false).toString()).contains("attemptMergeAbort=true").contains("pruneLocals=false");
        assertThat(new GitExtenderSettings(false, true).toString()).contains("attemptMergeAbort=false").contains("pruneLocals=true");
        assertThat(new GitExtenderSettings(true, true).toString()).contains("attemptMergeAbort=true").contains("pruneLocals=true");
    }
}