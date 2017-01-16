package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SimpleMerger {
    private final Git git;
    private final Project project;
    private final VirtualFile root;
    private final String remoteBranchName;

    private GitCommandResult mergeResult;
    private GitCommandResult abortResult;

    public SimpleMerger(
            @NotNull Git git,
            @NotNull Project project,
            @NotNull VirtualFile root,
            @NotNull String remoteBranchName
    ) {
        this.git = git;
        this.project = project;
        this.root = root;
        this.remoteBranchName = remoteBranchName;
    }

    public void mergeAbortIfFailed() {
        merge();

        if (mergeResult.success()) {
            return;
        }

        abortMerge();
    }

    protected void merge() {
        //try simple merging, abort if failed
        GitCommand merge = GitCommand.MERGE;
        GitLineHandler mergeHandler = new GitLineHandler(project, root, merge);
        mergeHandler.addParameters(remoteBranchName);
        mergeResult = git.runCommand(mergeHandler);
    }

    protected void abortMerge() {
        //we failed, now abort the process
        GitCommand abort = GitCommand.MERGE;
        GitLineHandler abortHandler = new GitLineHandler(project, root, abort);
        abortHandler.addParameters("--abort");
        abortResult = git.runCommand(abortHandler);

        if (!abortResult.success()) {
            //We have really messed up! What can I do now?
        }
    }

    @NotNull
    public GitCommandResult getMergeResult() {
        return mergeResult;
    }

    @Nullable
    public GitCommandResult getAbortResult() {
        return abortResult;
    }
}
