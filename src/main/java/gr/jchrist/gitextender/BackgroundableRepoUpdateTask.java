package gr.jchrist.gitextender;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class BackgroundableRepoUpdateTask extends Task.Backgroundable {
    private final AtomicInteger countDown;
    private final AccessToken accessToken;
    private final GitRepository repo;
    private final String repoName;
    private final GitExtenderSettings settings;

    public BackgroundableRepoUpdateTask(
            @NotNull GitRepository repo,
            @NotNull String repoName,
            @NotNull GitExtenderSettings settings,
            @NotNull AtomicInteger countDown,
            @NotNull AccessToken accessToken
    ) {
        super(repo.getProject(), "Updating: " + repoName, false);
        this.countDown = countDown;
        this.accessToken = accessToken;
        this.repo = repo;
        this.repoName = repoName;
        this.settings = settings;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        RepositoryUpdater repositoryUpdater = new RepositoryUpdater(repo, indicator, repoName, settings);
        try {
            repositoryUpdater.updateRepository();
        } finally {
            if (countDown.decrementAndGet() <= 0) {
                //the last task finished should clean up, release project changes and show info notification
                allUpdatesFinished();
            }
        }
    }

    protected void allUpdatesFinished() {
        DvcsUtil.workingTreeChangeFinished(repo.getProject(), accessToken);
        NotificationUtil.showInfoNotification("Update Completed", "Git Extender updated all projects");
    }
}
