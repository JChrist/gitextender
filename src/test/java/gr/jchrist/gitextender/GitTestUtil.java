package gr.jchrist.gitextender;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.VcsLogProvider;
import com.intellij.vcs.log.impl.VcsLogManager;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.log.GitLogProvider;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static gr.jchrist.gitextender.GitExecutor.*;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
        setGitUser();

        cloneRepo(remotePath, remoteAccessPath, false);
        cd(remoteAccessPath);
        setGitUser();

        tac("initial_file.txt");
        push();

        checkout("-b", "develop");
        push("origin",  "develop");

        cloneRepo(remotePath, root, false);

        VirtualFile gitDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(root, GitUtil.DOT_GIT));
        assertNotNull(gitDir);
        GitRepository repo = registerRepo(project, root);

        cd(root);
        setGitUser();
        checkout("develop");
        checkout("master");

        return repo;
    }

    @NotNull
    public static GitRepository registerRepo(Project project, String root) {
        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
        vcsManager.setDirectoryMapping(root, GitVcs.NAME);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(root));
        assertFalse(vcsManager.getAllVcsRoots().length == 0);
        GitRepository repository = GitUtil.getRepositoryManager(project).getRepositoryForRoot(file);
        assertNotNull("Couldn't find repository for root " + root, repository);
        return repository;
    }

    public static void setGitUser() {
        git("config user.name 'JChrist'");
        git("config user.email 'j@christ.gr'");
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
        assertEquals("Incorrect notification type: " + tos(actual), type, actual.getType());
        assertEquals("Incorrect notification title: " + tos(actual), title, actual.getTitle());
        assertEquals("Incorrect notification content: " + tos(actual), cleanupForAssertion(content), cleanupForAssertion(actual.getContent()));
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

    public static GitLogProvider findGitLogProvider(@NotNull Project project) {
        List<VcsLogProvider> providers =
                ContainerUtil.filter(Extensions.getExtensions(VcsLogManager.LOG_PROVIDER_EP, project),
                        provider -> provider.getSupportedVcs().equals(GitVcs.getKey()));
        assertEquals("Incorrect number of GitLogProviders", 1, providers.size());
        return (GitLogProvider)providers.get(0);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> T overrideService(@NotNull Project project, Class<? super T> serviceInterface, Class<T> serviceImplementation) {
        String key = serviceInterface.getName();
        MutablePicoContainer picoContainer = (MutablePicoContainer) project.getPicoContainer();
        picoContainer.unregisterComponent(key);
        picoContainer.registerComponentImplementation(key, serviceImplementation);
        return (T) ServiceManager.getService(project, serviceInterface);
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
