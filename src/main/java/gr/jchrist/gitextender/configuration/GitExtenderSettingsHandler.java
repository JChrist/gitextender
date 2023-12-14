package gr.jchrist.gitextender.configuration;

import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.NotNull;

public class GitExtenderSettingsHandler {
    static final String PREFIX = "gr.jchrist.gitextender.";
    static final String ATTEMPT_MERGE_ABORT_KEY = PREFIX + "attemptMergeAbort";
    static final String PRUNE_LOCALS_KEY = PREFIX + "pruneLocals";

    public GitExtenderSettingsHandler() {
    }

    public GitExtenderSettings loadSettings() {
        return new GitExtenderSettings(getAttemptMergeAbort(), getPruneLocals());
    }

    public void saveSettings(GitExtenderSettings settings) {
        setAttemptMergeAbort(settings.getAttemptMergeAbort());
        setPruneLocals(settings.getPruneLocals());
    }

    private boolean getAttemptMergeAbort() {
        return getApplicationProperties().getBoolean(ATTEMPT_MERGE_ABORT_KEY);
    }

    private boolean getPruneLocals() {
        return getApplicationProperties().getBoolean(PRUNE_LOCALS_KEY);
    }

    private void setAttemptMergeAbort(boolean attemptMergeAbort) {
        getApplicationProperties().setValue(ATTEMPT_MERGE_ABORT_KEY, attemptMergeAbort);
    }

    private void setPruneLocals(boolean pruneLocals) {
        getApplicationProperties().setValue(PRUNE_LOCALS_KEY, pruneLocals);
    }

    @NotNull
    private PropertiesComponent getApplicationProperties() {
        return PropertiesComponent.getInstance();
    }
}
