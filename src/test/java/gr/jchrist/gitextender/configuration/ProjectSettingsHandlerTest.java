package gr.jchrist.gitextender.configuration;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import gr.jchrist.gitextender.TestingUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ProjectSettingsHandlerTest extends BasePlatformTestCase {
    ProjectSettingsHandler handler;

    @Before
    public final void setUp() throws Exception {
        super.setUp();
        handler = new ProjectSettingsHandler(super.getProject());
        handler.clearSelectedModules();
    }

    @After
    public final void tearDown() throws Exception {
        handler.clearSelectedModules();
        super.tearDown();
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

        handler.addSelectedModule(module1);
        handler.removeSelectedModule(module1);
        assertThat(handler.loadSelectedModules()).as("expected no modules selected initially").isEmpty();
    }

    @Test
    public void setSelectedModules() throws Exception {
        handler.setSelectedModules(Arrays.asList("module1", "module2"));
        assertThat(handler.loadSelectedModules()).containsExactly("module1", "module2");
    }

    @Test
    public void testGetProject() throws Exception {
        assertThat(handler.getProject()).isEqualTo(super.getProject());
    }
}
