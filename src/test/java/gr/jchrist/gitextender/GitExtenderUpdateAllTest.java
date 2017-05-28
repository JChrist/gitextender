package gr.jchrist.gitextender;

import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.TestDataProvider;
import com.intellij.vcsUtil.VcsImplUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.configuration.GitExtenderSettingsHandler;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import gr.jchrist.gitextender.dialog.SelectModuleDialog;
import mockit.Delegate;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class GitExtenderUpdateAllTest {
    private TestingUtil.BaseTest base;

    @Before
    public void before() throws Exception {
        base = TestingUtil.getBaseTest();
        base.setUp();
    }

    @After
    public void after() throws Exception {
        base.tearDown();
    }

    @Test
    public void getGitRepositoryManager() throws Exception {
        assertThat(GitExtenderUpdateAll.getGitRepositoryManager(base.getProject()))
                .isNotNull();
    }

    @Test
    public void noVcsRepositoryManager(
            final @Mocked Project project
    ) throws Exception {
        new Expectations() {{
            project.getComponent(VcsRepositoryManager.class);
            result = null;
        }};

        assertThat(GitExtenderUpdateAll.getGitRepositoryManager(project)).isNull();
    }

    @Test
    public void errorGettingRepositoryManager(
            final @Mocked Project project
    ) throws Exception {
        new Expectations() {{
            project.getComponent(VcsRepositoryManager.class);
            result = new Exception("test exception");
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
        assertThat(base.getProject())
                .as("null project returned from base")
                .isNotNull();

        final String repoName = base.getProject().getName();

        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(base.getProject()));
        final GitExtenderSettings settings = new GitExtenderSettings();
        new Expectations() {{
            gitRepositoryManager.getRepositories(); result = Collections.singletonList(gitRepository);
            gitRepositoryManager.updateAllRepositories();
            gitRepository.getProject(); result = base.getProject();
            gitRepository.getRoot(); result = root;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root); result = repoName;

            projectSettingsHandler.loadSelectedModules(); result = new ArrayList<>();
            gitExtenderSettingsHandler.loadSettings(); result = settings;

            new BackgroundableRepoUpdateTask(gitRepository, repoName, settings, (AtomicInteger)any, (AccessToken)any);
            result = task;
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            selectModuleDialog.showAndGet(); times = 0;
            task.queue(); times = 1;
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
            final @Mocked BackgroundableRepoUpdateTask task,
            final @Mocked VcsImplUtil vcsImplUtil,
            final @Mocked SelectModuleDialog selectModuleDialog,
            final @Mocked ProjectSettingsHandler projectSettingsHandler,
            final @Mocked GitExtenderSettingsHandler gitExtenderSettingsHandler
    ) throws Exception {
        assertThat(base.getProject())
                .as("null project returned from base")
                .isNotNull();

        final String baseRepoName = base.getProject().getName();
        final String repoName1 = baseRepoName + "_1";
        final String repoName2 = baseRepoName + "_2";
        final String repoName3 = baseRepoName + "_3";

        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(base.getProject()));
        final GitExtenderSettings settings = new GitExtenderSettings();
        new Expectations() {{
            gitRepositoryManager.getRepositories();
            result = Arrays.asList(gitRepository1, gitRepository2, gitRepository3);
            gitRepositoryManager.updateAllRepositories();
            gitRepository1.getProject(); result = base.getProject();
            gitRepository2.getProject(); result = base.getProject();
            gitRepository3.getProject(); result = base.getProject();
            gitRepository1.getRoot(); result = root1;
            gitRepository2.getRoot(); result = root2;
            gitRepository3.getRoot(); result = root3;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root1); result = repoName1;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root2); result = repoName2;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root3); result = repoName3;

            selectModuleDialog.showAndGet(); result = true;
            projectSettingsHandler.loadSelectedModules();
            result = new ArrayList<>(Arrays.asList(repoName1, repoName2, repoName3));

            gitExtenderSettingsHandler.loadSettings(); result = settings;

            new BackgroundableRepoUpdateTask(gitRepository1, repoName1, settings, (AtomicInteger)any, (AccessToken)any);
            result = task;
            new BackgroundableRepoUpdateTask(gitRepository2, repoName2, settings, (AtomicInteger)any, (AccessToken)any);
            result = task;
            new BackgroundableRepoUpdateTask(gitRepository3, repoName3, settings, (AtomicInteger)any, (AccessToken)any);
            result = task;
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            task.queue(); times = 3;
        }};
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
            final @Mocked BackgroundableRepoUpdateTask task1,
            final @Mocked BackgroundableRepoUpdateTask task2,
            final @Mocked VcsImplUtil vcsImplUtil,
            final @Mocked SelectModuleDialog selectModuleDialog,
            final @Mocked ProjectSettingsHandler projectSettingsHandler,
            final @Mocked GitExtenderSettingsHandler gitExtenderSettingsHandler
    ) throws Exception {
        assertThat(base.getProject())
                .as("null project returned from base")
                .isNotNull();

        final String baseRepoName = base.getProject().getName();
        final String repoName1 = baseRepoName + "_1";
        final String repoName2 = baseRepoName + "_2";
        final String repoName3 = baseRepoName + "_3";

        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere",
                new TestDataProvider(base.getProject()));
        final GitExtenderSettings settings = new GitExtenderSettings();
        new Expectations() {{
            gitRepositoryManager.getRepositories();
            result = Arrays.asList(gitRepository1, gitRepository2, gitRepository3);
            gitRepositoryManager.updateAllRepositories();
            gitRepository1.getProject(); result = base.getProject();
            gitRepository2.getProject(); result = base.getProject();
            gitRepository3.getProject(); result = base.getProject();
            gitRepository1.getRoot(); result = root1;
            gitRepository2.getRoot(); result = root2;
            gitRepository3.getRoot(); result = root3;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root1); result = repoName1;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root2); result = repoName2;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root3); result = repoName3;

            selectModuleDialog.showAndGet(); result = true;
            projectSettingsHandler.loadSelectedModules();
            result = new ArrayList<>(Arrays.asList(repoName1, repoName2));

            gitExtenderSettingsHandler.loadSettings(); result = settings;

            new BackgroundableRepoUpdateTask(gitRepository1, repoName1, settings, (AtomicInteger)any, (AccessToken)any);
            result = task1;
            new BackgroundableRepoUpdateTask(gitRepository2, repoName2, settings, (AtomicInteger)any, (AccessToken)any);
            result = task2;
            new BackgroundableRepoUpdateTask(gitRepository3, repoName3, settings, (AtomicInteger)any, (AccessToken)any);
            times = 0;
        }};

        updater.actionPerformed(event);

        new Verifications() {{
            task1.queue();
            task2.queue();
        }};
    }
}