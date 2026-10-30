package changecaseplugin;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts the selection of every caret, or the word under it when nothing is selected, to the next enabled case format.
 * All carets go to the same format.
 */
public final class ChangeCaseAction extends AnAction implements DumbAware {

    // format produced by the previous run, see CaseFormat.next
    private CaseFormat last;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(CommonDataKeys.EDITOR) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null || !EditorModificationUtil.requestWriting(editor)) {
            return;
        }

        Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();
        Map<Caret, TextRange> ranges = new LinkedHashMap<>();
        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            ranges.put(caret, caret.hasSelection()
                    ? new TextRange(caret.getSelectionStart(), caret.getSelectionEnd())
                    : wordAt(text, caret.getOffset()));
        }

        List<CaseFormat> cycle = ChangeCaseSettings.getInstance().cycle();
        CaseFormat target = ranges.values().stream()
                .map(range -> CaseFormat.next(range.subSequence(text).toString(), cycle, last))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (target == null) {
            return;
        }
        last = target;

        // bottom-up, so replacing one range does not move the ones still to do
        List<Caret> carets = new ArrayList<>(ranges.keySet());
        carets.sort(Comparator.comparingInt((Caret caret) -> ranges.get(caret).getStartOffset()).reversed());

        WriteCommandAction.runWriteCommandAction(editor.getProject(), "Change Case", null, () -> {
            int done = Integer.MAX_VALUE; // start of the last converted range: two carets can sit in the same word
            for (Caret caret : carets) {
                TextRange range = ranges.get(caret);
                if (range.getEndOffset() > done) {
                    continue;
                }
                String original = range.subSequence(document.getCharsSequence()).toString();
                String converted = target.convert(original);
                if (converted.equals(original)) {
                    continue;
                }
                int start = range.getStartOffset();
                document.replaceString(start, range.getEndOffset(), converted);
                caret.moveToOffset(start + converted.length());
                caret.setSelection(start, start + converted.length());
                done = start;
            }
        });
    }

    /** Letters, digits and the separators of the formats written without spaces. */
    static TextRange wordAt(CharSequence text, int offset) {
        int start = offset;
        int end = offset;
        while (start > 0 && isWordChar(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && isWordChar(text.charAt(end))) {
            end++;
        }
        return new TextRange(start, end);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }
}
