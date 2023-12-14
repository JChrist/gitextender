package gr.jchrist.gitextender;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static gr.jchrist.gitextender.GitExecutor.addCommit;
import static gr.jchrist.gitextender.GitExecutor.append;
import static gr.jchrist.gitextender.GitExecutor.cd;
import static gr.jchrist.gitextender.GitExecutor.checkout;
import static gr.jchrist.gitextender.GitExecutor.git;
import static gr.jchrist.gitextender.GitExecutor.last;
import static gr.jchrist.gitextender.GitExecutor.push;
import static gr.jchrist.gitextender.GitExecutor.tac;
import static org.assertj.core.api.Assertions.assertThat;

public class GitTestUtil {
    public static void cloneRepo(@NotNull String source, @NotNull String destination, boolean bare) {
        cd(source);
        git("clone "+(bare ? "--bare " : "") + "-- . " + destination);
    }

    @NotNull
    public static GitRepository createRemoteRepositoryAndCloneToLocal(
            @NotNull Project project, @NotNull String root, @NotNull String remotePath, @NotNull String remoteAccessPath) {
        cd(remotePath);
        git("init --bare --initial-branch=" + ProjectUpdateITest.MAIN_BRANCH_NAME);
        setupGitConfig();

        cloneRepo(remotePath, remoteAccessPath, false);
        cd(remoteAccessPath);
        setupGitConfig();

        tac("initial_file.txt");
        push();

        checkout("-b", "develop");
        push("origin",  "develop");

        cloneRepo(remotePath, root, false);

        VirtualFile gitDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(root, GitUtil.DOT_GIT));
        assertThat(gitDir).isNotNull();
        GitRepository repo = registerRepo(project, root);

        cd(root);
        setupGitConfig();
        checkout("develop");
        checkout("main");

        return repo;
    }

    @NotNull
    public static GitRepository registerRepo(Project project, String root) {
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertThat(vcsManager.getAllVcsRoots().length).isNotZero();
        GitRepositoryManager grm = GitUtil.getRepositoryManager(project);

        AtomicReference<GitRepository> ref = new AtomicReference<>();
        AbstractIT.runOutOfEdt(() -> {
            var repository = grm.getRepositoryForRoot(file);
            ref.set(repository);
        });
        var repository = ref.get();
        assertThat(repository).as("Couldn't find repository for root " + root).isNotNull();
        return repository;
    }

    public static void setupGitConfig() {
        git("config user.name 'JChrist'");
        git("config user.email 'jchrist@jchrist.gr'");
        git("config push.default simple");
    }

    @NotNull
    public static String makeCommit(String file) throws IOException {
        append(file, "some content");
        addCommit("some message");
        return last();
    }

    public static Notification assertNotification(@NotNull NotificationType type,
                                                  @NotNull String title,
                                                  @NotNull String content,
                                                  @NotNull Notification actual) {
        assertThat(actual.getType()).as("Incorrect notification type: " + tos(actual)).isEqualTo(type);
        assertThat(actual.getTitle()).as("Incorrect notification title: " + tos(actual)).isEqualTo(title);
        assertThat(cleanupForAssertion(actual.getContent()))
                .as("Incorrect notification content: " + tos(actual))
                .isEqualTo(cleanupForAssertion(content));
        return actual;
    }

    @NotNull
    public static String cleanupForAssertion(@NotNull String content) {
        return content.replace("<br/>", "\n").replace("\n", " ").replaceAll("[ ]{2,}", " ").replaceAll(" href='[^']*'", "").trim();
    }

    @NotNull
    private static String tos(@NotNull Notification notification) {
        return notification.getTitle() + "|" + notification.getContent();
    }
}
