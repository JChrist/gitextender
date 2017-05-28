package gr.jchrist.gitextender.dialog;

import gr.jchrist.gitextender.TestingUtil;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectModuleDialogTest {
    private TestingUtil.BaseTest base;
    private ProjectSettingsHandler projectSettingsHandler;
    private List<String> testRepos = new ArrayList<>(Arrays.asList("repo1", "repo2", "repo3"));

    @Before
    public void before() throws Exception {
        base = TestingUtil.getBaseTest();
        base.setUp();
        projectSettingsHandler = new ProjectSettingsHandler(base.getProject());
    }

    @After
    public void after() throws Exception {
        base.tearDown();
    }

    @Test
    public void init() throws Exception {
        base.invokeTestRunnable(() -> {
            SelectModuleDialog smd = new SelectModuleDialog(projectSettingsHandler, testRepos);
            assertThat(smd).isNotNull();

        });
    }
}