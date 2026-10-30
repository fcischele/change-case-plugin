package changecaseplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Which formats the action cycles through, and in which order. Shared by all projects. */
@Service
@State(name = "ChangeCase", storages = @Storage("changeCase.xml"))
public final class ChangeCaseSettings implements PersistentStateComponent<ChangeCaseSettings.Saved> {

    /** Formats are stored by enum name. */
    public static final class Saved {
        public List<String> order = new ArrayList<>();
        public List<String> disabled = new ArrayList<>();
    }

    private Saved saved = new Saved();

    static ChangeCaseSettings getInstance() {
        return ApplicationManager.getApplication().getService(ChangeCaseSettings.class);
    }

    @Override
    public @NotNull Saved getState() {
        return saved;
    }

    @Override
    public void loadState(@NotNull Saved state) {
        saved = state;
    }

    /** Every format in the configured order; formats the saved order does not know yet go at the end. */
    List<CaseFormat> order() {
        Set<CaseFormat> order = parse(saved.order).collect(Collectors.toCollection(LinkedHashSet::new));
        order.addAll(List.of(CaseFormat.values()));
        return List.copyOf(order);
    }

    Set<CaseFormat> disabled() {
        return parse(saved.disabled).collect(Collectors.toCollection(() -> EnumSet.noneOf(CaseFormat.class)));
    }

    /** The enabled formats, in order. */
    List<CaseFormat> cycle() {
        Set<CaseFormat> disabled = disabled();
        return order().stream().filter(format -> !disabled.contains(format)).toList();
    }

    void update(List<CaseFormat> order, Set<CaseFormat> disabled) {
        saved.order = names(order.stream());
        saved.disabled = names(disabled.stream());
    }

    // names no longer in CaseFormat are dropped
    private static Stream<CaseFormat> parse(List<String> names) {
        return names.stream().flatMap(name -> Arrays.stream(CaseFormat.values()).filter(format -> format.name().equals(name)));
    }

    private static List<String> names(Stream<CaseFormat> formats) {
        return formats.map(Enum::name).collect(Collectors.toCollection(ArrayList::new));
    }
}
