package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsView implements SearchableConfigurable {

    public static final String DIALOG_TITLE = "GitExtender Settings";
    GitExtenderSettings gitExtenderSettings = GitExtenderSettings.getInstance();

    private JPanel mainPanel;
    private JCheckBox attemptMergeAbort;

    public SettingsView() {
        super();
    }

    public void setup() {

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
        GitExtenderSettings save = save();
        return save == null
                || !save.getAttemptMergeAbort().equals(gitExtenderSettings.getAttemptMergeAbort());
    }

    @Override
    public void apply() throws ConfigurationException {
        GitExtenderSettings save = save();
        gitExtenderSettings.setAttemptMergeAbort(save.getAttemptMergeAbort());
    }

    @Override
    public void reset() {
        fill(gitExtenderSettings);
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        attemptMergeAbort = null;
    }

    protected void fill(GitExtenderSettings gitExtenderSettings) {
        attemptMergeAbort.setSelected(gitExtenderSettings == null || gitExtenderSettings.getAttemptMergeAbort() == null ? false :
                gitExtenderSettings.getAttemptMergeAbort());
    }

    protected GitExtenderSettings save() {
        GitExtenderSettings ss = new GitExtenderSettings();
        ss.attemptMergeAbort = attemptMergeAbort != null && attemptMergeAbort.isSelected();
        return ss;
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    JCheckBox getAttemptMergeAbort() {
        return attemptMergeAbort;
    }
}