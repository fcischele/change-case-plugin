package changecaseplugin;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/** Settings → Editor → Change Case: one checkbox per format, moved up and down to set the order. */
public final class ChangeCaseConfigurable implements Configurable {

    private CheckBoxList<CaseFormat> formats;

    @Override
    public String getDisplayName() {
        return "Change Case";
    }

    @Override
    public JComponent createComponent() {
        formats = new CheckBoxList<>();
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        panel.add(new JBLabel("Formats to cycle through, in order:"), BorderLayout.NORTH);
        panel.add(ToolbarDecorator.createDecorator(formats).disableAddAction().disableRemoveAction().createPanel(), BorderLayout.CENTER);

        // keeps the list at its natural height instead of filling the page
        JPanel top = new JPanel(new BorderLayout());
        top.add(panel, BorderLayout.NORTH);
        return top;
    }

    @Override
    public boolean isModified() {
        ChangeCaseSettings settings = ChangeCaseSettings.getInstance();
        return !shownOrder().equals(settings.order()) || !shownDisabled().equals(settings.disabled());
    }

    @Override
    public void apply() throws ConfigurationException {
        Set<CaseFormat> disabled = shownDisabled();
        if (disabled.size() == CaseFormat.values().length) {
            throw new ConfigurationException("Enable at least one format.");
        }
        ChangeCaseSettings.getInstance().update(shownOrder(), disabled);
    }

    @Override
    public void reset() {
        ChangeCaseSettings settings = ChangeCaseSettings.getInstance();
        Set<CaseFormat> disabled = settings.disabled();
        formats.clear();
        for (CaseFormat format : settings.order()) {
            formats.addItem(format, format.label, !disabled.contains(format));
        }
    }

    @Override
    public void disposeUIResources() {
        formats = null;
    }

    private List<CaseFormat> shownOrder() {
        return IntStream.range(0, formats.getModel().getSize()).mapToObj(formats::getItemAt).toList();
    }

    private Set<CaseFormat> shownDisabled() {
        Set<CaseFormat> disabled = EnumSet.noneOf(CaseFormat.class);
        for (CaseFormat format : shownOrder()) {
            if (!formats.isItemSelected(format)) {
                disabled.add(format);
            }
        }
        return disabled;
    }
}
