package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
public class SettingsDialogTest extends LightPlatformCodeInsightFixtureTestCase {

    public void testInit() throws Exception {
        SettingsDialog sd = new SettingsDialog(myFixture.getProject());
        assertThat(sd).isNotNull();
        assertThat(sd.getSettingsView()).isNotNull();
        assertThat(sd.getSettingsView().getMainPanel()).isNotNull();
    }

    public void testValidation() throws Exception {
        SettingsDialog sd = new SettingsDialog(myFixture.getProject());
        assertThat(sd.doValidate()).isNull();
    }
}