package gr.jchrist.gitextender;

import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.TestLoggerFactory;
import com.intellij.util.ArrayUtil;
import git4idea.DialogManager;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.Git;
import git4idea.config.GitVcsSettings;
import git4idea.repo.GitRepositoryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@RunWith(JUnit4.class)
public abstract class AbstractIT extends PlatformTestCase {
    protected File myTestRoot;
    protected VirtualFile myTestRootFile;
    protected VirtualFile myProjectRoot;
    protected String myProjectPath;
    protected String myTestStartedIndicator;
    protected ChangeListManagerImpl changeListManager;
    protected GitRepositoryManager myGitRepositoryManager;
    protected GitVcsSettings myGitSettings;
    protected Git myGit;
    protected GitVcs myVcs;
    protected DialogManager myDialogManager;
    protected VcsNotifier myVcsNotifier;

    @Rule
    public TestName testName = new TestName();

    @Override
    @Before
    public final void setUp() throws Exception {
        EdtTestUtil.runInEdtAndWait(() -> super.setUp());
        myTestRoot = new File(FileUtil.getTempDirectory(), "testRoot");
        myFilesToDelete.add(myTestRoot);
        checkTestRootIsEmpty(myTestRoot);

        myTestRootFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(myTestRoot);
        this.refresh();

        myTestStartedIndicator = enableDebugLogging();

        // myProjectRoot = myProject.getBaseDir();
        myProjectPath = myProject.getBasePath();

        changeListManager = (ChangeListManagerImpl) ChangeListManager.getInstance(myProject);

        myGitSettings = GitVcsSettings.getInstance(myProject);
        myGitSettings.getAppSettings().setPathToGit(GitExecutor.PathHolder.getGitExecutable());

        myDialogManager = ServiceManager.getService(DialogManager.class);
        myVcsNotifier = ServiceManager.getService(myProject, VcsNotifier.class);

        myGitRepositoryManager = GitUtil.getRepositoryManager(myProject);
        myGit = ServiceManager.getService(Git.class);
        myVcs = GitVcs.getInstance(myProject);
        myVcs.doActivate();

        addSilently();
        removeSilently();
    }

    @Override
    @After
    public final void tearDown() throws Exception {
        try {
            EdtTestUtil.runInEdtAndWait( () -> super.tearDown() );
        } finally {
            if (myAssertionsInTestDetected) {
                TestLoggerFactory.dumpLogToStdout(myTestStartedIndicator);
            }
        }
    }

    public Collection<File> getFilesToDelete() {
        return myFilesToDelete;
    }

    /**
     * Returns log categories which will be switched to DEBUG level.
     * Implementations must add theirs categories to the ones from super class,
     * not to erase log categories from the super class.
     * (e.g. by calling `super.getDebugLogCategories().plus(additionalCategories)`.
     */
    protected Collection<String> getDebugLogCategories() {
        return Collections.emptyList();
    }

    public File getIprFile() throws IOException {
        File projectRoot = new File(myTestRoot, "project");
        return FileUtil.createTempFile(projectRoot, getName() + "_", ProjectFileType.DOT_DEFAULT_EXTENSION);
    }

    @Override
    public void setUpModule() {
        // we don't need a module in Git tests
    }

    @Override
    public boolean runInDispatchThread() {
        return false;
    }

    @Override
    public String getName() {
        String name = super.getName();
        if (name != null) {
            return name;
        }
        return testName.getMethodName();
    }

    @Override
    public String getTestName(boolean lowercaseFirstLetter) {
        String name = super.getTestName(lowercaseFirstLetter);
        name = StringUtil.shortenTextWithEllipsis(name.trim().replace(" ", "_"), 12, 6, "_");
        if (name.startsWith("_")) {
            name = name.substring(1);
        }
        return name;
    }

    protected void refresh() {
        VfsUtil.markDirtyAndRefresh(false, true, false, myTestRootFile);
    }

    private void checkTestRootIsEmpty(File testRoot) {
        File[] files = testRoot.listFiles();
        if (files != null && files.length > 0) {
            LOG.warn("Test root was not cleaned up during some previous test run. " + "testRoot: " + testRoot +
                    ", files: " + Arrays.toString(files));
            for (File file : files) {
                LOG.assertTrue(FileUtil.delete(file));
            }
        }
    }

    private String enableDebugLogging() {
        TestLoggerFactory.enableDebugLogging(getTestRootDisposable(), ArrayUtil.toStringArray(getDebugLogCategories()));
        String testStartedIndicator = createTestStartedIndicator();
        LOG.info(testStartedIndicator);
        return testStartedIndicator;
    }

    private String createTestStartedIndicator() {
        return "Starting " + getClass().getName() + "." + getTestName(false) + Math.random();
    }

    protected void addSilently() {
        doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    }

    protected void removeSilently() {
        doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    }

    protected void doActionSilently(VcsConfiguration.StandardConfirmation op) {
        setStandardConfirmation(myProject, GitVcs.NAME, op, VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
    }

    public static void setStandardConfirmation(
            Project project, String vcsName, VcsConfiguration.StandardConfirmation op,
            VcsShowConfirmationOption.Value value) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        AbstractVcs vcs = vcsManager.findVcsByName(vcsName);
        VcsShowConfirmationOption option = vcsManager.getStandardConfirmation(op, vcs);
        option.setValue(value);
    }

    public File getTestRoot() {
        return myTestRoot;
    }

    public VirtualFile getTestRootFile() {
        return myTestRootFile;
    }

    public VirtualFile getProjectRoot() {
        return myProjectRoot;
    }

    public String getProjectPath() {
        return myProjectPath;
    }

    public String getTestStartedIndicator() {
        return myTestStartedIndicator;
    }

    public ChangeListManagerImpl getChangeListManager() {
        return changeListManager;
    }

    public GitRepositoryManager getGitRepositoryManager() {
        return myGitRepositoryManager;
    }

    public GitVcsSettings getGitSettings() {
        return myGitSettings;
    }

    public Git getGit() {
        return myGit;
    }

    public GitVcs getVcs() {
        return myVcs;
    }

    public DialogManager getDialogManager() {
        return myDialogManager;
    }

    public VcsNotifier getVcsNotifier() {
        return myVcsNotifier;
    }
}
