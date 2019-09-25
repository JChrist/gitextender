package gr.jchrist.gitextender;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.TestDataProvider;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.vcsUtil.VcsImplUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.configuration.GitExtenderSettingsHandler;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import gr.jchrist.gitextender.dialog.SelectModuleDialog;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class GitExtenderUpdateAllTest extends BasePlatformTestCase {
    private Project project;

    @Before
    public final void before() throws Exception {
        super.setUp();
        project = super.getProject();
    }

    @After
    public final void after() throws Exception {
        super.tearDown();
    }

    @Test
    public void getGitRepositoryManager() throws Exception {
        assertThat(GitExtenderUpdateAll.getGitRepositoryManager(project))
                .isNotNull();
    }

    @Test
    public void errorGettingRepositoryManager(
            final @Mocked Project project,
            final @Mocked GitRepositoryManager grmanager
    ) throws Exception {
        new Expectations() {{
            GitRepositoryManager.getInstance(project); result = new Exception("test error creating");
        }};

        assertThat(GitExtenderUpdateAll.getGitRepositoryManager(project)).isNull();
    }

    @Test
    public void actionPerformedSingleRepoNoDialog (
            final @Mocked GitRepositoryManager gitRepositoryManager,
            final @Mocked GitRepository gitRepository,
            final @Mocked VirtualFile root,
            final @Mocked BackgroundableRepoUpdateTask task,
            final @Mocked VcsImplUtil vcsImplUtil,
            final @Mocked SelectModuleDialog selectModuleDialog,
            final @Mocked ProjectSettingsHandler projectSettingsHandler,
            final @Mocked GitExtenderSettingsHandler gitExtenderSettingsHandler
    ) throws Exception {
        assertThat(project)
                .as("null project returned from base")
                .isNotNull();

        final String repoName = project.getName();

        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(project));
        final GitExtenderSettings settings = new GitExtenderSettings();
        new Expectations() {{
            gitRepositoryManager.getRepositories(); result = Collections.singletonList(gitRepository);
            gitRepository.getProject(); result = project;
            gitRepository.getRoot(); result = root;
            VcsImplUtil.getShortVcsRootName(project, root); result = repoName;

            projectSettingsHandler.loadSelectedModules(); result = new ArrayList<>();
            gitExtenderSettingsHandler.loadSettings(); result = settings;

            new BackgroundableRepoUpdateTask(gitRepository, repoName, settings, (CountDownLatch)any);
            result = task;
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            selectModuleDialog.showAndGet(); times = 0;
            task.run(); times = 1;
        }};
    }

    @Test
    public void actionPerformedMultipleReposAllSelected (
            final @Mocked GitRepositoryManager gitRepositoryManager,
            final @Mocked GitRepository gitRepository1,
            final @Mocked GitRepository gitRepository2,
            final @Mocked GitRepository gitRepository3,
            final @Mocked VirtualFile root1,
            final @Mocked VirtualFile root2,
            final @Mocked VirtualFile root3,
            final @Mocked VcsImplUtil vcsImplUtil,
            final @Mocked SelectModuleDialog selectModuleDialog,
            final @Mocked ProjectSettingsHandler projectSettingsHandler,
            final @Mocked GitExtenderSettingsHandler gitExtenderSettingsHandler,
            final @Mocked RepositoryUpdater repoUpdater
    ) throws Exception {
        assertThat(project)
                .as("null project returned from base")
                .isNotNull();

        final String baseRepoName = project.getName();
        final String repoName1 = baseRepoName + "_1";
        final String repoName2 = baseRepoName + "_2";
        final String repoName3 = baseRepoName + "_3";

        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(project));
        final GitExtenderSettings settings = new GitExtenderSettings();
        new Expectations() {{
            gitRepositoryManager.getRepositories();
            result = Arrays.asList(gitRepository1, gitRepository2, gitRepository3);
            gitRepository1.getProject(); result = project;
            gitRepository2.getProject(); result = project;
            gitRepository3.getProject(); result = project;
            gitRepository1.getRoot(); result = root1;
            gitRepository2.getRoot(); result = root2;
            gitRepository3.getRoot(); result = root3;
            VcsImplUtil.getShortVcsRootName(project, root1); result = repoName1;
            VcsImplUtil.getShortVcsRootName(project, root2); result = repoName2;
            VcsImplUtil.getShortVcsRootName(project, root3); result = repoName3;

            selectModuleDialog.showAndGet(); result = true;
            projectSettingsHandler.loadSelectedModules();
            result = new ArrayList<>(Arrays.asList(repoName1, repoName2, repoName3));

            gitExtenderSettingsHandler.loadSettings(); result = settings;

            new RepositoryUpdater(gitRepository1, repoName1, settings); result = repoUpdater;
            new RepositoryUpdater(gitRepository2, repoName2, settings); result = repoUpdater;
            new RepositoryUpdater(gitRepository3, repoName3, settings); result = repoUpdater;
        }};

        updater.actionPerformed(event);
        assertThat(updater.updateCountDown).isNotNull();
        assertThat(updater.updateCountDown.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void actionPerformedMultipleReposPartSelected (
            final @Mocked GitRepositoryManager gitRepositoryManager,
            final @Mocked GitRepository gitRepository1,
            final @Mocked GitRepository gitRepository2,
            final @Mocked GitRepository gitRepository3,
            final @Mocked VirtualFile root1,
            final @Mocked VirtualFile root2,
            final @Mocked VirtualFile root3,
            final @Mocked VcsImplUtil vcsImplUtil,
            final @Mocked SelectModuleDialog selectModuleDialog,
            final @Mocked ProjectSettingsHandler projectSettingsHandler,
            final @Mocked GitExtenderSettingsHandler gitExtenderSettingsHandler,
            final @Mocked RepositoryUpdater repoUpdater
    ) throws Exception {
        assertThat(project)
                .as("null project returned from base")
                .isNotNull();

        final String baseRepoName = project.getName();
        final String repoName1 = baseRepoName + "_1";
        final String repoName2 = baseRepoName + "_2";
        final String repoName3 = baseRepoName + "_3";

        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(project));
        final GitExtenderSettings settings = new GitExtenderSettings();
        new Expectations() {{
            gitRepositoryManager.getRepositories();
            result = Arrays.asList(gitRepository1, gitRepository2, gitRepository3);
            gitRepository1.getProject(); result = project;
            gitRepository2.getProject(); result = project;
            gitRepository3.getProject(); result = project;
            gitRepository1.getRoot(); result = root1;
            gitRepository2.getRoot(); result = root2;
            gitRepository3.getRoot(); result = root3;
            VcsImplUtil.getShortVcsRootName(project, root1); result = repoName1;
            VcsImplUtil.getShortVcsRootName(project, root2); result = repoName2;
            VcsImplUtil.getShortVcsRootName(project, root3); result = repoName3;

            selectModuleDialog.showAndGet(); result = true;
            projectSettingsHandler.loadSelectedModules();
            result = new ArrayList<>(Arrays.asList(repoName1, repoName2));

            gitExtenderSettingsHandler.loadSettings(); result = settings;

            new RepositoryUpdater(gitRepository1, repoName1, settings); result = repoUpdater;
            new RepositoryUpdater(gitRepository2, repoName2, settings); result = repoUpdater;
            new RepositoryUpdater(gitRepository3, repoName3, settings); times = 0;
        }};

        updater.actionPerformed(event);
        assertThat(updater.updateCountDown).isNotNull();
        assertThat(updater.updateCountDown.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void actionPerformedNoProject (
            @Mocked final AnActionEvent event,
            @Mocked final NotificationUtil notificationUtil
    ) throws Exception {
        new Expectations() {{
            event.getProject(); result = null;
        }};

        GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        updater.actionPerformed(event);

        new Verifications() {{
            NotificationUtil.showErrorNotification("Update Failed", anyString);
        }};
    }

    @Test
    public void actionPerformedError (
            @Mocked final AnActionEvent event,
            @Mocked final NotificationUtil notificationUtil
    ) throws Exception {
        new Expectations() {{
            event.getProject(); result = new Exception("test exception during action performed");
        }};

        GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        updater.actionPerformed(event);

        new Verifications() {{
            NotificationUtil.showErrorNotification("Update Failed", anyString);
        }};
    }

    @Test
    public void actionPerformedNoRepositories (
            @Mocked final AnActionEvent event,
            @Mocked final NotificationUtil notificationUtil,
            @Mocked final Project project,
            @Mocked final GitRepositoryManager manager
    ) throws Exception {
        GitExtenderUpdateAll updater = new GitExtenderUpdateAll();

        new Expectations() {{
            event.getProject(); result = project;
            manager.getRepositories(); result = Collections.emptyList();
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            NotificationUtil.showErrorNotification("Update Failed", anyString);
        }};
    }

    @Test
    public void actionPerformedCanceledFromDialog (
            @Mocked final AnActionEvent event,
            @Mocked final NotificationUtil notificationUtil,
            @Mocked final Project project,
            @Mocked final GitRepositoryManager manager,
            @Mocked final GitRepository gitRepository,
            @Mocked final ProjectSettingsHandler settingsHandler,
            @Mocked final VcsImplUtil vcsImplUtil,
            @Mocked final SelectModuleDialog dialog
    ) throws Exception {
        GitExtenderUpdateAll updater = new GitExtenderUpdateAll();

        new Expectations() {{
            event.getProject(); result = project;
            manager.getRepositories(); result = Arrays.asList(gitRepository, gitRepository);
            VcsImplUtil.getShortVcsRootName(project, (VirtualFile) any); result = "test";
            dialog.showAndGet(); result = false;
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            NotificationUtil.showInfoNotification("Update Canceled", anyString);
        }};
    }

    @Test
    public void actionPerformedNothingSelectedFromDialog (
            @Mocked final AnActionEvent event,
            @Mocked final NotificationUtil notificationUtil,
            @Mocked final Project project,
            @Mocked final GitRepositoryManager manager,
            @Mocked final GitRepository gitRepository,
            @Mocked final ProjectSettingsHandler settingsHandler,
            @Mocked final VcsImplUtil vcsImplUtil,
            @Mocked final SelectModuleDialog dialog
    ) throws Exception {
        GitExtenderUpdateAll updater = new GitExtenderUpdateAll();

        new Expectations() {{
            event.getProject(); result = project;
            manager.getRepositories(); result = Arrays.asList(gitRepository, gitRepository);
            VcsImplUtil.getShortVcsRootName(project, (VirtualFile) any); result = "test";
            dialog.showAndGet(); result = true;
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            NotificationUtil.showInfoNotification("Update Canceled", anyString);
        }};
    }
}
