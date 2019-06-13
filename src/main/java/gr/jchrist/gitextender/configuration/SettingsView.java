package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsView implements SearchableConfigurable {
    public static final String DIALOG_TITLE = "GitExtender Settings";
    private GitExtenderSettingsHandler settingsHandler;
    private GitExtenderSettings originalSavedSettings;

    private JPanel mainPanel;
    private JCheckBox attemptMergeAbort;
    private JCheckBox pruneLocals;

    public SettingsView() {
        super();

        this.settingsHandler = new GitExtenderSettingsHandler();
        this.originalSavedSettings = settingsHandler.loadSettings();
    }

    @NotNull
    @Override
    public String getId() {
        return DIALOG_TITLE;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        GitExtenderSettings selected = selected();
        return selected == null || !selected.equals(originalSavedSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        GitExtenderSettings selected = selected();
        settingsHandler.saveSettings(selected);
        originalSavedSettings = settingsHandler.loadSettings();
    }

    @Override
    public void reset() {
        attemptMergeAbort.setSelected(originalSavedSettings != null && originalSavedSettings.getAttemptMergeAbort());
        pruneLocals.setSelected(originalSavedSettings != null && originalSavedSettings.getAttemptMergeAbort());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        attemptMergeAbort = null;
        pruneLocals = null;
    }

    protected GitExtenderSettings selected() {
        return new GitExtenderSettings(attemptMergeAbort != null && attemptMergeAbort.isSelected(),
                pruneLocals != null && pruneLocals.isSelected());
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    JCheckBox getAttemptMergeAbort() {
        return attemptMergeAbort;
    }

    JCheckBox getPruneLocals() {
        return pruneLocals;
    }
}