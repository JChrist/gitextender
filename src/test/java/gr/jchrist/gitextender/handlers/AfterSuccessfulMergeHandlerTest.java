package gr.jchrist.gitextender.handlers;

import com.intellij.history.LocalHistory;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsAnnotationRefresher;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesCache;
import com.intellij.openapi.vcs.update.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ContentUtilEx;
import git4idea.merge.MergeChangeCollector;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JMockit.class)
public class AfterSuccessfulMergeHandlerTest {
    @Mocked MergeState mergeState;

    private AfterSuccessfulMergeHandler handler;

    @Before
    public void before() throws Exception {
        handler = new AfterSuccessfulMergeHandler(mergeState);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void afterMerge(
            final @Mocked MergeChangeCollector collector,
            final @Mocked RefreshVFsSynchronously refreshVFsSynchronously,
            final @Mocked VcsAnnotationRefresher refresher,
            final @Mocked UpdateFilesHelper updateFilesHelper,
            final @Mocked UpdatedFilesNotifier updatedFilesNotifier,
            final @Mocked LocalHistory localHistory,
            final @Mocked ToolWindow toolWindow,
            final @Mocked ToolWindowManager toolWindowManager,
            final @Mocked RestoreUpdateTree restoreUpdateTree,
            final @Mocked UpdateInfoTree updateInfoTree,
            final @Mocked Application application,
            final @Mocked ApplicationManager applicationManager,
            final @Mocked CommittedChangesCache committedChangesCache,
            final @Mocked ContentUtilEx contentUtilEx
    ) throws Exception {
        final UpdatedFiles updatedFiles = UpdatedFiles.create();
        //add a file, so that it's not considered empty
        updatedFiles.getGroupById(FileGroup.UPDATED_ID).add("test", "test", null);

        new Expectations() {{
            mergeState.getUpdatedFiles(); result = updatedFiles;

            mergeState.getProject().getMessageBus().syncPublisher(VcsAnnotationRefresher.LOCAL_CHANGES_CHANGED);
            result = refresher;

            mergeState.getProject().isOpen(); result = true;
            ApplicationManager.getApplication(); result = application;
            application.isDispatchThread(); result = true;
            application.isHeadlessEnvironment(); result = true;
        }};

        handler.afterMerge();

        new Verifications() {{
            collector.collect(updatedFiles, (List<VcsException>) any);
            RefreshVFsSynchronously.updateAllChanged(updatedFiles);
            UpdateFilesHelper.iterateFileGroupFilesDeletedOnServerFirst(updatedFiles,
                    (UpdateFilesHelper.Callback) any);

            mergeState.getLocalHistoryAction().finish();
            new UpdatedFilesNotifier(updatedFiles);
            updatedFilesNotifier.prepareNotificationWithUpdateInfo();

            updateInfoTree.expandRootChildren();
        }};
    }

    @Test
    public void refreshFilesMarkedAsDirty(
            @Mocked final RefreshVFsSynchronously refreshVFsSynchronously,
            @Mocked final VcsAnnotationRefresher refresher
    ) throws Exception {
        UpdatedFiles updatedFiles = UpdatedFiles.create();
        FileGroup changedOnServer = updatedFiles.getGroupById(FileGroup.CHANGED_ON_SERVER_ID);
        FileGroup child = new FileGroup("test child 1", "test status 1", true, FileGroup.REMOVED_FROM_REPOSITORY_ID, true);
        child.add("child1", "child1", null);
        changedOnServer.addChild(child);

        FileGroup child2 = new FileGroup("test child 2", "test status 2", true, "child2", true);
        child2.add("child2", "child2", null);
        changedOnServer.addChild(child2);

        FileGroup child3 = new FileGroup("test child 3", "test status 3", true, "child3", true);
        child3.add("child3", "child3", null);

        updatedFiles.registerGroup(changedOnServer);
        updatedFiles.registerGroup(child3);

        new Expectations() {{
            mergeState.getUpdatedFiles(); result = updatedFiles;
            RefreshVFsSynchronously.updateAllChanged(updatedFiles);
            mergeState.getProject().getMessageBus().syncPublisher(VcsAnnotationRefresher.LOCAL_CHANGES_CHANGED);
            result = refresher;
        }};

        handler.refreshFiles();

        new VerificationsInOrder() {{
            refresher.dirty("child1");
            refresher.dirty("child2");
            refresher.dirty("child3");
        }};
    }

    @Test
    public void showUpdatedFilesWithNoChanges(
            @Mocked final UpdatedFilesNotifier notifier
    ) throws Exception {
        final UpdatedFiles updatedFiles = UpdatedFiles.create();
        new Expectations() {{
            mergeState.getUpdatedFiles(); result = updatedFiles;
        }};

        handler.showUpdatedFiles();
        //verify no notification due to empty files
        new Verifications() {{
            notifier.prepareNotificationWithUpdateInfo(); times = 0;
        }};
    }

    @Test
    public void generateUpdateInfoTreeWithNoContentManager(
            @Mocked final ToolWindow changes
    ) throws Exception {
        new Expectations() {{
            mergeState.getProject().isOpen(); result = true;
            changes.getContentManager(); result = null;
        }};

        assertThat(handler.generateUpdateInfoTree()).isNull();
    }

    @Test
    public void showUpdateInfoTreeWithNoContentManager(
            @Mocked final ToolWindow changes
    ) throws Exception {
        new Expectations() {{
            mergeState.getProject().isOpen(); result = true;
            changes.getContentManager(); result = null;
        }};

        // if it didn't stop due to no content manager,
        // it'll stop with an exception for null info tree
        handler.showUpdateTree(null);
    }
}