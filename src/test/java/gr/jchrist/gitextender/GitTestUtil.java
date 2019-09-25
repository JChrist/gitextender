package gr.jchrist.gitextender;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;

import static gr.jchrist.gitextender.GitExecutor.*;
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
        git("init --bare");
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
        checkout("master");

        return repo;
    }

    @NotNull
    public static GitRepository registerRepo(Project project, String root) {
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertThat(vcsManager.getAllVcsRoots().length).isNotZero();
        GitRepository repository = GitUtil.getRepositoryManager(project).getRepositoryForRoot(file);
        assertThat(repository).as("Couldn't find repository for root " + root).isNotNull();
        return repository;
    }

    public static void setupGitConfig() {
        git("config user.name 'JChrist'");
        git("config user.email 'j@christ.gr'");
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

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> T overrideProjectComponent(@NotNull Project project, Class<? super T> serviceInterface, Class<T> serviceImplementation) {
        String key = serviceInterface.getName();
        MutablePicoContainer picoContainer = (MutablePicoContainer) project.getPicoContainer();
        picoContainer.unregisterComponent(key);
        picoContainer.registerComponentImplementation(key, serviceImplementation);
        return (T) project.getComponent(serviceInterface);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> T overrideService(Class<? super T> serviceInterface, Class<T> serviceImplementation) {
        String key = serviceInterface.getName();
        MutablePicoContainer picoContainer = (MutablePicoContainer) ApplicationManager.getApplication().getPicoContainer();
        picoContainer.unregisterComponent(key);
        picoContainer.registerComponentImplementation(key, serviceImplementation);
        return (T) ServiceManager.getService(serviceInterface);
    }
}
