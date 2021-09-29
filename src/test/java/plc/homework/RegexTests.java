package plc.homework;

import com.sun.org.apache.xpath.internal.Arg;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                //examples
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),

                //Matching tests
                Arguments.of("period in the name", "jon.jonson@yahoo.com", true),
                Arguments.of("~ used in domain", "jakc@gmail~~.com", true),
                Arguments.of("Mixed Casing", "JackJackerson@GMaIl.com", true),
                Arguments.of("Only numbers where possible", "123@321.ufl", true),
                //Non-Matching tests
                Arguments.of("3+ char after '.' ", "GoAmerica@gmail.couk", false),
                Arguments.of("single letter", "a@gmail.com", false),
                Arguments.of("Capital Letter after '.'", "jack@mail.CoM", false),
                Arguments.of("sub 3 letters used after '.'", "jack@gmail.ca", false),
                Arguments.of("2 '.''s in the domain", "Jack@gmail.c.om", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),

                //pass cases
                Arguments.of("15 Characters", "thisis15charact", true),
                Arguments.of("17 Characters", "thisis17character", true),
                Arguments.of("19 Characters", "thisis19characters!", true),
                Arguments.of("15 Characters with \n", "12345678901234\n", true),
                Arguments.of("multiple escape usage w/ 13 char", "\n\n\n\n\n\n\n\n\n\n\\b\\", true),
                Arguments.of("Using non newline escape char", "\t234567890abc", true),
                //fail cases
                Arguments.of("12 Characters", "thisis12char", false),
                Arguments.of("21 Characters", "this is 21 characters", false),
                Arguments.of("16 Characters", "thisis16characte", false),
                Arguments.of("18 Characters", "thisis18characters", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),

                //pass cases
                Arguments.of("empty list", "[]", true),
                Arguments.of("list with space", "['a', 'b', 'c']", true),
                Arguments.of("list with mixed-space", "['a', 'b','c', 'd']", true),
                Arguments.of("Number chars", "['1','2', 'a']", true),
                Arguments.of("Special Characters", "['%']", true),
                Arguments.of("escape chars", "['\t']", true),

                //fail cases
                Arguments.of("bad escape", "['\\a']", false),
                Arguments.of("newline", "['\n']", true),
                Arguments.of("double brackets", "[['1']]", false),
                Arguments.of("multi-char elem", "['ab', 'b']", false),
                Arguments.of("Empty Char", "['']", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success );
    }

    public static Stream<Arguments> testDecimalRegex() {

        return Stream.of(
                Arguments.of("0 < test < 1", "0.423", true),
                Arguments.of("negative", "-1.4", true),
                Arguments.of("trailing 0's", "4.0000", true),
                Arguments.of("large sig figs", "10002.4213421", true),
                Arguments.of("true 0","0.000000", true),

                Arguments.of("No left side", ".4", false),
                Arguments.of("No right side", "1.", false),
                Arguments.of("No decimal", "10", false),
                Arguments.of("String", "false", false),
                Arguments.of("leading 0's", "04.2", false),
                Arguments.of("2 -'s", "--4.4", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {

        return Stream.of(
                Arguments.of("Name", "\"Bob Dylan\"", true),
                Arguments.of("Escape \\b", "\"Bob\\bDylan\"", true),
                Arguments.of("Escape \\", "\"Bob\\\\ Dylan\"", true),
                Arguments.of("Escape \\n", "\"Bob\\nDylan\"", true),
                Arguments.of("Escape \\r", "\"Bob\\rDylan\"", true),
                Arguments.of("Escape \\'", "\"Bob\\' Dylan\"", true),
                Arguments.of("Escape \\\"", "\"Bob \\\" Dylan\"", true),
                Arguments.of("Escape \\t", "\"Bob \\t Dylan\"", true),
                Arguments.of("Numbers", "\"1234\"", true),
                Arguments.of("Nothing after Escape", "\"Bob \\\\\"", true),
                Arguments.of("Start Escape", "\"\\n Bob123\"", true),
                Arguments.of("No capitals", "\"bob\"", true),
                Arguments.of("null String", "\"\"", true),
                Arguments.of("Symbols", "\"$$%#\"", true),

                Arguments.of("No enclosing \"", "\"Bob", false),
                Arguments.of("bad Escape", "\"bad\\l\"", false),
                Arguments.of("blank Escape", "\" bad \\ escape", false),
                Arguments.of("Chars outside \" \"", "f\"Bad\"", false),
                Arguments.of("wrong quotes", "' Bad '", false)

        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
