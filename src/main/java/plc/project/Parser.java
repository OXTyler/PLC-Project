package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {
    interface ExpressionParser {
        public Ast.Expression parse();
    }

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (match("LET")) {
            String name = "";

            if (!peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("An identifier is needed after LET", tokens.index - 1);
            }

            name = tokens.get(0).getLiteral();
            tokens.advance();

            Ast.Expression exp = null;

            if (match("=")) {
                exp = parseExpression();
            }

            if (match(";")) {
                return new Ast.Statement.Declaration(name, Optional.ofNullable(exp));
            }

        } else if (peek("SWITCH")) {
            // TODO
        } else if (peek("IF")) {
            // TODO
        } else if (peek("WHILE")) {
            // TODO
        } else if (peek("RETURN")) {
            // TODO
        } else {
            // first evaluate first expression
            Ast.Expression firstExp = parseExpression();
            // check if there's an equals sign
            if (match("=")) {
                // evaluate second expression
                Ast.Expression secondExp = parseExpression();
                if(match(";")) return new Ast.Statement.Assignment(firstExp, secondExp);
                throw new ParseException("Missing semi-colon", tokens.get(0).getIndex());
            } else{
                if(match(";")) return new Ast.Statement.Expression(firstExp);
                throw new ParseException("Missing semi-colon", tokens.get(0).getIndex());
            }


        }

        throw new ParseException("Invalid Statement", tokens.get(0).getIndex());
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression(){
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() {
        return parseRulesForBinaryExpressions(this::parseComparisonExpression, new String[]{"&&", "||"}, new String[]{});
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() {
        return parseRulesForBinaryExpressions(this::parseAdditiveExpression, new String[]{"!=", "==", ">", "<"}, new String[]{"&&", "||"});
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression(){
        return parseRulesForBinaryExpressions(this::parseMultiplicativeExpression, new String[]{"+", "-"}, new String[]{"&&", "||", "!=", "==", ">", "<"});
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        return parseRulesForBinaryExpressions(this::parsePrimaryExpression, new String[]{"^", "*", "/"}, new String[]{"&&", "||", "!=", "==", ">", "<", "+", "-"});
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL")) {
            tokens.advance();
            return new Ast.Expression.Literal(null);

        } else if (peek("TRUE")) {

            Ast.Expression.Literal val = new Ast.Expression.Literal(Boolean.TRUE);
            tokens.advance();
            return val;

        } else if (peek("FALSE")) {
            Ast.Expression.Literal val = new Ast.Expression.Literal(Boolean.FALSE);
            tokens.advance();
            return val;

        } else if (peek(Token.Type.INTEGER)) {

            BigInteger i = new BigInteger(tokens.get(0).getLiteral());
            tokens.advance();
            return new Ast.Expression.Literal(i);

        } else if (peek(Token.Type.DECIMAL)) {

            BigDecimal d = new BigDecimal(tokens.get(0).getLiteral());
            tokens.advance();
            return new Ast.Expression.Literal(d);

        } else if (peek(Token.Type.CHARACTER)) {

            char c = replaceEscaped(tokens.get(0).getLiteral()).charAt(1); // use 1 because literal is in format 'c'
            tokens.advance();
            return new Ast.Expression.Literal(c);

        } else if (peek(Token.Type.STRING)) {

            String s = tokens.get(0).getLiteral();
            s = s.substring(1, s.length() - 1); // trim double quotes
            s = replaceEscaped(s);

            tokens.advance();
            return new Ast.Expression.Literal(s);

        } else if(match("(")) {
            Ast.Expression val = parseExpression();
            if(match(")")) return new Ast.Expression.Group(val);


        } else if (peek(Token.Type.IDENTIFIER)){
            Ast.Expression.Access val = new Ast.Expression.Access(Optional.empty(),tokens.get(0).getLiteral());
            tokens.advance();
            if (match("(")){
                List<Ast.Expression> args = new ArrayList<Ast.Expression>();
                if(match(")")) return new Ast.Expression.Function(val.getName(), args);
                    while (tokens.has(0)) {
                        args.add(parseExpression());
                        if(!match(",")){
                            if(match(")"))return new Ast.Expression.Function(val.getName(), args);
                            throw new ParseException("Invalid arguments", tokens.get(0).getIndex());
                        }
                    }


            } else if (match("[")){
                return new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), tokens.get(0).getLiteral())), val.getName());
            }
            return val;
        }

        if(tokens.has(0)) throw new ParseException("Invalid expression", tokens.get(0).getIndex()); //in case its missing at the end
        throw new ParseException("Invalid expression", tokens.get(-1).getIndex());

    }

    /**
     * Returns the expression for rules for binary expressions (parseLogicalExpression, parseAdditiveExpression, etc)
     *
     * @param next the next expression parser function
     * @param operators the operators of the current rule
     * @param lowerOperators the lower precedent operators of the current rule
     * @return the expression
     */
    private Ast.Expression parseRulesForBinaryExpressions(ExpressionParser next, String[] operators, String[] lowerOperators) {
        Ast.Expression exp = next.parse();
        // check if there's an operator
        for (String operator : operators) {
            if (match(operator)) {

                Ast.Expression right = parseExpression();

                if (right instanceof Ast.Expression.Binary) {
                    String rightOperator = ((Ast.Expression.Binary) right).getOperator();
                    Ast.Expression rightRight = ((Ast.Expression.Binary) right).getRight();
                    Ast.Expression rightLeft = ((Ast.Expression.Binary) right).getLeft();
                    // merge operator arrays
                    List<String> equalOrLowerPriority = new ArrayList<>(Arrays.asList(operators));
                    equalOrLowerPriority.addAll(Arrays.asList(lowerOperators));

                    for (String o : equalOrLowerPriority) { // if right operator is of equal or lower priority
                        if (rightOperator.equals(o)) {
                            Ast.Expression.Binary left = new Ast.Expression.Binary(operator, exp, rightLeft);
                            return new Ast.Expression.Binary(rightOperator, left, rightRight);
                        }
                    }
                }

                return new Ast.Expression.Binary(operator, exp, right);
            }
        }

        return exp;
    }

    /**
     * Replaces all escaped with characters in a string with the actual char
     * @param s the string whose characters are to be replaced
     * @return the string with escaped sequences replaced with the actual corresponding char
     */
    private String replaceEscaped(String s) {
        s = s.replaceAll("\\\\b", "\b");
        s = s.replaceAll("\\\\r", "\r");
        s = s.replaceAll("\\\\n", "\n");
        s = s.replaceAll("\\\\t", "\t");
        s = s.replaceAll("\\\\'", "'");
        s = s.replaceAll("\\\\\"", "\"");
        s = s.replaceAll("\\\\", "\\");
        return s;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
