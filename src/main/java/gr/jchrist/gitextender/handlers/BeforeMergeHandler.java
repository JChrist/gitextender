package gr.jchrist.gitextender.handlers;

import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitRevisionNumber;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

public class BeforeMergeHandler {
    private final Project project;
    private final VirtualFile root;
    private final GitRepository repo;
    private final String repoName;
    private final String localBranchName;
    private final String remoteBranchName;

    public BeforeMergeHandler(
            @NotNull Project project,
            @NotNull VirtualFile root,
            @NotNull GitRepository repo,
            @NotNull String repoName,
            @NotNull String localBranchName,
            @NotNull String remoteBranchName) {
        this.project = project;
        this.root = root;
        this.repo = repo;
        this.repoName = repoName;
        this.localBranchName = localBranchName;
        this.remoteBranchName = remoteBranchName;
    }

    @NotNull
    public MergeState beforeMerge() {
        //record the starting revision number
        GitRevisionNumber start;
        try {
            start = GitRevisionNumber.resolve(project, root, "HEAD");
        } catch (Exception e) {
            //we failed to get a revision number, that shouldn't stop us from updating the branch
            start = null;
        }

        final String beforeLabel = getBeforeLocalHistoryLabel();
        Label before = LocalHistory.getInstance().putSystemLabel(project, beforeLabel);
        LocalHistoryAction myLocalHistoryAction = LocalHistory.getInstance().startAction(beforeLabel);

        //keep track of all updated files
        UpdatedFiles updatedFiles = UpdatedFiles.create();
        return new MergeState(project, root, repo, repoName, localBranchName, remoteBranchName,
                start, before, myLocalHistoryAction, updatedFiles);
    }

    protected String getBeforeLocalHistoryLabel() {
        return "Before updating:" + repoName + " " + localBranchName;
    }
}
