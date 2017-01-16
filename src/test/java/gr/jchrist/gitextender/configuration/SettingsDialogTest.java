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
public class SettingsDialogTest {
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
    public void testInit() throws Exception {
        base.invokeTestRunnable(() -> {
            SettingsDialog sd = new SettingsDialog(base.getFixture().getProject());
            assertThat(sd).isNotNull();
            assertThat(sd.getSettingsView()).isNotNull();
            assertThat(sd.getSettingsView().getMainPanel()).isNotNull();
        });
    }

    @Test
    public void testValidation() throws Exception {
        base.invokeTestRunnable(() -> {
            SettingsDialog sd = new SettingsDialog(base.getFixture().getProject());
            assertThat(sd.doValidate()).isNull();
        });
    }
}