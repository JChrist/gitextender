package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsDialog extends DialogWrapper {

    private SettingsView settingsView = new SettingsView();

    public SettingsDialog(@Nullable Project project) {
        super(project);
        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle("Git Extender Settings");
        settingsView.setup();
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        //return settingsView.doValidate();
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return settingsView.createComponent();
    }

    public boolean isModified() {
        return settingsView.isModified();
    }

    public void apply() throws ConfigurationException {
        settingsView.apply();
    }
}
