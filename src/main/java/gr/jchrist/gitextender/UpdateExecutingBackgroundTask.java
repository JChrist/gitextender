package gr.jchrist.gitextender;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateExecutingBackgroundTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(UpdateExecutingBackgroundTask.class);
    protected AccessToken accessToken;
    protected CountDownLatch updateCountDownLatch;
    protected ExecutorService executor;
    protected AtomicBoolean executingFlag;

    public UpdateExecutingBackgroundTask(
            Project project, String title,
            AccessToken accessToken, CountDownLatch updatingLatch, ExecutorService executor, AtomicBoolean executing) {
        super(project, title, false, PerformInBackgroundOption.ALWAYS_BACKGROUND);
        this.accessToken = accessToken;
        this.updateCountDownLatch = updatingLatch;
        this.executor = executor;
        this.executingFlag = executing;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            logger.debug("waiting for update to finish - " + Thread.currentThread().getName());
            //10 minutes should be extreme enough to account for ALL updates
            updateCountDownLatch.await(10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("error awaiting update latch!", e);
        }
        try {
            executor.shutdownNow();
        } catch (Exception e) {
            logger.warn("error shutting down executor", e);
        }
        logger.debug("update finished - " + Thread.currentThread().getName());
        //the last task finished should clean up, release project changes and show info notification
        accessToken.finish();
        NotificationUtil.showInfoNotification("Update Completed", "Git Extender updated all projects");
        executingFlag.set(false);
    }
}
