package gr.jchrist.gitextender;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JMockit.class)
public class BackgroundableRepoUpdateTaskTest {
    @Test
    public void run(
            final @Mocked GitRepository repo,
            final @Mocked AccessToken token,
            final @Mocked Project project,
            final @Mocked ProgressIndicator indicator,
            final @Mocked RepositoryUpdater updater,
            final @Mocked NotificationUtil notificationUtil
    ) throws Exception {
        final String repoName = "test repo";
        final GitExtenderSettings settings = new GitExtenderSettings(true);
        final AtomicInteger countDown = new AtomicInteger(1);
        new Expectations() {{
            repo.getProject(); result = project;
        }};
        BackgroundableRepoUpdateTask task = new BackgroundableRepoUpdateTask(repo, repoName, settings, countDown, token);

        task.run(indicator);

        new Verifications() {{
            updater.updateRepository();
            token.finish();
            NotificationUtil.showInfoNotification("Update Completed", "Git Extender updated all projects");
        }};
    }

    @Test
    public void runButOthersRemain(
            final @Mocked GitRepository repo,
            final @Mocked AccessToken token,
            final @Mocked Project project,
            final @Mocked ProgressIndicator indicator,
            final @Mocked RepositoryUpdater updater,
            final @Mocked NotificationUtil notificationUtil
    ) throws Exception {
        final String repoName = "test repo";
        final GitExtenderSettings settings = new GitExtenderSettings(true);
        final AtomicInteger countDown = new AtomicInteger(2);
        new Expectations() {{
            repo.getProject(); result = project;
        }};

        BackgroundableRepoUpdateTask task = new BackgroundableRepoUpdateTask(repo, repoName, settings, countDown, token);

        task.run(indicator);

        new Verifications() {{
            updater.updateRepository();

            token.finish(); times = 0;
            NotificationUtil.showInfoNotification("Update Completed", "Git Extender updated all projects"); times = 0;
        }};
    }
}