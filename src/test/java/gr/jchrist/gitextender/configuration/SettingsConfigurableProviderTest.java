package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.options.Configurable;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
@RunWith(JMockit.class)
public class SettingsConfigurableProviderTest {
    @Test
    public void createConfigurable(
            @Mocked final SettingsView settingsView
    ) throws Exception {
        SettingsConfigurableProvider scp = new SettingsConfigurableProvider();
        Configurable c = scp.createConfigurable();
        assertThat(c)
                .as("unexpected configurable created")
                .isNotNull()
                .isOfAnyClassIn(SettingsView.class);
    }
}