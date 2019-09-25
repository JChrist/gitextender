package gr.jchrist.gitextender;

import com.intellij.openapi.application.AccessToken;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

public class BackgroundableRepoUpdateTask implements Runnable {
    private final CountDownLatch countDownLatch;
    private final GitRepository repo;
    private final String repoName;
    private final GitExtenderSettings settings;

    public BackgroundableRepoUpdateTask(
            @NotNull GitRepository repo,
            @NotNull String repoName,
            @NotNull GitExtenderSettings settings,
            @NotNull CountDownLatch countDownLatch
    ) {
        this.countDownLatch = countDownLatch;
        this.repo = repo;
        this.repoName = repoName;
        this.settings = settings;
    }

    @Override
    public void run() {
        RepositoryUpdater repositoryUpdater = new RepositoryUpdater(repo, repoName, settings);
        try {
            repositoryUpdater.updateRepository();
        } finally {
            countDownLatch.countDown();
        }
    }
}
