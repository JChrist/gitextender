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
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
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
        /*new Expectations() {{
            project.getComponent(VcsRepositoryManager.class); result = vcsRepositoryManager;
            new GitRepositoryManager(project, vcsRepositoryManager); result = gitRepositoryManager;
        }};*/

        assertThat(GitExtenderUpdateAll.getGitRepositoryManager(base.getProject())).isNotNull();
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
    public void actionPerformed(
            final @Mocked GitRepositoryManager gitRepositoryManager,
            final @Mocked GitRepository gitRepository,
            final @Mocked VirtualFile root,
            final @Mocked VcsImplUtil vcsImplUtil,
            final @Mocked BackgroundableRepoUpdateTask task
    ) throws Exception {
        assertThat(base.getProject())
                .as("null project returned from base")
                .isNotNull();

        final String repoName = base.getProject().getName();
        final GitExtenderUpdateAll updater = new GitExtenderUpdateAll();
        final AnActionEvent event = AnActionEvent.createFromAnAction(updater, null, "somewhere", new TestDataProvider(base.getProject()));
        final GitExtenderSettings settings = GitExtenderSettings.getInstance();
        new Expectations() {{
            gitRepositoryManager.getRepositories();
            result = Arrays.asList(gitRepository, gitRepository, gitRepository);
            gitRepositoryManager.updateAllRepositories();
            gitRepository.getProject();
            result = base.getProject();
            gitRepository.getRoot();
            result = root;
            VcsImplUtil.getShortVcsRootName(base.getProject(), root);
            result = repoName;
            new BackgroundableRepoUpdateTask(gitRepository, repoName, settings, (AtomicInteger) any, (AccessToken) any);
            result = task;
        }};
        updater.actionPerformed(event);

        new Verifications() {{
            task.queue();
        }};
    }
}