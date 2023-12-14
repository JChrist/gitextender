package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.LightPlatform4TestCase;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/15
 */
public class SettingsViewTest extends LightPlatform4TestCase {
    private SettingsView settingsView;

    @Before
    public void before() throws Exception {
        settingsView = new SettingsView();
        if (settingsView.isModified()) {
            settingsView.apply();
        }

        assertThat(settingsView.isModified()).isFalse();
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
    public void testPruneLocalsIsModifiedApply() throws Exception {
        assertThat(settingsView.getPruneLocals().isSelected()).isFalse();
        assertThat(settingsView.isModified()).isFalse();
        settingsView.getPruneLocals().setSelected(true);
        assertThat(settingsView.isModified()).isTrue();
        settingsView.apply();
        assertThat(settingsView.isModified()).isFalse();
        assertThat(settingsView.getPruneLocals().isSelected()).isTrue();
    }

    @Test
    public void testDisposeUIResources() throws Exception {
        settingsView.disposeUIResources();
        assertThat(settingsView.getMainPanel()).isNull();
        assertThat(settingsView.getAttemptMergeAbort()).isNull();
        assertThat(settingsView.getPruneLocals()).isNull();
    }
}
