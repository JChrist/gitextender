package gr.jchrist.gitextender.configuration;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectSettingsHandler {
    static final String PREFIX = "gr.jchrist.gitextender.";
    static final String SELECTED_MODULES_KEY = PREFIX + "selectedModules";

    private final Project project;
    private final PropertiesComponent properties;

    public ProjectSettingsHandler(@NotNull Project project) {
        this.project = project;
        this.properties = PropertiesComponent.getInstance(this.project);
    }

    @NotNull
    public List<String> loadSelectedModules() {
        List<String> modules = new ArrayList<>();

        String[] selectedModules = properties.getValues(SELECTED_MODULES_KEY);
        if (selectedModules == null || selectedModules.length == 0) {
            return modules;
        }

        modules.addAll(Arrays.asList(selectedModules));
        return modules;
    }

    public void addSelectedModule(@NotNull String module) {
        List<String> modules = loadSelectedModules();
        if (!modules.contains(module)) {
            modules.add(module);
            properties.setValues(SELECTED_MODULES_KEY, modules.toArray(new String[modules.size()]));
        }
    }

    public void removeSelectedModule(@NotNull String module) {
        List<String> modules = loadSelectedModules();
        if (modules.contains(module)) {
            modules.remove(module);
            if (modules.isEmpty()) {
                clearSelectedModules();
            } else {
                properties.setValues(SELECTED_MODULES_KEY, modules.toArray(new String[modules.size()]));
            }
        }
    }

    public void setSelectedModules(@NotNull List<String> modules) {
        properties.setValues(SELECTED_MODULES_KEY, modules.toArray(new String[modules.size()]));
    }

    public void clearSelectedModules() {
        properties.unsetValue(SELECTED_MODULES_KEY);
    }

    public Project getProject() {
        return project;
    }
}
