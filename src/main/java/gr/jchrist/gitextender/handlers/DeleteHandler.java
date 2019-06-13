package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import org.jetbrains.annotations.NotNull;

public class DeleteHandler {
    public static final String DELETE_FLAG = "-D";
    private final Git git;
    private final Project project;
    private final VirtualFile root;

    public DeleteHandler(
            @NotNull Git git,
            @NotNull Project project,
            @NotNull VirtualFile root) {
        this.git = git;
        this.project = project;
        this.root = root;
    }

    @NotNull
    public GitCommandResult delete(String localBranchName) {
        GitCommand checkout = GitCommand.BRANCH;
        GitLineHandler deleteHandler = new GitLineHandler(project, root, checkout);
        deleteHandler.addParameters(DELETE_FLAG, localBranchName);
        return git.runCommand(deleteHandler);
    }
}
