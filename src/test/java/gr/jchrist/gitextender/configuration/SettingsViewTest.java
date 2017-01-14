package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/15
 */
public class SettingsViewTest extends LightPlatformCodeInsightFixtureTestCase {
    private SettingsView settingsView;

    public void setUp() throws Exception {
        super.setUp();
        settingsView = new SettingsView();
    }

    public void testSetup() throws Exception {
        settingsView.setup();
        assertThat(settingsView.getMainPanel()).isNotNull();
        assertThat(settingsView.getAttemptMergeAbort()).isNotNull();
    }

    public void testGetId() throws Exception {
        assertThat(settingsView.getId()).isEqualTo(SettingsView.DIALOG_TITLE);
    }

    public void testGetDisplayName() throws Exception {
        assertThat(settingsView.getDisplayName()).isEqualTo(SettingsView.DIALOG_TITLE);
    }

    public void testGetHelpTopic() throws Exception {
        assertThat(settingsView.getHelpTopic()).isNull();
    }

    public void testCreateComponent() throws Exception {
        assertThat(settingsView.createComponent()).isNotNull();
    }

    public void testIsModifiedApply() throws Exception {
        assertThat(settingsView.isModified()).isTrue();
        settingsView.apply();
        assertThat(settingsView.isModified()).isFalse();
    }

    public void testDisposeUIResources() throws Exception {
        settingsView.disposeUIResources();
        assertThat(settingsView.getMainPanel()).isNull();
        assertThat(settingsView.getAttemptMergeAbort()).isNull();
    }

    public void testFill() throws Exception {
        assertThat(settingsView.getAttemptMergeAbort().isSelected()).isFalse();
        GitExtenderSettings ges = new GitExtenderSettings();
        ges.attemptMergeAbort = true;
        settingsView.fill(ges);
        assertThat(settingsView.getAttemptMergeAbort().isSelected()).isTrue();
    }

    public void testSave() throws Exception {
        assertThat(settingsView.save().getAttemptMergeAbort()).isFalse();
        GitExtenderSettings ges = new GitExtenderSettings();
        ges.attemptMergeAbort = true;
        settingsView.fill(ges);
        assertThat(settingsView.save().getAttemptMergeAbort()).isTrue();
    }
}