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
import com.intellij.testFramework.TestDataProvider;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.branch.GitBranchesCollection;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.configuration.GitExtenderSettingsHandler;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static gr.jchrist.gitextender.GitExecutor.cd;
import static gr.jchrist.gitextender.GitExecutor.checkout;
import static gr.jchrist.gitextender.GitExecutor.git;
import static gr.jchrist.gitextender.GitExecutor.push;
import static gr.jchrist.gitextender.GitExecutor.tac;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ProjectUpdateITest extends AbstractIT {
    public static final String MAIN_BRANCH_NAME = "main";
    private static final Logger logger = Logger.getInstance(ProjectUpdateITest.class);

    private static final String remoteName = "testRemote";
    private static final String remoteAccessName = "testRemoteAccess";
    private String remoteRepoPath;
    private String remoteRepoAccessPath;
    private GitRepository repository;

    private MessageBusConnection mbc;
    private final List<Notification> capturedInfos = new ArrayList<>();
    private final List<Notification> capturedErrors = new ArrayList<>();

    private AnActionEvent event;
    private GitExtenderUpdateAll updater;
    private GitExtenderSettings settings;
    private GitExtenderSettingsHandler appSettingsHandler;

    @Before
    public final void before() throws Exception {
        remoteRepoPath = Files.createTempDirectory(remoteName).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        remoteRepoAccessPath = Files.createTempDirectory(remoteAccessName).toRealPath(LinkOption.NOFOLLOW_LINKS).toString();

        logger.info("creating test remote in directory: " + remoteRepoPath+" with access (non-bare) in:"+remoteRepoAccessPath);

        repository = GitTestUtil.createRemoteRepositoryAndCloneToLocal(super.getProject(), super.getProjectPath(), remoteRepoPath, remoteRepoAccessPath);
        final var grm = super.getGitRepositoryManager();
        runOutOfEdt(() -> {
            repository.update();
            grm.updateAllRepositories();
        });

        logger.info("Starting up with repos: " + grm.getRepositories() +
                " Branch track infos: "+ grm.getRepositories().get(0).getBranchTrackInfos());

        updater = new GitExtenderUpdateAll();
        final var tdp = new TestDataProvider(super.getProject());
        event = AnActionEvent.createFromAnAction(updater, null, "somewhere", tdp::getData);

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
    public final void after() throws Exception {
        if(mbc != null) {
            mbc.disconnect();
        }
        try {
            repository.dispose();
        } catch (Exception e) {
            logger.warn("error disposing git repo");
        }
    }

    @Test
    public void updateNoChanges() throws Exception {
        runUpdate();
        assertNoErrors();
        assertOnMainBranch();
    }

    @Test
    public void updateRemoteOnlyChanges() throws Exception {
        final String newFileName = "test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(newFileName);
        push();

        cd(super.getProjectPath());
        checkout(MAIN_BRANCH_NAME);

        runUpdate();

        assertNoErrors();

        assertOnMainBranch();

        checkout("develop");
        assertThat(Paths.get(super.getProjectPath(), newFileName)).exists();
    }

    @Test
    public void updateLocalRemoteDivergedOnlyFFNotUpdated() throws Exception {
        final String remoteFileName = "test_new_file.txt";
        final String localFileName = "local_test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(remoteFileName);
        push();

        cd(super.getProjectPath());
        checkout("develop");
        tac(localFileName);
        //not pushing this since it would get rejected (remote is 1 commit ahead)
        checkout(MAIN_BRANCH_NAME);

        final var grm = getGitRepositoryManager();
        updateRepos(repository);

        runUpdate();

        assertError(Collections.singletonList("fast-forward only"),
                Arrays.asList("Local branch: develop", "Remote branch: origin/develop"));

        assertOnMainBranch();

        checkout("develop");
        assertThat(Paths.get(super.getProjectPath(), localFileName)).exists();
        assertThat(Paths.get(super.getProjectPath(), remoteFileName)).doesNotExist();
    }

    @Test
    public void updateLocalRemoteDivergedSimpleMergeUpdated() throws Exception {
        final String remoteFileName = "test_new_file.txt";
        final String localFileName = "local_test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(remoteFileName);
        push();

        cd(super.getProjectPath());
        checkout("develop");
        tac(localFileName);
        //not pushing this since it would get rejected (remote is 1 commit ahead)
        checkout(MAIN_BRANCH_NAME);

        updateRepos(repository);

        //enable merge/abort
        settings.setAttemptMergeAbort(true);
        appSettingsHandler.saveSettings(settings);

        runUpdate();

        assertNoErrors();
        //assertError(Collections.singletonList("fast-forward only"),
        //        Arrays.asList("Local branch: develop", "Remote branch: origin/develop"));

        assertOnMainBranch();

        checkout("develop");
        assertThat(Paths.get(super.getProjectPath(), localFileName)).exists();
        assertThat(Paths.get(super.getProjectPath(), remoteFileName)).exists();
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
        cd(super.getProjectPath());
        checkout("develop");
        tac(fileName, localContent);
        //not pushing this since it would get rejected (remote is 1 commit ahead)
        checkout(MAIN_BRANCH_NAME);

        updateRepos(repository);

        //enable merge/abort
        settings.setAttemptMergeAbort(true);
        appSettingsHandler.saveSettings(settings);

        runUpdate();

        assertError(Collections.singletonList("failed"), Collections.singletonList("fast-forward"),
                Arrays.asList("Local branch: develop", "Remote branch: origin/develop", "Merge was aborted"),
                Collections.singletonList("fast-forward"));

        assertOnMainBranch();

        checkout("develop");
        assertThat(Paths.get(super.getProjectPath(), fileName))
                .as("expected to abort merge, so local file should be as it was before merging")
                .exists().hasBinaryContent(localContent.getBytes());
    }

    @Test
    public void updateWithPrunedRemotesLocalDeleted() {
        final String remoteFileName = "test_new_file.txt";
        cd(remoteRepoAccessPath);
        checkout("develop");
        tac(remoteFileName);
        push();

        cd(super.getProjectPath());
        checkout("develop");
        git("pull");

        updateRepos();

        //now delete branch on remote
        cd(remoteRepoAccessPath);
        checkout(MAIN_BRANCH_NAME);
        git("branch -D develop");
        //delete remote branch
        git("push origin --delete develop");

        //and get back to our local
        cd(super.getProjectPath());

        //enable merge/abort
        settings.setAttemptMergeAbort(true);
        settings.setPruneLocals(true);
        appSettingsHandler.saveSettings(settings);

        runUpdate();

        assertNoErrors();

        //this means that the pruned remote branch led us to delete the local one and switch to main
        assertOnMainBranch();
        assertThat(super.getGitRepositoryManager().getRepositories()).hasSize(1);
        GitBranchesCollection gbl = super.getGitRepositoryManager().getRepositories().get(0).getBranches();
        assertThat(gbl.findLocalBranch(MAIN_BRANCH_NAME)).as("no main branch found locally").isNotNull();
        assertThat(gbl.findLocalBranch("develop")).as("develop branch found, while expected to have been auto-deleted").isNull();
    }

    private void runUpdate() {
        // Boolean result = WriteCommandAction.runWriteCommandAction(super.getProject(),
        //         (Computable<Boolean>) ()-> {updater.actionPerformed(event); return true;});
        updater.actionPerformed(event);
        waitForUpdateToFinish();
        logger.info("update action performed");
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

    private void assertOnMainBranch() {
        assertThat(repository.getCurrentBranchName())
                .as("we didn't get back to our original branch after updating")
                .isEqualTo("main")
                .isEqualTo(getCurrentBranchFromGit());
    }

    private String getCurrentBranchFromGit() {
        String result = git("status");
        // expected result will be similar to:
        // On branch main
        // Your branch is up-to-date with 'origin/main'.
        // nothing to commit, working tree clean
        result = result.replace("\n", "<br>");
        String[] firstLineWords = result.split("<br>")[0].split(" ");
        String branch = firstLineWords[firstLineWords.length-1];
        logger.info("current branch is:["+branch+"] as reported from (formatted) git status: "+result);
        return branch;
    }

    private void waitForUpdateToFinish() {
        if (updater != null && updater.updateCountDown != null) {
            try {
                updater.updateCountDown.await(10, TimeUnit.SECONDS);
                int retries = 0;
                while (updater.executingFlag.get() && retries < 100) {
                    TimeUnit.MILLISECONDS.sleep(100);
                    retries++;
                }
            } catch (Exception ex) {
                logger.warn("error waiting for update to finish! it took more than 1m!", ex);
                fail("error waiting for update to finish! it took more than 1m!");
            }
        }
    }
}
