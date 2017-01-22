package gr.jchrist.gitextender.handlers;

import com.intellij.history.Label;
import com.intellij.history.LocalHistoryAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRevisionNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MergeState {
    private final Project project;
    private final VirtualFile root;
    private final String repoName;
    private final String localBranchName;
    private final String remoteBranchName;
    private final GitRevisionNumber start;
    private final Label before;
    private final LocalHistoryAction localHistoryAction;
    private final UpdatedFiles updatedFiles;

    //for tests
    MergeState() {
        this.project = null;
        this.root = null;
        this.repoName = null;
        this.localBranchName = null;
        this.remoteBranchName = null;
        this.start = null;
        this.before = null;
        this.localHistoryAction = null;
        this.updatedFiles = null;
    }

    public MergeState(
            @NotNull Project project, @NotNull VirtualFile root,
            @NotNull String repoName, @NotNull String localBranchName, @NotNull String remoteBranchName,
            @Nullable GitRevisionNumber start, @NotNull Label before,
            @NotNull LocalHistoryAction localHistoryAction, @NotNull UpdatedFiles updatedFiles) {
        this.project = project;
        this.root = root;
        this.repoName = repoName;
        this.localBranchName = localBranchName;
        this.remoteBranchName = remoteBranchName;
        this.start = start;
        this.before = before;
        this.localHistoryAction = localHistoryAction;
        this.updatedFiles = updatedFiles;
    }

    public Project getProject() {
        return project;
    }

    public VirtualFile getRoot() {
        return root;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getLocalBranchName() {
        return localBranchName;
    }

    public String getRemoteBranchName() {
        return remoteBranchName;
    }

    public GitRevisionNumber getStart() {
        return start;
    }

    public Label getBefore() {
        return before;
    }

    public LocalHistoryAction getLocalHistoryAction() {
        return localHistoryAction;
    }

    public UpdatedFiles getUpdatedFiles() {
        return updatedFiles;
    }
}
