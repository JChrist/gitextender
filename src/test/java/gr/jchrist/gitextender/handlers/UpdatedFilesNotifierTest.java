package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdatedFilesNotifierTest {
    @Test
    public void prepareNotificationWithUpdateInfo() throws Exception {
        UpdatedFiles uf = UpdatedFiles.create();
        uf.getTopLevelGroups().get(0).add("test path", "test repo name", null);
        String result = new UpdatedFilesNotifier(uf).prepareNotificationWithUpdateInfo();
        assertThat(result).isNotNull().isNotEmpty();
    }
}