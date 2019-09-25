package gr.jchrist.gitextender;

import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class BackgroundableRepoUpdateTaskTest {
    @Test
    public void run(
            final @Mocked GitRepository repo,
            final @Mocked RepositoryUpdater updater
    ) throws Exception {
        final String repoName = "test repo";
        final GitExtenderSettings settings = new GitExtenderSettings(true, true);
        final CountDownLatch countDown = new CountDownLatch(1);
        BackgroundableRepoUpdateTask task = new BackgroundableRepoUpdateTask(repo, repoName, settings, countDown);

        task.run();

        assertThat(countDown.getCount()).isZero();

        new Verifications() {{
            updater.updateRepository();
        }};
    }

    @Test
    public void runButOthersRemain(
            final @Mocked GitRepository repo,
            final @Mocked RepositoryUpdater updater
    ) throws Exception {
        final String repoName = "test repo";
        final GitExtenderSettings settings = new GitExtenderSettings(true, true);
        final CountDownLatch countDown = new CountDownLatch(2);

        BackgroundableRepoUpdateTask task = new BackgroundableRepoUpdateTask(repo, repoName, settings, countDown);

        task.run();

        assertThat(countDown.getCount()).isPositive();

        new Verifications() {{
            updater.updateRepository();
        }};
    }
}
