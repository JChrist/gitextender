package gr.jchrist.gitextender.configuration;

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
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return settingsView.createComponent();
    }

    SettingsView getSettingsView() {
        return settingsView;
    }
}
