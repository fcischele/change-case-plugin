package changecaseplugin;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A way of writing a sequence of words. Text is converted by splitting it into words and joining them back in the
 * target format, so any format converts directly into any other.
 */
enum CaseFormat {
    CAMEL("camelCase", "", Word.LOWER, Word.CAPITALIZED),
    PASCAL("PascalCase", "", Word.CAPITALIZED, Word.CAPITALIZED),
    SNAKE("snake_case", "_", Word.LOWER, Word.LOWER),
    SCREAMING_SNAKE("SNAKE_CASE", "_", Word.UPPER, Word.UPPER),
    KEBAB("kebab-case", "-", Word.LOWER, Word.LOWER),
    SPACE("space case", " ", Word.LOWER, Word.LOWER),
    TITLE("Title Case", " ", Word.CAPITALIZED, Word.CAPITALIZED);

    // leading and trailing symbols ($var, "quoted", _private) are kept as they are
    private static final Pattern EDGES = Pattern.compile("([^\\p{L}\\p{N}]*)(.*?)([^\\p{L}\\p{N}]*)", Pattern.DOTALL);
    private static final Pattern SEPARATORS = Pattern.compile("[\\s_\\-]+");
    // fooBar -> foo|Bar, md5Hash -> md5|Hash, XMLParser -> XML|Parser
    private static final Pattern HUMPS = Pattern.compile("(?<=[\\p{Ll}\\p{N}])(?=\\p{Lu})|(?<=\\p{Lu})(?=\\p{Lu}\\p{Ll})");

    final String label;
    private final String separator;
    private final Word first;
    private final Word rest;

    CaseFormat(String label, String separator, Word first, Word rest) {
        this.label = label;
        this.separator = separator;
        this.first = first;
        this.rest = rest;
    }

    String convert(String text) {
        Matcher edges = edges(text);
        return edges.group(1) + join(words(edges.group(2))) + edges.group(3);
    }

    /**
     * The format that follows the one text is written in, skipping formats that would leave it unchanged.
     * A single word reads the same in several formats ("total" is camelCase, snake_case and kebab-case):
     * if {@code last} is one of them it is taken as the current format, so repeated calls still go around the cycle.
     *
     * @return null when no format in the cycle changes text
     */
    static @Nullable CaseFormat next(String text, List<CaseFormat> cycle, @Nullable CaseFormat last) {
        String core = edges(text).group(2);
        List<String> words = words(core);

        int current = last != null && last.join(words).equals(core) ? cycle.indexOf(last) : -1;
        for (int i = 0; current < 0 && i < cycle.size(); i++) {
            if (cycle.get(i).join(words).equals(core)) {
                current = i;
            }
        }

        for (int step = 1; step <= cycle.size(); step++) {
            CaseFormat format = cycle.get((current + step) % cycle.size());
            if (!format.join(words).equals(core)) {
                return format;
            }
        }
        return null;
    }

    static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        for (String part : SEPARATORS.split(text)) {
            if (!part.isEmpty()) {
                words.addAll(List.of(HUMPS.split(part)));
            }
        }
        return words;
    }

    private String join(List<String> words) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                joined.append(separator);
            }
            joined.append((i == 0 ? first : rest).apply(words.get(i)));
        }
        return joined.toString();
    }

    private static Matcher edges(String text) {
        Matcher matcher = EDGES.matcher(text);
        matcher.matches(); // every group can be empty, so this always matches
        return matcher;
    }

    private enum Word {
        LOWER, UPPER, CAPITALIZED;

        String apply(String word) {
            return switch (this) {
                case LOWER -> word.toLowerCase(Locale.ROOT);
                case UPPER -> word.toUpperCase(Locale.ROOT);
                case CAPITALIZED -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
            };
        }
    }
}
