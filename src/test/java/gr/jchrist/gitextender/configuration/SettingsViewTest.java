package gr.jchrist.gitextender.configuration;

import gr.jchrist.gitextender.GitTestUtil;
import gr.jchrist.gitextender.TestingUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/15
 */
public class SettingsViewTest {
    private TestingUtil.BaseTest base;
    private SettingsView settingsView;

    @Before
    public void before() throws Exception {
        base = TestingUtil.getBaseTest();
        base.setUp();
        GitTestUtil.overrideService(GitExtenderSettings.class, GitExtenderSettings.class);
        GitExtenderSettings settings = new GitExtenderSettings();
        assertThat(settings).isNotNull();
        settingsView = new SettingsView();
    }

    @After
    public void after() throws Exception {
        base.tearDown();
    }

    @Test
    public void testSetup() throws Exception {
        settingsView.setup();
        assertThat(settingsView.getMainPanel()).isNotNull();
        assertThat(settingsView.getAttemptMergeAbort()).isNotNull();
    }

    @Test
    public void testGetId() throws Exception {
        assertThat(settingsView.getId()).isEqualTo(SettingsView.DIALOG_TITLE);
    }

    @Test
    public void testGetDisplayName() throws Exception {
        assertThat(settingsView.getDisplayName()).isEqualTo(SettingsView.DIALOG_TITLE);
    }

    @Test
    public void testGetHelpTopic() throws Exception {
        assertThat(settingsView.getHelpTopic()).isNull();
    }

    @Test
    public void testCreateComponent() throws Exception {
        assertThat(settingsView.createComponent()).isNotNull();
    }

    @Test
    public void testIsModifiedApply() throws Exception {
        assertThat(settingsView.isModified()).isFalse();
        settingsView.getAttemptMergeAbort().setSelected(true);
        assertThat(settingsView.isModified()).isTrue();
        settingsView.apply();
        assertThat(settingsView.isModified()).isFalse();
        assertThat(settingsView.getAttemptMergeAbort().isSelected()).isTrue();
    }

    @Test
    public void testDisposeUIResources() throws Exception {
        settingsView.disposeUIResources();
        assertThat(settingsView.getMainPanel()).isNull();
        assertThat(settingsView.getAttemptMergeAbort()).isNull();
    }
}