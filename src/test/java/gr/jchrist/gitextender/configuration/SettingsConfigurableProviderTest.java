package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.options.Configurable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jchrist
 * @since 2017/01/14
 */
public class SettingsConfigurableProviderTest {
    @Test
    public void createConfigurable() throws Exception {
        SettingsConfigurableProvider scp = new SettingsConfigurableProvider();
        Configurable c = scp.createConfigurable();
        assertThat(c)
                .as("unexpected configurable created")
                .isNotNull()
                .isOfAnyClassIn(SettingsView.class);
    }
}