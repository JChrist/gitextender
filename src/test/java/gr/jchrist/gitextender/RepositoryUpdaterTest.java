package gr.jchrist.gitextender;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.commands.Git;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRepository;
import git4idea.update.GitFetcher;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.handlers.CheckoutHandler;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import static gr.jchrist.gitextender.TestingUtil.error;
import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class RepositoryUpdaterTest {
    private final String repoName = "testRepo";
    private final String initialBranch = "testInitialBranch";
    @Mocked GitRepository repo;
    @Mocked ProgressIndicator indicator;
    @Mocked NotificationUtil notificationUtil;
    @Mocked GitUtil gitUtil;
    @Mocked Git git;
    @Mocked ServiceManager serviceManager;
    @Mocked Project project;
    @Mocked VirtualFile root;
    @Mocked GitFetcher fetcher;
    @Mocked GitBranchTrackInfo branchTrackInfo;
    @Mocked BranchUpdater branchUpdater;
    @Mocked CheckoutHandler checkoutHandler;

    private GitExtenderSettings settings;
    private RepositoryUpdater repositoryUpdater;

    @Before
    public void before() throws Exception {
        settings = new GitExtenderSettings();
        repositoryUpdater = new RepositoryUpdater(repo, indicator, repoName, settings);
    }

    @Test
    public void updateRepository() throws Exception {
        new Expectations() {{
            repo.isRebaseInProgress(); result = false;
            repo.getRemotes(); result = Collections.singletonList(null);
            repo.getCurrentBranchName(); result = initialBranch;
            repo.getProject(); result = project;
            repo.getRoot(); result = root;

            ServiceManager.getService(project, Git.class); result = git;

            GitUtil.hasLocalChanges(anyBoolean, project, root); result = true;

            git.stashSave(repo, anyString); result = success;
            fetcher.fetchRootsAndNotify((Collection<GitRepository>) any, null, true);
            result = new Delegate<Boolean>() {
                public boolean fetchRootsAndNotify(Collection<GitRepository> repos, String title, boolean notify) {
                    assertThat(repos).hasSize(1).containsExactly(repo);
                    return true;
                }
            };

            repo.update();
            repo.getBranchTrackInfos(); result = Collections.singletonList(branchTrackInfo);

            new BranchUpdater(git, repo, repoName, branchTrackInfo, settings); result = branchUpdater;
            branchUpdater.update(); result = new BranchUpdateResult(success, success, null, null);

            new CheckoutHandler(git, project, root, initialBranch); result = checkoutHandler;
            checkoutHandler.checkout();
            repo.update();
            git.stashPop(repo);
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, anyString);
            times = 0;
        }};
    }

    @Test
    public void repoInRebase() throws Exception {
        new Expectations() {{
            repo.isRebaseInProgress(); result = true;
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, RepositoryUpdater.getMessage(RepositoryUpdater.REBASE_IN_PROGRESS, repoName));
        }};
    }

    @Test
    public void noRemotes() throws Exception {
        new Expectations() {{
            repo.getRemotes(); result = Collections.emptyList();
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, RepositoryUpdater.getMessage(RepositoryUpdater.NO_REMOTES, repoName));
        }};
    }

    @Test
    public void noCurrentBranch() throws Exception {
        new Expectations() {{
            repo.getRemotes().isEmpty(); result = false;
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, RepositoryUpdater.getMessage(RepositoryUpdater.NO_CURRENT_BRANCH, repoName));
        }};
    }

    @Test
    public void errorDecidingWhetherToStash() throws Exception {
        new Expectations() {{
            repo.getRemotes().isEmpty(); result = false;
            repo.getCurrentBranchName(); result = "test";
            GitUtil.hasLocalChanges(anyBoolean, repo.getProject(), repo.getRoot());
            result = new Exception("test exception while checking whether there are changed files");
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, withPrefix(RepositoryUpdater.getMessage(RepositoryUpdater.FAILED_HAS_CHANGES, repoName)));
        }};
    }

    @Test
    public void errorStashing() throws Exception {
        new Expectations() {{
            repo.getRemotes().isEmpty(); result = false;
            repo.getCurrentBranchName(); result = "test";
            GitUtil.hasLocalChanges(anyBoolean, repo.getProject(), repo.getRoot()); result = true;
            git.stashSave(repo, anyString); result = error;
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, withPrefix(RepositoryUpdater.getMessage(RepositoryUpdater.NO_STASH, repoName)));
        }};
    }

    @Test
    public void fetchError() throws Exception {
        new Expectations() {{
            repo.getRemotes().isEmpty(); result = false;
            repo.getCurrentBranchName(); result = initialBranch;
            fetcher.fetchRootsAndNotify((Collection<GitRepository>) any, null, true); result = false;
        }};

        repositoryUpdater.updateRepository();

        new Verifications() {{
            repo.update(); times = 0;
            NotificationUtil.showErrorNotification(anyString, anyString); times = 0;
        }};
    }

    @Test
    public void checkoutError() throws Exception {
        final String local = "local";
        new Expectations() {{
            repo.isRebaseInProgress(); result = false;
            repo.getRemotes(); result = Collections.singletonList(null);
            repo.getCurrentBranchName(); result = initialBranch;
            repo.getProject(); result = project;
            repo.getRoot(); result = root;

            ServiceManager.getService(project, Git.class); result = git;

            GitUtil.hasLocalChanges(anyBoolean, project, root); result = true;

            git.stashSave(repo, anyString); result = success;
            fetcher.fetchRootsAndNotify((Collection<GitRepository>) any, null, true);
            result = new Delegate<Boolean>() {
                public boolean fetchRootsAndNotify(Collection<GitRepository> repos, String title, boolean notify) {
                    assertThat(repos).hasSize(1).containsExactly(repo);
                    return true;
                }
            };

            repo.update();
            repo.getBranchTrackInfos(); result = Collections.singletonList(branchTrackInfo);

            new BranchUpdater(git, repo, repoName, branchTrackInfo, settings); result = branchUpdater;
            branchUpdater.update(); result = new BranchUpdateResult(error, null, null, null);
            branchUpdater.getLocalBranchName(); result = local;
            branchUpdater.getRemoteBranchName(); times = 0;


            new CheckoutHandler(git, project, root, initialBranch); result = checkoutHandler;
            checkoutHandler.checkout();
            repo.update();
            git.stashPop(repo);
        }};

        repositoryUpdater.updateRepository();
        List<String> errors = new ArrayList<>();
        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, withCapture(errors));
        }};

        assertThat(errors).as("expected to capture one error content").hasSize(1);
        String error = errors.get(0);
        assertThat(error).as("unexpected error notification content")
                .contains(repoName, local, "Checkout Error:");
    }

    @Test
    public void ffMergeError() throws Exception {
        final String local = "local";
        final String remote = "remote";

        settings.attemptMergeAbort = false;

        new Expectations() {{
            repo.isRebaseInProgress(); result = false;
            repo.getRemotes(); result = Collections.singletonList(null);
            repo.getCurrentBranchName(); result = initialBranch;
            repo.getProject(); result = project;
            repo.getRoot(); result = root;

            ServiceManager.getService(project, Git.class); result = git;

            GitUtil.hasLocalChanges(anyBoolean, project, root); result = true;

            git.stashSave(repo, anyString); result = success;
            fetcher.fetchRootsAndNotify((Collection<GitRepository>) any, null, true);
            result = new Delegate<Boolean>() {
                public boolean fetchRootsAndNotify(Collection<GitRepository> repos, String title, boolean notify) {
                    assertThat(repos).hasSize(1).containsExactly(repo);
                    return true;
                }
            };

            repo.update();
            repo.getBranchTrackInfos(); result = Collections.singletonList(branchTrackInfo);

            new BranchUpdater(git, repo, repoName, branchTrackInfo, settings); result = branchUpdater;
            branchUpdater.update(); result = new BranchUpdateResult(success, error, null, null);
            branchUpdater.getLocalBranchName(); result = local;
            branchUpdater.getRemoteBranchName(); result = remote;

            new CheckoutHandler(git, project, root, initialBranch); result = checkoutHandler;
            checkoutHandler.checkout();
            repo.update();
            git.stashPop(repo);
        }};

        repositoryUpdater.updateRepository();
        List<String> errors = new ArrayList<>();
        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, withCapture(errors));
        }};

        assertThat(errors).as("expected to capture one error content").hasSize(1);
        String error = errors.get(0);
        assertThat(error).as("unexpected error notification content")
                .contains(repoName, local, remote, "Merge error:");
    }

    @Test
    public void MergeErrorAborted() throws Exception {
        final String local = "local";
        final String remote = "remote";
        settings.attemptMergeAbort = true;

        new Expectations() {{
            repo.isRebaseInProgress(); result = false;
            repo.getRemotes(); result = Collections.singletonList(null);
            repo.getCurrentBranchName(); result = initialBranch;
            repo.getProject(); result = project;
            repo.getRoot(); result = root;

            ServiceManager.getService(project, Git.class); result = git;

            GitUtil.hasLocalChanges(anyBoolean, project, root); result = true;

            git.stashSave(repo, anyString); result = success;
            fetcher.fetchRootsAndNotify((Collection<GitRepository>) any, null, true);
            result = new Delegate<Boolean>() {
                public boolean fetchRootsAndNotify(Collection<GitRepository> repos, String title, boolean notify) {
                    assertThat(repos).hasSize(1).containsExactly(repo);
                    return true;
                }
            };

            repo.update();
            repo.getBranchTrackInfos(); result = Collections.singletonList(branchTrackInfo);

            new BranchUpdater(git, repo, repoName, branchTrackInfo, settings); result = branchUpdater;
            branchUpdater.update(); result = new BranchUpdateResult(success, error, error, success);
            branchUpdater.getLocalBranchName(); result = local;
            branchUpdater.getRemoteBranchName(); result = remote;

            new CheckoutHandler(git, project, root, initialBranch); result = checkoutHandler;
            checkoutHandler.checkout();
            repo.update();
            git.stashPop(repo);
        }};

        repositoryUpdater.updateRepository();

        List<String> errors = new ArrayList<>();
        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, withCapture(errors));
        }};

        assertThat(errors).as("expected to capture one error content").hasSize(1);
        String error = errors.get(0);
        assertThat(error).as("unexpected error notification content")
                .contains(repoName, local, remote, "Merge error:", "was aborted");
    }

    @Test
    public void MergeErrorNotAborted() throws Exception {
        final String local = "local";
        final String remote = "remote";
        settings.attemptMergeAbort = true;

        new Expectations() {{
            repo.isRebaseInProgress(); result = false;
            repo.getRemotes(); result = Collections.singletonList(null);
            repo.getCurrentBranchName(); result = initialBranch;
            repo.getProject(); result = project;
            repo.getRoot(); result = root;

            ServiceManager.getService(project, Git.class); result = git;

            GitUtil.hasLocalChanges(anyBoolean, project, root); result = true;

            git.stashSave(repo, anyString); result = success;
            fetcher.fetchRootsAndNotify((Collection<GitRepository>) any, null, true);
            result = new Delegate<Boolean>() {
                public boolean fetchRootsAndNotify(Collection<GitRepository> repos, String title, boolean notify) {
                    assertThat(repos).hasSize(1).containsExactly(repo);
                    return true;
                }
            };

            repo.update(); times = 1;
            repo.getBranchTrackInfos(); result = Arrays.asList(branchTrackInfo, branchTrackInfo);

            new BranchUpdater(git, repo, repoName, branchTrackInfo, settings); result = branchUpdater;
            branchUpdater.update(); result = new BranchUpdateResult(success, error, error, error); times = 1;
            branchUpdater.getLocalBranchName(); result = local;
            branchUpdater.getRemoteBranchName(); result = remote;

            new CheckoutHandler(git, project, root, initialBranch);
            result = checkoutHandler; minTimes = 0;
            checkoutHandler.checkout(); times = 0;
            git.stashPop(repo); times = 0;
        }};

        repositoryUpdater.updateRepository();

        List<String> errors = new ArrayList<>();
        new Verifications() {{
            NotificationUtil.showErrorNotification(anyString, withCapture(errors));
        }};

        assertThat(errors).as("expected to capture one error content").hasSize(1);
        String error = errors.get(0);
        assertThat(error).as("unexpected error notification content")
                .contains(repoName, local, remote, "Merge error:", "NOT aborted");
    }
}