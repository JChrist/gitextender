package gr.jchrist.gitextender.dialog;

import com.intellij.ide.util.ElementsChooser;
import com.intellij.openapi.ui.DialogWrapper;
import gr.jchrist.gitextender.configuration.ProjectSettingsHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.List;

public class SelectModuleDialog extends DialogWrapper {
    private final ProjectSettingsHandler settingsHandler;
    private final List<String> repos;

    private JPanel contentPane;
    private JScrollPane scrollPane;
    protected JButton selectAllBtn;
    protected JButton selectNoneBtn;
    protected ElementsChooser<String> repoChooser;

    public SelectModuleDialog(@NotNull ProjectSettingsHandler projectSettingsHandler, @NotNull List<String> repos) {
        super(projectSettingsHandler.getProject());
        this.repos = repos;
        this.settingsHandler = projectSettingsHandler;

        init();
        setTitle("Select Modules to Update");
    }

    @Override
    protected void init() {
        super.init();

        this.selectAllBtn.addActionListener(evt -> {
            settingsHandler.setSelectedModules(repos);
            repoChooser.setAllElementsMarked(true);
        });
        this.selectNoneBtn.addActionListener(evt -> {
            settingsHandler.clearSelectedModules();
            repoChooser.setAllElementsMarked(false);
        });

        List<String> savedSelection = settingsHandler.loadSelectedModules();
        //make sure we don't have stale entries
        savedSelection.removeIf(str -> {
            if (!repos.contains(str)) {
                settingsHandler.removeSelectedModule(str);
                return true;
            }
            return false;
        });

        repoChooser.markElements(savedSelection);
        setOKActionEnabled(!savedSelection.isEmpty());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    protected void createUIComponents() {
        repoChooser = new ElementsChooser<>(repos, false);
        repoChooser.addElementsMarkListener((ElementsChooser.ElementsMarkListener<String>) (element, isMarked) -> {
            if (isMarked) {
                settingsHandler.addSelectedModule(element);
            } else {
                settingsHandler.removeSelectedModule(element);
            }

            setOKActionEnabled(!settingsHandler.loadSelectedModules().isEmpty());
        });
    }
}
