package gr.jchrist.gitextender.configuration;

import gr.jchrist.gitextender.TestingUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectSettingsHandlerTest {
    ProjectSettingsHandler handler;
    private TestingUtil.BaseTest base;

    @Before
    public void setUp() throws Exception {
        base = TestingUtil.getBaseTest();
        base.setUp();
        handler = new ProjectSettingsHandler(base.getProject());
        handler.clearSelectedModules();
    }

    @After
    public void tearDown() throws Exception {
        handler.clearSelectedModules();
        base.tearDown();
    }

    @Test
    public void loadSelectedModules() throws Exception {
        assertThat(handler.loadSelectedModules()).as("expected no modules selected initially").isEmpty();
        final String module1 = "module1";
        handler.addSelectedModule(module1);
        assertThat(handler.loadSelectedModules()).as("expected single module present")
                .containsExactly(module1);

        final String module2 = "module2";
        handler.addSelectedModule(module2);
        assertThat(handler.loadSelectedModules()).as("expected 2 modules present")
                .containsExactly(module1, module2);

        handler.addSelectedModule(module1);
        handler.addSelectedModule(module2);
        assertThat(handler.loadSelectedModules()).as("expected 2 modules present")
                .containsExactly(module1, module2);

        handler.removeSelectedModule("invalid module");
        assertThat(handler.loadSelectedModules()).as("expected single module present")
                .containsExactly(module1, module2);

        handler.removeSelectedModule(module1);
        assertThat(handler.loadSelectedModules()).as("expected single module present")
                .containsExactly(module2);

        handler.clearSelectedModules();
        assertThat(handler.loadSelectedModules()).as("expected no modules selected initially").isEmpty();
    }

    @Test
    public void addRemoveModule() throws Exception {
    }

    @Test
    public void addSelectedModule() throws Exception {
    }

    @Test
    public void removeSelectedModule() throws Exception {
    }

    @Test
    public void setSelectedModules() throws Exception {
    }

    @Test
    public void clearSelectedModules() throws Exception {
    }

    @Test
    public void getProject() throws Exception {
    }

}