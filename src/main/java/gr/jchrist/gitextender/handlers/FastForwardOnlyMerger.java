package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import org.jetbrains.annotations.NotNull;

public class FastForwardOnlyMerger {
    private final Git git;
    private final Project project;
    private final VirtualFile root;
    private final String remoteBranchName;

    public FastForwardOnlyMerger(
            @NotNull Git git,
            @NotNull Project project,
            @NotNull VirtualFile root,
            @NotNull String remoteBranchName) {
        this.git = git;
        this.project = project;
        this.root = root;
        this.remoteBranchName = remoteBranchName;
    }

    @NotNull
    public GitCommandResult mergeFastForward() {
        //merge with -ff only
        GitCommand merge = GitCommand.MERGE;
        GitLineHandler mergeHandler = new GitLineHandler(project, root, merge);
        mergeHandler.addParameters("--ff-only");
        mergeHandler.addParameters(remoteBranchName);
        return git.runCommand(mergeHandler);
    }
}
