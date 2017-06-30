package gr.jchrist.gitextender;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowHeadlessManagerImpl;
import com.intellij.testFramework.TestDataProvider;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.configuration.GitExtenderSettingsHandler;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static gr.jchrist.gitextender.GitExecutor.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectUpdateITest {
    private static final Logger logger = Logger.getInstance(ProjectUpdateITest.class);

    private static final String remoteName = "testRemote";
    private static final String remoteAccessName = "testRemoteAccess";
    private String remoteRepoPath;
    private String remoteRepoAccessPath;
    private GitRepository repository;

    private TestingUtil.BaseIT base;
    private MessageBusConnection mbc;
    private final List<Notification> capturedInfos = new ArrayList<>();
    private final List<Notification> capturedErrors = new ArrayList<>();

    private AnActionEvent event;
    private GitExtenderUpdateAll updater;
    private GitExtenderSettings settings;
    private GitExtenderSettingsHandler appSettingsHandler;

    @Before
    public void before() throws Exception {
        base = TestingUtil.getBaseIT();
        base.setUp();

        remoteRepoPath = Files.createTempDirectory(remoteName).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        remoteRepoAccessPath = Files.createTempDirectory(remoteAccessName).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        base.getFilesToDelete().add(new File(remoteRepoPath));
        base.getFilesToDelete().add(new File(remoteRepoAccessPath));
        base.getFilesToDelete().add(new File(base.getProjectPath()));

        logger.info("creating test remote in directory: " + remoteRepoPath+" with access (non-bare) in:"+remoteRepoAccessPath);

        repository = GitTestUtil.createRemoteRepositoryAndCloneToLocal(base.getProject(), base.getProjectPath(),
                remoteRepoPath, remoteRepoAccessPath);
        repository.update();
        base.getGitRepositoryManager().updateAllRepositories();

        logger.info("Starting up with repos: " + base.getGitRepositoryManager().getRepositories() +
                " Branch track infos: "+ base.getGitRepositoryManager().getRepositories().get(0).getBranchTrackInfos());

        updater = new GitExtenderUpdateAll();
        event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(base.getProject()));

        //ToolWindowHeadlessManagerImpl tw = new ToolWindowHeadlessManagerImpl(base.getProject());
        GitTestUtil.overrideService(base.getProject(), ToolWindowManager.class, ToolWindowHeadlessManagerImpl.class);
        ToolWindowManager tw = ToolWindowManager.getInstance(base.getProject());
        tw.registerToolWindow(ToolWindowId.VCS, true, ToolWindowAnchor.BOTTOM);

        assertThat(ToolWindowManager.getInstance(base.getProject()).getToolWindow(ToolWindowId.VCS)).isNotNull();

        logger.info("window is: "+ToolWindowManager.getInstance(base.getProject()).getToolWindow(ToolWindowId.VCS));

        Application app = ApplicationManager.getApplication();
        logger.info("initialized app: "+app);
        mbc = app.getMessageBus().connect();
        mbc.setDefaultHandler(
                (event1, params) -> {
                    assertThat(params).hasSize(1);
                    assertThat(params[0]).isInstanceOf(Notification.class);
                    Notification n = (Notification) params[0];
                    logger.info("captured notification: "+n.getTitle()+" "+n.getContent());
                    if(n.getType().equals(NotificationType.ERROR)) {
                        capturedErrors.add(n);
                    } else {
                        capturedInfos.add(n);
                    }
                });
        mbc.subscribe(Notifications.TOPIC);
        logger.info("created message bus connection and subscribed to notifications:"+mbc);
        appSettingsHandler = new GitExtenderSettingsHandler();
        settings = new GitExtenderSettings();
        appSettingsHandler.saveSettings(settings);
    }

    @After
    public void after() throws Exception {
        if(mbc != null) {
            mbc.disconnect();
        }
        base.tearDown();
    }

    @Test
    public void updateNoChanges() throws Exception {
        updater.actionPerformed(event);
        logger.info("update action performed");

        assertNoErrors();
        assertOnMasterBranch();
    }

    @Test
    public void updateRemoteOnlyChanges() throws Exception {
        final String newFileName = "test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(newFileName);
        push();

        cd(base.getProjectPath());
        checkout("master");

        runUpdate();

        assertNoErrors();

        assertOnMasterBranch();

        checkout("develop");
        assertThat(Paths.get(base.getProjectPath(), newFileName)).exists();
    }

    @Test
    public void updateLocalRemoteDivergedOnlyFFNotUpdated() throws Exception {
        final String remoteFileName = "test_new_file.txt";
        final String localFileName = "local_test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(remoteFileName);
        push();

        cd(base.getProjectPath());
        checkout("develop");
        tac(localFileName);
        //not pushing this since it would get rejected (remote is 1 commit ahead)
        checkout("master");

        repository.update();
        base.getGitRepositoryManager().updateAllRepositories();

        runUpdate();

        assertError(Collections.singletonList("fast-forward only"),
                Arrays.asList("Local branch: develop", "Remote branch: origin/develop"));

        assertOnMasterBranch();

        checkout("develop");
        assertThat(Paths.get(base.getProjectPath(), localFileName)).exists();
        assertThat(Paths.get(base.getProjectPath(), remoteFileName)).doesNotExist();
    }

    @Test
    public void updateLocalRemoteDivergedSimpleMergeUpdated() throws Exception {
        final String remoteFileName = "test_new_file.txt";
        final String localFileName = "local_test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(remoteFileName);
        push();

        cd(base.getProjectPath());
        checkout("develop");
        tac(localFileName);
        //not pushing this since it would get rejected (remote is 1 commit ahead)
        checkout("master");

        repository.update();
        base.getGitRepositoryManager().updateAllRepositories();

        //enable merge/abort
        settings.setAttemptMergeAbort(true);
        appSettingsHandler.saveSettings(settings);

        runUpdate();

        assertNoErrors();
        //assertError(Collections.singletonList("fast-forward only"),
        //        Arrays.asList("Local branch: develop", "Remote branch: origin/develop"));

        assertOnMasterBranch();

        checkout("develop");
        assertThat(Paths.get(base.getProjectPath(), localFileName)).exists();
        assertThat(Paths.get(base.getProjectPath(), remoteFileName)).exists();
    }

    @Test
    public void updateLocalRemoteConflictOnFileAborted() throws Exception {
        //add a new file to the remote on develop
        final String fileName = "test_new_file.txt";
        final String remoteContent = "remote content";
        final String localContent = "local content";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(fileName, remoteContent);
        push();

        //and add the same file on our local develop (with different content from the remote)
        cd(base.getProjectPath());
        checkout("develop");
        tac(fileName, localContent);
        //not pushing this since it would get rejected (remote is 1 commit ahead)
        checkout("master");

        repository.update();
        base.getGitRepositoryManager().updateAllRepositories();

        //enable merge/abort
        settings.setAttemptMergeAbort(true);
        appSettingsHandler.saveSettings(settings);

        runUpdate();

        assertError(Collections.singletonList("failed"), Collections.singletonList("fast-forward"),
                Arrays.asList("Local branch: develop", "Remote branch: origin/develop", "Merge was aborted"),
                Collections.singletonList("fast-forward"));

        assertOnMasterBranch();

        checkout("develop");
        assertThat(Paths.get(base.getProjectPath(), fileName))
                .as("expected to abort merge, so local file should be as it was before merging")
                .exists().hasBinaryContent(localContent.getBytes());
    }

    private void runUpdate() {
        Boolean result = WriteCommandAction.runWriteCommandAction(base.getProject(),
                (Computable<Boolean>) ()-> {updater.actionPerformed(event); return true;});

        logger.info("update action performed. result:"+result);
    }

    private void assertNoErrors() {
        assertNotifications(0);
    }

    private void assertError(List<String> titleParts, List<String> contentParts) {
        assertError(titleParts, Collections.emptyList(), contentParts, Collections.emptyList());
    }

    private void assertError(List<String> titleParts, List<String> notContainedTitleParts,
                             List<String> contentParts, List<String> notContainedContentParts) {
        Notification error = assertNotifications(1);
        assertThat(error).isNotNull();

        assertThat(error.getTitle())
                .as("unexpected error notification title")
                .contains(titleParts)
                .satisfies(err -> notContainedTitleParts.forEach(part -> assertThat(err).doesNotContain(part)));

        assertThat(error.getContent()).as("unexpected error notification content")
                .contains(contentParts)
                .satisfies(err -> notContainedContentParts.forEach(part -> assertThat(err).doesNotContain(part)));
    }

    @Nullable
    private Notification assertNotifications(int expectedErrors) {
        assertThat(capturedInfos)
                .as("expected to capture one informational notification, that update finished")
                .hasSize(1);

        assertThat(capturedErrors)
                .as("invalid number of error notifications captured, we expected to find: "+expectedErrors)
                .hasSize(expectedErrors);

        if(expectedErrors > 0) {
            return capturedErrors.get(0);
        }

        return null;
    }

    private void assertOnMasterBranch() {
        assertThat(repository.getCurrentBranchName())
                .as("we didn't get back to our original branch after updating")
                .isEqualTo("master")
                .isEqualTo(getCurrentBranchFromGit());
    }

    private String getCurrentBranchFromGit() {
        String result = git("status");
        // expected result will be similar to:
        // On branch master
        // Your branch is up-to-date with 'origin/master'.
        // nothing to commit, working tree clean
        result = result.replace("\n", "<br>");
        String[] firstLineWords = result.split("<br>")[0].split(" ");
        String branch = firstLineWords[firstLineWords.length-1];
        logger.info("current branch is:["+branch+"] as reported from (formatted) git status: "+result);
        return branch;
    }
}