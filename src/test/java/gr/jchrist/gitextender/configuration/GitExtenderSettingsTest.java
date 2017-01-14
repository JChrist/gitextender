package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
public class GitExtenderSettingsTest extends LightPlatformCodeInsightFixtureTestCase {
    public void testGetInstance() throws Exception {
        GitExtenderSettings ges = GitExtenderSettings.getInstance();
        assertThat(ges).isNotNull();

        GitExtenderSettings state = ges.getState();
        assertThat(state).isSameAs(ges);
    }

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

    public void testGetAttemptMergeAbort() throws Exception {
        GitExtenderSettings ges = new GitExtenderSettings();
        ges.attemptMergeAbort = true;
        assertThat(ges.getAttemptMergeAbort())
                .as("get failure")
                .isTrue();
    }

    public void testSetAttemptMergeAbort() throws Exception {
        GitExtenderSettings ges = new GitExtenderSettings();
        ges.setAttemptMergeAbort(true);
        assertThat(ges.attemptMergeAbort)
                .as("set failure")
                .isTrue();
    }
}