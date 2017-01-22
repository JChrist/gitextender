package gr.jchrist.gitextender;

import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.TestLoggerFactory;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import git4idea.DialogManager;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import git4idea.config.GitVcsSettings;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class TestingUtil {
    public static final GitCommandResult error =
            new GitCommandResult(false, 1,
                    Collections.singletonList("test error output"),
                    Collections.singletonList("error"),
                    new Exception("test exception"));
    public static final GitCommandResult success =
            new GitCommandResult(true, 0,
                    Collections.emptyList(),
                    Collections.singletonList("success"),
                    null);

    public static BaseTest getBaseTest() {
        return new BaseTest() {};
    }

    public static BaseIT getBaseIT() {
        return new BaseIT() {};
    }

    public static abstract class BaseTest {
        private final Inner inner;

        public BaseTest() {
            inner = new Inner() {};
        }

        public void setUp() throws Exception {
            inner.setUp();
        }

        public void tearDown() throws Exception {
            inner.tearDown();
        }

        @NotNull
        public CodeInsightTestFixture getFixture() {
            return inner.getFixture();
        }

        @NotNull
        public Project getProject() {
            return inner.getProject();
        }

        @NotNull
        public Module getModule() {
            return inner.getModule();
        }

        public void invokeTestRunnable(@NotNull Runnable runnable) throws Exception {
            inner.invokeTestRunnable(runnable);
        }

        private static abstract class Inner extends LightPlatformCodeInsightFixtureTestCase {
            @Override
            public void setUp() throws Exception {
                super.setUp();
            }

            @Override
            public void tearDown() throws Exception {
                super.tearDown();
            }

            public CodeInsightTestFixture getFixture() {
                return myFixture;
            }

            @Override
            public Project getProject() {
                return super.getProject();
            }

            public Module getModule() {
                return myModule;
            }

            @Override
            public void invokeTestRunnable(@NotNull Runnable runnable) throws Exception {
                super.invokeTestRunnable(runnable);
            }
        }
    }

    public static abstract class BaseIT {
        private final Inner inner;

        public BaseIT() {
            this.inner = new Inner(){};
        }

        public void setUp() throws Exception {
            inner.setUp();
        }

        public void tearDown() throws Exception {
            inner.tearDown();
        }

        public Collection<File> getFilesToDelete() {
            return inner.getFilesToDelete();
        }

        public File getTestRoot() {
            return inner.myTestRoot;
        }

        public VirtualFile getTestRootFile() {
            return inner.myTestRootFile;
        }

        public VirtualFile getProjectRoot() {
            return inner.myProjectRoot;
        }

        public String getProjectPath() {
            return inner.myProjectPath;
        }

        public String getTestStartedIndicator() {
            return inner.myTestStartedIndicator;
        }

        public ChangeListManagerImpl getChangeListManager() {
            return inner.changeListManager;
        }

        public GitRepositoryManager getGitRepositoryManager() {
            return inner.myGitRepositoryManager;
        }

        public GitVcsSettings getGitSettings() {
            return inner.myGitSettings;
        }

        public Git getGit() {
            return inner.myGit;
        }

        public GitVcs getVcs() {
            return inner.myVcs;
        }

        public DialogManager getDialogManager() {
            return inner.myDialogManager;
        }

        public VcsNotifier getVcsNotifier() {
            return inner.myVcsNotifier;
        }

        public Project getProject() {
            return inner.getProject();
        }

        private static abstract class Inner extends PlatformTestCase {
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

            public void setUp() throws Exception {
                myTestRoot = new File(FileUtil.getTempDirectory(), "testRoot");
                PlatformTestCase.myFilesToDelete.add(myTestRoot);
                checkTestRootIsEmpty(myTestRoot);

                EdtTestUtil.runInEdtAndWait(() -> super.setUp());
                myTestRootFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(myTestRoot);
                this.refresh();

                myTestStartedIndicator = enableDebugLogging();

                myProjectRoot = myProject.getBaseDir();
                myProjectPath = myProjectRoot.getPath();

                changeListManager = (ChangeListManagerImpl) ChangeListManager.getInstance(myProject);

                myGitSettings = GitVcsSettings.getInstance(myProject);
                myGitSettings.getAppSettings().setPathToGit("git");

                myDialogManager = ServiceManager.getService(DialogManager.class);
                myVcsNotifier = ServiceManager.getService(myProject, VcsNotifier.class);

                myGitRepositoryManager = GitUtil.getRepositoryManager(myProject);
                myGit = ServiceManager.getService(Git.class);
                myVcs = GitVcs.getInstance(myProject);
                myVcs.doActivate();

                addSilently();
                removeSilently();
            }

            public void tearDown() throws Exception {
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

            public void setUpModule() {
                // we don't need a module in Git tests
            }

            public boolean runInDispatchThread() {
                return false;
            }

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

            protected void updateChangeListManager() {
                ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
                VcsDirtyScopeManager.getInstance(myProject).markEverythingDirty();
                changeListManager.ensureUpToDate(false);
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
        }
    }
}
