package gr.jchrist.gitextender.configuration;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GitExtenderSettingsHandler {
    static final String PREFIX = "gr.jchrist.gitextender.";
    static final String ATTEMPT_MERGE_ABORT_KEY = PREFIX + "attemptMergeAbort";

    public GitExtenderSettingsHandler() {
    }

    public GitExtenderSettings loadSettings() {
        return new GitExtenderSettings(getAttemptMergeAbort());
    }

    public void saveSettings(GitExtenderSettings settings) {
        setAttemptMergeAbort(settings.getAttemptMergeAbort());
    }
    private boolean getAttemptMergeAbort() {
        return getApplicationProperties().getBoolean(ATTEMPT_MERGE_ABORT_KEY);
    }

    private void setAttemptMergeAbort(boolean attemptMergeAbort) {
        getApplicationProperties().setValue(ATTEMPT_MERGE_ABORT_KEY, attemptMergeAbort);
    }

    @NotNull
    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
