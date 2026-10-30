package changecaseplugin;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static changecaseplugin.CaseFormat.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChangeCaseTest {

    private static final Map<CaseFormat, String> FIRST_NAME = Map.of(
            CAMEL, "firstName", PASCAL, "FirstName", SNAKE, "first_name", SCREAMING_SNAKE, "FIRST_NAME",
            KEBAB, "first-name", SPACE, "first name", TITLE, "First Name");

    @Test
    public void everyFormatConvertsToEveryOther() {
        for (String input : FIRST_NAME.values()) {
            FIRST_NAME.forEach((format, expected) -> assertEquals(input + " as " + format, expected, format.convert(input)));
        }
    }

    @Test
    public void splitsHumpsAndAcronyms() {
        assertEquals(List.of("XML", "Http", "Parser2", "Go"), words("XMLHttpParser2Go"));
        assertEquals(List.of("md5", "Hash"), words("md5Hash"));
    }

    @Test
    public void keepsLeadingAndTrailingSymbols() {
        assertEquals("$user_name", SNAKE.convert("$userName"));
        assertEquals("__initValue__", CAMEL.convert("__init_value__"));
        assertEquals("\"FIRST_NAME\";", SCREAMING_SNAKE.convert("\"firstName\";"));
    }

    @Test
    public void cyclesThroughEveryFormat() {
        assertEquals(List.of("FIRST_NAME", "first-name", "first name", "First Name", "firstName", "FirstName", "first_name"),
                run("first_name", List.of(values()), 7));
    }

    @Test
    public void singleWordStillCycles() {
        // "total" is camelCase, snake_case, kebab-case and space case at the same time
        assertEquals(List.of("Total", "total", "TOTAL", "total", "Total", "total"), run("total", List.of(values()), 6));
    }

    @Test
    public void onlyUsesTheGivenCycle() {
        List<CaseFormat> cycle = List.of(SNAKE, CAMEL);
        assertEquals(CAMEL, next("first_name", cycle, null));
        assertEquals(SNAKE, next("firstName", cycle, null));
        assertEquals(SNAKE, next("first-name", cycle, null));
    }

    @Test
    public void nothingToConvert() {
        assertNull(next("42", List.of(values()), null));
        assertNull(next("--", List.of(values()), null));
    }

    @Test
    public void expandsToTheWholeWord() {
        String line = "$this->first_name = order-total;";
        assertEquals("first_name", ChangeCaseAction.wordAt(line, line.indexOf("name")).subSequence(line));
        assertEquals("order-total", ChangeCaseAction.wordAt(line, line.indexOf("total")).subSequence(line));
        assertEquals("", ChangeCaseAction.wordAt(line, line.indexOf("=") + 1).subSequence(line));
    }

    private static List<String> run(String text, List<CaseFormat> cycle, int presses) {
        List<String> seen = new ArrayList<>();
        CaseFormat last = null;
        for (int i = 0; i < presses; i++) {
            last = next(text, cycle, last);
            text = last.convert(text);
            seen.add(text);
        }
        return seen;
    }
}
