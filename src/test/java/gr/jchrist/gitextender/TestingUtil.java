package gr.jchrist.gitextender;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import git4idea.commands.GitCommandResult;
import org.jetbrains.annotations.NotNull;

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
        return new BaseTest() {
        };
    }

    public static abstract class BaseTest {
        private final Inner inner;

        public BaseTest() {
            inner = new Inner() {
            };
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
}
