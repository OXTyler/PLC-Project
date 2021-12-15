package plc.project;

import com.sun.org.apache.xpath.internal.Arg;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Alphabetic with @", "@get_-Name", true),
                Arguments.of("Single Character", "a", true),
                Arguments.of("Hyphenated", "a-b-c", true),

                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Leading Digit With @", "@1fish2fish3fishbluefish", false),
                Arguments.of("Underscores", "___", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("zero", "0", true),
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),

                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Comma Separated", "1,234", false),
                Arguments.of("Leading Zeros", "007", false),
                Arguments.of("zero and more", "01234", false),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Hyphen", "-", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Trailing Zeros", "7.0000", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Leading 0 decimal", "0.1234", true),
                Arguments.of("Leading negative 0 decimal", "-0.1234", true),

                Arguments.of("Single Digit", "1", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Double Decimal", "1..0", false),
                Arguments.of("Multiple Decimal Points", "1243.1234.1234", false),
                Arguments.of("Multiple Decimals", "1.2.3", false),
                Arguments.of("Number Method", "1.toString()", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "'c'", true),
                Arguments.of("Numeric", "'2'", true),
                Arguments.of("Newline Escape", "'\\n'", true),
                Arguments.of("MultiChar", "'ab'", false),
                Arguments.of("Empty", "''", false),
                Arguments.of("Multiple", "'abc'", false),
                Arguments.of("Unterminated", "'", false),
                Arguments.of("Newline", "'\n'", false),
                Arguments.of("No leading quote", "c'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Symbols", "\"!@#$%^&*()\"", true),
                Arguments.of("Numeric", "\"1234@@@23a\\b\"", true),

                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Newline Unterminated","\"unterminated\n\"", false),
                Arguments.of("No leading quotation", "test\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Symbol", "$", true),
                Arguments.of("Plus Sign", "+", true),

                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testWhitespace() {
        return Stream.of(
                Arguments.of("Multiple Spaces", "one   two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 6)
                )),
                Arguments.of("Trailing Newline", "token\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0)
                )),
                Arguments.of("Not Whitespace", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two",4)
                )),
                Arguments.of("Not Whitespace 2", "one\rtwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two",4)
                )),
                Arguments.of("Not Whitespace 3", "one\ttwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two",4)
                ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMixedToken(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testMixedToken() {
        return Stream.of(
                Arguments.of("Multiple Decimals", "1.2.3", Arrays.asList(

                )),
                Arguments.of("Trailing Newline", "!====", Arrays.asList(

                )),
                Arguments.of("Weird Quotes", "'\"'string\"'\"", Arrays.asList(

                ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET; x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.OPERATOR, ";", 3),
                        new Token(Token.Type.IDENTIFIER, "x", 5),
                        new Token(Token.Type.OPERATOR, "=", 7),
                        new Token(Token.Type.INTEGER, "5", 9),
                        new Token(Token.Type.OPERATOR, ";", 10)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "55 555; 23", Arrays.asList(
                        new Token(Token.Type.INTEGER, "55", 0),
                        new Token(Token.Type.INTEGER, "555", 3),
                        new Token(Token.Type.OPERATOR, ";", 6),
                        new Token(Token.Type.INTEGER, "23", 8)
                )),
                Arguments.of("Example 4", "LET X,Y = 5.4,10;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "X", 4),
                        new Token(Token.Type.OPERATOR, ",", 5),
                        new Token(Token.Type.IDENTIFIER, "Y", 6),
                        new Token(Token.Type.OPERATOR, "=", 8 ),
                        new Token(Token.Type.DECIMAL, "5.4", 10 ),
                        new Token(Token.Type.OPERATOR, ",", 13 ),
                        new Token(Token.Type.INTEGER, "10", 14 ),
                        new Token(Token.Type.OPERATOR, ";", 16 )
                ))

        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
