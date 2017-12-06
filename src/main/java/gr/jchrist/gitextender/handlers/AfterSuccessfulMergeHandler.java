package gr.jchrist.gitextender.handlers;

import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsAnnotationRefresher;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesCache;
import com.intellij.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.intellij.openapi.vcs.update.*;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ContentUtilEx;
import com.intellij.util.WaitForProgressToShow;
import git4idea.merge.MergeChangeCollector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AfterSuccessfulMergeHandler extends AfterMergeHandler {
    public AfterSuccessfulMergeHandler(@NotNull MergeState mergeState) {
        super(mergeState);
    }

    @Override
    public void afterMerge() {
        //we managed to update the branch, so let's try to get which files were updated
        gatherChanges();
        refreshFiles();
        mergeState.getLocalHistoryAction().finish();

        showUpdatedFiles();
    }

    protected void gatherChanges() {
        MergeChangeCollector collector = new MergeChangeCollector(
                mergeState.getProject(), mergeState.getRoot(), mergeState.getStart());
        final List<VcsException> exceptions = new ArrayList<>();
        collector.collect(mergeState.getUpdatedFiles(), exceptions);
    }

    protected void refreshFiles() {
        //request that we update repo for all updated files
        RefreshVFsSynchronously.updateAllChanged(mergeState.getUpdatedFiles());

        //also notify annotations
        final VcsAnnotationRefresher refresher = mergeState.getProject().getMessageBus()
                .syncPublisher(VcsAnnotationRefresher.LOCAL_CHANGES_CHANGED);
        UpdateFilesHelper.iterateFileGroupFilesDeletedOnServerFirst(mergeState.getUpdatedFiles(),
                (filePath, groupId) -> refresher.dirty(filePath));
    }

    protected void showUpdatedFiles() {
        //if no files were updated, then there's nothing else to do
        if (mergeState.getUpdatedFiles().isEmpty()) {
            return;
        }

        final String notificationWithUpdateInfo =
                new UpdatedFilesNotifier(mergeState.getUpdatedFiles())
                        .prepareNotificationWithUpdateInfo();

        WaitForProgressToShow.runOrInvokeLaterAboveProgress(() -> {
            final UpdateInfoTree tree = generateUpdateInfoTree();

            CommittedChangesCache.getInstance(mergeState.getProject())
                    .processUpdatedFiles(mergeState.getUpdatedFiles(), tree::setChangeLists);
            showUpdateTree(tree);
            VcsBalloonProblemNotifier.showOverChangesView(mergeState.getProject(),
                    "VCS Update Finished" + notificationWithUpdateInfo, MessageType.INFO);
        }, null, mergeState.getProject());
    }

    public UpdateInfoTree generateUpdateInfoTree() {
        if (!mergeState.getProject().isOpen() || mergeState.getProject().isDisposed()) return null;
        ContentManager contentManager = getContentManager(mergeState.getProject());
        if (contentManager == null) {
            return null;  // content manager is made null during dispose; flag is set later
        }
        //now create the *after* update label, so that we have the before (from mergeState) and the after update diff
        final Label after = LocalHistory.getInstance().putSystemLabel(mergeState.getProject(),
                "After updating:" + mergeState.getProject().getName() + " " +
                        mergeState.getLocalBranchName());

        ActionInfo actionInfo = ActionInfo.UPDATE;
        RestoreUpdateTree restoreUpdateTree = RestoreUpdateTree.getInstance(mergeState.getProject());
        restoreUpdateTree.registerUpdateInformation(mergeState.getUpdatedFiles(), actionInfo);
        final String text = "GitExtender updated Repo:" + mergeState.getRepoName() + " " +
                mergeState.getLocalBranchName() + "->" + mergeState.getRemoteBranchName();
        //don't use the one from project level vcs manager, since we want to split creation of tree from showing
        /*final UpdateInfoTree updateInfoTree = ProjectLevelVcsManagerEx.getInstanceEx(project).
                showUpdateProjectInfo(updatedFiles, text, actionInfo, false);*/

        final UpdateInfoTree updateInfoTree = new UpdateInfoTree(contentManager, mergeState.getProject(),
                mergeState.getUpdatedFiles(), text, actionInfo);
        //todo the update info tree is not quite an _update_ info tree
        //files changed in branches other than the one checked out will not show correct changes
        //however, leave the actions as is, this should be fixed in a future version (in a way that I don't know yet :)
        /*{
            @Override
            protected void addActionsTo(DefaultActionGroup group) {
                //do not add any actions, since they will be wrong anyway
                //the changed files might be in another branch than the one checked out, so no real diffing can be done
            }
        };*/

        updateInfoTree.setBefore(mergeState.getBefore());
        updateInfoTree.setAfter(after);

        updateInfoTree.setCanGroupByChangeList(false);

        return updateInfoTree;
    }

    protected void showUpdateTree(final UpdateInfoTree updateInfoTree) {
        if (!mergeState.getProject().isOpen() || mergeState.getProject().isDisposed()) return;
        ContentManager contentManager = getContentManager(mergeState.getProject());
        if (contentManager == null) {
            return; // content manager is made null during dispose; flag is set later
        }
        final String tabName = mergeState.getRepoName() + " " + mergeState.getLocalBranchName() +
                "->" + mergeState.getRemoteBranchName();
        ContentUtilEx.addTabbedContent(contentManager, updateInfoTree,
                "Update Info", tabName, true, updateInfoTree);
        ToolWindowManager.getInstance(mergeState.getProject()).getToolWindow(ToolWindowId.VCS).activate(null);
        updateInfoTree.expandRootChildren();
    }

    public ContentManager getContentManager(Project project) {
        ToolWindow changes = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.VCS);
        return changes.getContentManager();
    }
}
