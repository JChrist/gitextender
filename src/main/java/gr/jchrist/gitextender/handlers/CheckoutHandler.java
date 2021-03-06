package gr.jchrist.gitextender.handlers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import org.jetbrains.annotations.NotNull;

public class CheckoutHandler {
    private final Git git;
    private final Project project;
    private final VirtualFile root;

    public CheckoutHandler(
            @NotNull Git git,
            @NotNull Project project,
            @NotNull VirtualFile root) {
        this.git = git;
        this.project = project;
        this.root = root;
    }

    @NotNull
    public GitCommandResult checkout(String localBranchName) {
        GitCommand checkout = GitCommand.CHECKOUT;
        GitLineHandler checkoutHandler = new GitLineHandler(project, root, checkout);
        checkoutHandler.addParameters(localBranchName);
        return git.runCommand(checkoutHandler);
    }
}
