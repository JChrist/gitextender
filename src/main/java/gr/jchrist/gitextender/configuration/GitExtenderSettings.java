package gr.jchrist.gitextender.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

@State(
        name = "GitExtenderSettings",
        storages = {
                @Storage(id = "default", file = "$APP_CONFIG$/gitextender-settings.xml")
        }
)
public class GitExtenderSettings implements PersistentStateComponent<GitExtenderSettings> {
    public boolean attemptMergeAbort;

    public static GitExtenderSettings getInstance() {
        return ServiceManager.getService(GitExtenderSettings.class);
    }

    @Nullable
    @Override
    public GitExtenderSettings getState() {
        return this;
    }

    @Override
    public void loadState(GitExtenderSettings gitExtenderSettings) {
        XmlSerializerUtil.copyBean(gitExtenderSettings, this);
    }

    public boolean getAttemptMergeAbort() {
        return attemptMergeAbort;
    }

    public void setAttemptMergeAbort(boolean attemptMergeAbort) {
        this.attemptMergeAbort = attemptMergeAbort;
    }
}
