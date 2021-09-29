package plc.project;

import java.awt.print.PrinterAbortException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */

public final class Lexer {


    private final CharStream chars;


    public Lexer(String input) {
        chars = new CharStream(input);
    }
    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token>Tokens =new ArrayList<Token>();
        while(chars.has(0)){

            if (peek("[ \b\n\r\t]")){
                chars.index++;

            } else{
               Tokens.add(lexToken());

            }

        }
        return Tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("@?|[A-Za-z]")) return lexIdentifier();
        if (peek("-","[0-9]") || peek("[0-9]")) return lexNumber();
        else if(peek("'")) return lexCharacter();
        else if(peek("\"")) return lexString();
        else return lexOperator();
    }

    public Token lexIdentifier() {


        if (match("@", "[A-Za-z]"));
        else if (match("[A-Za-z]"));

        while(match("[A-Za-z0-9_-]*"));

        return chars.emit(Token.Type.IDENTIFIER);
    }


    public Token lexNumber() {
        boolean foundDecimalPoint = false;

        // take care of cases of first char being '-'or '0'
        if (peek("-")) {
            chars.advance();
            if (peek("0")) { // if -0
                chars.advance();
                if (peek(".")) { // must be decimal
                    chars.advance();
                    foundDecimalPoint = true;
                } else {
                    return chars.emit(Token.Type.DECIMAL);
                }
            }
        } else if (peek("0")) { // if 0, must be just 0 or 0.
            chars.advance();
            if (chars.has(1)) { // if more than one char left
                if (match( ".")) { // must be decimal point
                    foundDecimalPoint = true;
                } else {
                    return chars.emit(Token.Type.INTEGER);
                }
            } else {
                return chars.emit(Token.Type.INTEGER); // return 0 because no more chars left
            }
        }

        while (chars.has(0)) {
            String pattern = (foundDecimalPoint) ? "[0-9]" : "\\.|[0-9]";

            if (peek("\\.")) {
                if (!chars.has(1)) {
                    return chars.emit(Token.Type.DECIMAL);
                }

                if (!foundDecimalPoint) {
                    foundDecimalPoint = true;
                } else { // if already found a decimal point
                    return chars.emit(Token.Type.DECIMAL);
                }
            }

            if (peek(pattern)) {
                chars.advance();
            } else if (peek("[ ;\b\n\r\t]")) {
                return chars.emit((foundDecimalPoint) ? Token.Type.DECIMAL : Token.Type.INTEGER);
            } else {
                return chars.emit((foundDecimalPoint) ? Token.Type.DECIMAL : Token.Type.INTEGER);
            }
        }

        return chars.emit((foundDecimalPoint) ? Token.Type.DECIMAL : Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        chars.advance(); //gets rid of peeked '
        if(chars.has(1)){ //checks for char at index 0
            if (match("[^'\\n\\r\\\\]")){

                if (match("'")) return chars.emit(Token.Type.CHARACTER);
                throw new ParseException("Invalid Character at Index: " + (chars.index+1), chars.index+1);

            } else if (peek("\\\\")) {
                lexEscape();
                if (match("'")) return chars.emit(Token.Type.CHARACTER);
                throw new ParseException("Invalid Character at Index: " + (chars.index+1), chars.index+1);
            }
            } else{
                throw new ParseException("Invalid Character at Index: " + (chars.index+1), chars.index+1);
            }

       throw new ParseException("Invalid Character at Index " + (chars.index+1), chars.index+1);
    }

    public Token lexString() {
        match("\"");
        String stringPattern = "[^\"\n\r\\\\]";
        while(chars.has(0)){
            if(match(stringPattern));
            else if(peek("\\\\")) lexEscape();
            else if(match("\"")) return chars.emit(Token.Type.STRING);
            else throw new ParseException("Invalid Character Usage", chars.index+1);
        }

        throw new ParseException("Invalid Character Usage", chars.index);
    }

    public void lexEscape() {
        if (match("\\\\", "[bnrt'\"\\\\]")){
            return;
        }
        throw new ParseException("Invalid escape", chars.index+1);
    }

    public Token lexOperator() {

        if (chars.has(0) && (match("!", "=") ||
           match("=", "=") ||
           match("&", "&") ||
           match("|","|") ||
           match("(.)"))) {
                return chars.emit(Token.Type.OPERATOR);
            }
        throw new ParseException("Invalid Character Usage", chars.index);
        }


    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++){

            if ( !chars.has(i) ||
                !String.valueOf(chars.get(i)).matches(patterns[i])){
                    return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek){

            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }

        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {

            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
