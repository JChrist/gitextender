package gr.jchrist.gitextender;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitLocalBranch;
import git4idea.GitStandardRemoteBranch;
import git4idea.commands.Git;
import git4idea.repo.GitBranchTrackInfo;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import gr.jchrist.gitextender.configuration.GitExtenderSettings;
import gr.jchrist.gitextender.handlers.*;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static gr.jchrist.gitextender.TestingUtil.error;
import static gr.jchrist.gitextender.TestingUtil.success;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class BranchUpdaterTest {
    private final String repoName = "testGitRepo";
    private final String localBranchName = "testLocal";
    private final String remoteBranchName = "testRemote";
    private @Mocked Git git;
    private @Mocked GitRepository gitRepository;
    private GitExtenderSettings gitExtenderSettings;
    private GitBranchTrackInfo gitBranchTrackInfo;
    private BranchUpdater branchUpdater;

    @Before
    public void setUp() throws Exception {
        gitBranchTrackInfo = new GitBranchTrackInfo(
                new GitLocalBranch(localBranchName),
                new GitStandardRemoteBranch(
                        new GitRemote(repoName, Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList(), Collections.emptyList()), remoteBranchName),
                false);
        gitExtenderSettings = new GitExtenderSettings();
        branchUpdater = new BranchUpdater(git, gitRepository, repoName, gitBranchTrackInfo, gitExtenderSettings);
    }

    @Test
    public void testUpdateFFMerge(
            final @Mocked Project project,
            final @Mocked VirtualFile virtualFile,
            final @Mocked CheckoutHandler checkoutHandler,
            final @Mocked BeforeMergeHandler beforeMergeHandler,
            final @Mocked AfterMergeHandler afterMergeHandler,
            final @Mocked FastForwardOnlyMerger ffMerger
    ) throws Exception {
        new Expectations() {{
            gitRepository.getProject(); result = project;
            gitRepository.getRoot(); result = virtualFile;
            checkoutHandler.checkout(localBranchName); result = success;
            beforeMergeHandler.beforeMerge();
            ffMerger.mergeFastForward(); result = success;
            afterMergeHandler.afterMerge();
        }};

        BranchUpdateResult bur = branchUpdater.update();

        BranchUpdateResult expected = new BranchUpdateResult(success, success, null, null);
        assertThat(bur).as("unexpected branch update result")
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testUpdateSimpleMerge(
            final @Mocked Project project,
            final @Mocked VirtualFile virtualFile,
            final @Mocked CheckoutHandler checkoutHandler,
            final @Mocked BeforeMergeHandler beforeMergeHandler,
            final @Mocked AfterMergeHandler afterMergeHandler,
            final @Mocked FastForwardOnlyMerger ffMerger,
            final @Mocked SimpleMerger simpleMerger
    ) throws Exception {
        gitExtenderSettings.setAttemptMergeAbort(true);

        new Expectations() {{
            gitRepository.getProject(); result = project;
            gitRepository.getRoot(); result = virtualFile;
            checkoutHandler.checkout(localBranchName); result = success;

            beforeMergeHandler.beforeMerge();

            ffMerger.mergeFastForward(); result = error;

            simpleMerger.mergeAbortIfFailed();
            simpleMerger.getMergeResult(); result = success;
            simpleMerger.getAbortResult(); result = null;

            afterMergeHandler.afterMerge();
        }};

        BranchUpdateResult bur = branchUpdater.update();

        BranchUpdateResult expected = new BranchUpdateResult(success, error, success, null);
        assertThat(bur).as("unexpected branch update result")
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testUpdateCheckoutFailure(
            final @Mocked Project project,
            final @Mocked VirtualFile virtualFile,
            final @Mocked CheckoutHandler checkoutHandler
    ) throws Exception {

        new Expectations() {{
            gitRepository.getProject(); result = project;
            gitRepository.getRoot(); result = virtualFile;
            checkoutHandler.checkout(localBranchName); result = error;
        }};

        BranchUpdateResult bur = branchUpdater.update();

        BranchUpdateResult expected = new BranchUpdateResult(error, null, null, null);
        assertThat(bur).as("unexpected branch update result")
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testUpdateSimpleMergeFailedAborted(
            final @Mocked Project project,
            final @Mocked VirtualFile virtualFile,
            final @Mocked CheckoutHandler checkoutHandler,
            final @Mocked BeforeMergeHandler beforeMergeHandler,
            final @Mocked AfterMergeHandler afterMergeHandler,
            final @Mocked FastForwardOnlyMerger ffMerger,
            final @Mocked SimpleMerger simpleMerger
    ) throws Exception {
        gitExtenderSettings.setAttemptMergeAbort(true);

        new Expectations() {{
            gitRepository.getProject(); result = project;
            gitRepository.getRoot(); result = virtualFile;
            checkoutHandler.checkout(localBranchName); result = success;

            beforeMergeHandler.beforeMerge();

            ffMerger.mergeFastForward(); result = error;

            simpleMerger.mergeAbortIfFailed();
            simpleMerger.getMergeResult(); result = error;
            simpleMerger.getAbortResult(); result = success;

            afterMergeHandler.afterMerge(); times = 0;
        }};

        BranchUpdateResult bur = branchUpdater.update();

        BranchUpdateResult expected = new BranchUpdateResult(success, error, error, success);
        assertThat(bur).as("unexpected branch update result")
                .isEqualToComparingFieldByField(expected);

        assertThat(branchUpdater.getBranchUpdateResult()).isEqualTo(bur);
    }

    @Test
    public void testUpdateFFMergeFailedSimpleNotAttempted(
            final @Mocked Project project,
            final @Mocked VirtualFile virtualFile,
            final @Mocked CheckoutHandler checkoutHandler,
            final @Mocked BeforeMergeHandler beforeMergeHandler,
            final @Mocked AfterMergeHandler afterMergeHandler,
            final @Mocked FastForwardOnlyMerger ffMerger,
            final @Mocked SimpleMerger simpleMerger
    ) throws Exception {
        gitExtenderSettings.setAttemptMergeAbort(false);

        new Expectations() {{
            gitRepository.getProject(); result = project;
            gitRepository.getRoot(); result = virtualFile;
            checkoutHandler.checkout(localBranchName); result = success;

            beforeMergeHandler.beforeMerge();

            ffMerger.mergeFastForward(); result = error;

            simpleMerger.mergeAbortIfFailed(); times = 0;

            afterMergeHandler.afterMerge(); times = 0;
        }};

        BranchUpdateResult bur = branchUpdater.update();

        BranchUpdateResult expected = new BranchUpdateResult(success, error, null, null);
        assertThat(bur).as("unexpected branch update result")
                .isEqualToComparingFieldByField(expected);
    }

    @Test
    public void testLocalBranchName() throws Exception {
        assertThat(branchUpdater.getLocalBranchName())
                .as("unexpected local branch name")
                .isEqualTo(gitBranchTrackInfo.getLocalBranch().getName());
    }

    @Test
    public void testRemoteBranchName() throws Exception {
        assertThat(branchUpdater.getRemoteBranchName())
                .as("unexpected remote branch name")
                .isEqualTo(gitBranchTrackInfo.getRemoteBranch().getNameForLocalOperations());
    }
}