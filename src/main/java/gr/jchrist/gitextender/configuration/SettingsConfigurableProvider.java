package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import org.jetbrains.annotations.Nullable;

public class SettingsConfigurableProvider extends ConfigurableProvider {

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new SettingsView();
    }
}
