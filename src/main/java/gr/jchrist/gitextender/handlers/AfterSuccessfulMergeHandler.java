package gr.jchrist.gitextender.handlers;

import com.intellij.history.Label;
import com.intellij.history.LocalHistory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.changes.VcsAnnotationRefresher;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.update.ActionInfo;
import com.intellij.openapi.vcs.update.RestoreUpdateTree;
import com.intellij.openapi.vcs.update.UpdateFilesHelper;
import com.intellij.openapi.vcs.update.UpdateInfoTree;
import com.intellij.ui.content.ContentManager;
import com.intellij.vcs.ViewUpdateInfoNotification;
import git4idea.merge.MergeChangeCollector;
import org.jetbrains.annotations.NotNull;

public class AfterSuccessfulMergeHandler extends AfterMergeHandler {
    private static final Logger logger = Logger.getInstance(AfterSuccessfulMergeHandler.class);
    public AfterSuccessfulMergeHandler(@NotNull MergeState mergeState) {
        super(mergeState);
    }

    @Override
    public void afterMerge() {
        //we managed to update the branch, so let's try to get which files were updated
        gatherChanges();
        refreshFiles();
        mergeState.getLocalHistoryAction().finish();

        // TODO: disabling to work around issue https://github.com/JChrist/gitextender/issues/21
        // showUpdatedFiles();
    }

    protected void gatherChanges() {
        MergeChangeCollector collector = new MergeChangeCollector(
                mergeState.getProject(), mergeState.getRepo(), mergeState.getStart());
        try {
            collector.collect(mergeState.getUpdatedFiles());
        } catch (Exception e) {
            logger.warn("error collecting updated files", e);
        }
    }

    protected void refreshFiles() {
        //request that we update repo for all updated files
        //RefreshVFsSynchronously.updateAllChanged(mergeState.getUpdatedFiles());

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

        final UpdateInfoTree tree = generateUpdateInfoTree();
        showUpdateTree(tree);
    }

    public UpdateInfoTree generateUpdateInfoTree() {
        if (!mergeState.getProject().isOpen() || mergeState.getProject().isDisposed()) {
            logger.debug("project is not open or disposed! returning null tree!");
            return null;
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

        ProjectLevelVcsManagerEx plm = ProjectLevelVcsManagerEx.getInstanceEx(mergeState.getProject());
        ContentManager cm = plm != null ? plm.getContentManager() : null;
        if (cm == null) {
            logger.debug("content manager is null! returning null tree");
            return null;
        }

        final UpdateInfoTree updateInfoTree = new UpdateInfoTree(cm,
                mergeState.getProject(),
                mergeState.getUpdatedFiles(), text, actionInfo);

        updateInfoTree.setBefore(mergeState.getBefore());
        updateInfoTree.setAfter(after);

        updateInfoTree.setCanGroupByChangeList(false);

        return updateInfoTree;
    }

    protected void showUpdateTree(final UpdateInfoTree tree) {
        if (!mergeState.getProject().isOpen() || mergeState.getProject().isDisposed() || tree == null) return;
        ApplicationManager.getApplication().invokeLater(() -> ViewUpdateInfoNotification.focusUpdateInfoTree(mergeState.getProject(), tree), ModalityState.defaultModalityState());
    }
}
