package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdatedFilesNotifier {
    private final UpdatedFiles updatedFiles;

    public UpdatedFilesNotifier(@NotNull UpdatedFiles updatedFiles) {
        this.updatedFiles = updatedFiles;
    }

    @NotNull
    public String prepareNotificationWithUpdateInfo() {
        StringBuilder text = new StringBuilder();
        final List<FileGroup> groups = updatedFiles.getTopLevelGroups();
        for (FileGroup group : groups) {
            appendGroup(text, group);
        }
        return text.toString();
    }

    private void appendGroup(final StringBuilder text, final FileGroup group) {
        final int s = group.getFiles().size();
        if (s > 0) {
            text.append("\n");
            text.append(s).append(" ").append(StringUtil.pluralize("File", s)).append(" ").append(group.getUpdateName());
        }

        final List<FileGroup> list = group.getChildren();
        for (FileGroup g : list) {
            appendGroup(text, g);
        }
    }
}
