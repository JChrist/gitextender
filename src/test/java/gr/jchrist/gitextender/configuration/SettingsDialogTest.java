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
    @Before
    public final void before() throws Exception {
        super.setUp();
    }

    @After
    public final void after() throws Exception {
        super.tearDown();
    }

    @Test
    public void testInit() throws Exception {
        super.invokeTestRunnable(() -> {
            SettingsDialog sd = new SettingsDialog(super.getProject());
            assertThat(sd).isNotNull();
            assertThat(sd.getSettingsView()).isNotNull();
            assertThat(sd.getSettingsView().getMainPanel()).isNotNull();
        });
    }

    @Test
    public void testValidation() throws Exception {
        super.invokeTestRunnable(() -> {
            SettingsDialog sd = new SettingsDialog(super.getProject());
            assertThat(sd.doValidate()).isNull();
        });
    }
}
