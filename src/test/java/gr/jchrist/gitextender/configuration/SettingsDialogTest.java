package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
@RunWith(JUnit4.class)
public class SettingsDialogTest extends BasePlatformTestCase {

    @Test
    public void testInit() throws Throwable {
        super.runTestRunnable(() -> {
            SettingsDialog sd = new SettingsDialog(super.getProject());
            assertThat(sd).isNotNull();
            assertThat(sd.getSettingsView()).isNotNull();
            assertThat(sd.getSettingsView().getMainPanel()).isNotNull();
        });
    }

    @Test
    public void testValidation() throws Throwable {
        super.runTestRunnable(() -> {
            SettingsDialog sd = new SettingsDialog(super.getProject());
            assertThat(sd.doValidate()).isNull();
        });
    }
}
