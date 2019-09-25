package gr.jchrist.gitextender.dialog;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class SelectModuleDialogTest extends BasePlatformTestCase {
    private ProjectSettingsHandler projectSettingsHandler;
    private List<String> testRepos = new ArrayList<>(Arrays.asList("repo1", "repo2", "repo3"));

    @Before
    public final void before() throws Exception {
        super.setUp();
        projectSettingsHandler = new ProjectSettingsHandler(super.getProject());
    }

    @After
    public final void after() throws Exception {
        super.tearDown();
    }

    @Test
    public void init() throws Exception {
        super.invokeTestRunnable(() -> {
            SelectModuleDialog smd = new SelectModuleDialog(projectSettingsHandler, testRepos);
            assertThat(smd).isNotNull();
        });
    }

    @Test
    public void selectAllNone() throws Exception {
        super.invokeTestRunnable(() -> {
            SelectModuleDialog smd = new SelectModuleDialog(projectSettingsHandler, testRepos);
            assertThat(smd).isNotNull();
            smd.selectAllBtn.doClick();
            assertThat(smd.repoChooser.getMarkedElements()).hasSize(testRepos.size()).containsAll(testRepos);
            assertThat(projectSettingsHandler.loadSelectedModules()).hasSize(testRepos.size()).containsAll(testRepos);
            smd.selectNoneBtn.doClick();
            assertThat(smd.repoChooser.getMarkedElements()).isEmpty();
            assertThat(projectSettingsHandler.loadSelectedModules()).isEmpty();
        });
    }

    @Test
    public void savedSelectedModules() throws Exception {
        projectSettingsHandler.setSelectedModules(Arrays.asList(testRepos.get(0), "invalid module"));
        super.invokeTestRunnable(() -> {
            SelectModuleDialog smd = new SelectModuleDialog(projectSettingsHandler, testRepos);
            assertThat(smd).isNotNull();
            assertThat(smd.repoChooser.getMarkedElements()).hasSize(1).contains(testRepos.get(0));
        });
    }
}
