package com.ndtp.input;

import com.ndtp.NDTPLexer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;

import java.util.*;

/**
 * This class can be added to either the lexer or the parser error reporting
 * chains (or both). All it does is keep track of whether an error was detected,
 * so after parsing you can call sawError() to see if there was a problem.
 * <p>
 * For NDTP, we can use this in the front-end for displaying error messages.
 * Simply call NDTPErrorListener.getErrorIterator() and
 * NDTPErrorListener.getWarningIterator() to get an Iterator object for them.
 *
 * @author Joshua Crotts
 * @modified 2/20/2021
 */
public class NDTPErrorListener extends BaseErrorListener {

    /**
     * Set of all compiler errors generated while parsing.
     */
    private static final Set<Message> errors = new HashSet<>();

    /**
     * Set of all warning errors generated while parsing.
     */
    private static final Set<Message> warnings = new HashSet<>();

    /**
     * Keeps track of whether we have encountered an error or not.
     */
    private static boolean gotError = false;

    /**
     * Keeps track of whether we have encountered a warning or not.
     */
    private static boolean gotWarning = false;

    public NDTPErrorListener() {
        super();
    }

    /**
     * Prints an error message to the console with the line and column number
     * specified by the ParserRuleContext. The error flag is also set.
     *
     * @param ctx
     * @param errorMsg
     */
    public static void syntaxError(ParserRuleContext ctx, String errorMsg) {
        NDTPErrorListener.gotError = true;
        int lineNo = -1;
        int colNo = -1;

        if (ctx != null) {
            lineNo = ctx.start.getLine();
            colNo = ctx.start.getCharPositionInLine();
        } else {
            throw new IllegalArgumentException(
                    "Internal compiler error - ParserRuleContext cannot be null in ErrorListener.");
        }

        NDTPErrorListener.errors.add(new Message(errorMsg, colNo));
    }

    /**
     * Prints an warning message to the console with the line and column number
     * specified by the ParserRuleContext.
     *
     * @param ctx
     * @param warningMsg
     * @return void.
     */
    public static void syntaxWarning(ParserRuleContext ctx, String warningMsg) {
        NDTPErrorListener.gotWarning = true;
        int lineNo = -1;
        int colNo = -1;

        if (ctx != null) {
            lineNo = ctx.start.getLine();
            colNo = ctx.start.getCharPositionInLine();
        } else {
            throw new IllegalArgumentException(
                    "Internal compiler error - ParserRuleContext cannot be null in ErrorListener.");
        }

        NDTPErrorListener.warnings.add(new Message(warningMsg, colNo));
    }

    /**
     * Prints error messages generated through parsing the syntax tree to standard
     * error.
     *
     * @return void.
     */
    public static void printErrors() {
        List<Message> errorList = new ArrayList<Message>(NDTPErrorListener.errors);
        errorList.sort(Comparator.comparing(Message::getColNo));
        System.err.print("ERRORS(" + NDTPErrorListener.errors.size() + "):\n");
        for (Message error : errorList) {
            System.err.println(error);
        }
    }

    /**
     * Prints warning messages generated through parsing the syntax tree to standard
     * out.
     *
     * @return void.
     */
    public static void printWarnings() {
        List<Message> warningList = new ArrayList<Message>(NDTPErrorListener.warnings);
        warningList.sort(Comparator.comparing(Message::getColNo));
        System.out.print("WARNINGS(" + NDTPErrorListener.warnings.size() + "):\n");
        for (Message warning : warningList) {
            System.out.println(warning);
        }
    }

    /**
     * Was an error encountered?
     *
     * @return true if an error was seen.
     */
    public static boolean sawError() {
        return gotError;
    }

    /**
     * Was a warning encountered? This probably serves little use.
     *
     * @return true if a warning was seen.
     */
    public static boolean sawWarning() {
        return gotWarning;
    }

    /**
     * Returns an Iterator object for the errors generated during
     * parsing and semantic analysis.
     *
     * @return Iterator<Message> object.
     */
    public static Iterator<Message> getErrorIterator() {
        return errors.iterator();
    }

    /**
     * Returns an Iterator object for the warnings generated during
     * parsing and semantic analysis.
     *
     * @return Iterator<Message> object.
     */
    public static Iterator<Message> getWarningIterator() {
        return warnings.iterator();
    }

    /**
     * Since this is a static error listener, we need to reset the warnings and
     * errors each time we use this in a unit testing environment or we'll have
     * false positives.
     */
    public static void reset() {
        NDTPErrorListener.warnings.clear();
        NDTPErrorListener.errors.clear();
        NDTPErrorListener.gotError = false;
        NDTPErrorListener.gotWarning = false;
    }

    /**
     * This is the syntaxError method from the BaseErrorListener ANTLR class. We have
     * overridden it to set the error flag and add a new Message to our running set of
     * objects.
     */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int col, String errorMsg,
                            RecognitionException e) {
        // Lexer errors should always generate an unrecognized character message.
        if (recognizer instanceof Lexer) {
            Lexer lexer = (Lexer) recognizer;
            CharStream input = lexer.getInputStream();
            String offTok = lexer.getErrorDisplay(input.getText(new Interval(lexer._tokenStartCharIndex, input.index())));
            errorMsg = "Unrecognized character: '" + offTok + "'";
        } else {
            Parser parser = (Parser) recognizer;
            // First we grab the current token as well as the previous token.
            Token offTok = (Token) offendingSymbol;
            Token prevOffTok = parser.getTokenStream().get(offTok.getCharPositionInLine() - 1);
            int offTokPos = offTok.getCharPositionInLine();
            int tokId = offTok.getType();
            int prevTokId = prevOffTok.getType();
            String surroundingText = this.getSurroundingText(parser.getTokenStream().getText(), offTokPos);

            // Now check to see which type of error it is. If the offending token is a ) and the previous one is
            // either whitespace or an operator then they didn't enter an operand on the rhs.
            if (this.isCloseParenthesisToken(tokId) && (this.isWhitespaceToken(prevTokId) || this.isBinaryOpToken(prevTokId))) {
                errorMsg = "Missing operand on right-hand side at closing parenthesis ')' near " + surroundingText;
            }
            // If the offending token is a ) then there are too many.
            else if (this.isCloseParenthesisToken(tokId)) {
                errorMsg = "Extra closing ')' parentheses at " + surroundingText
                        + ". Check your formula for\t\n1. Too many closing ')' parentheses\t\n"
                        + "2. If your formula needs them/invalid characters\t\n3. If you put extra opening '(' parentheses.";
            }
            // If the PREVIOUS token is an opening parenthesis and the current offending one is a binop or a whitespace,
            // then we forgot an operand on the lhs.
            else if (this.isOpenParenthesisToken(prevTokId) && (this.isWhitespaceToken(tokId) || this.isBinaryOpToken(tokId))) {
                errorMsg = "Missing operand on left-hand side at opening parenthesis '(' near " + surroundingText;
            }
            // If the PREVIOUS token (prior to the offending one) is a binary operator, then we used one where we shouldn't have.
            else if (this.isBinaryOpToken(tokId)) {
                errorMsg = "Too many binary connectives found at " + surroundingText + ". Check your input!";
            }
            // If the offending token is EOF or a binop then we have unbalanced parentheses.
            else if (tokId == NDTPLexer.EOF) {
                errorMsg = "Unbalanced parenthesis near " + surroundingText + ". Check your input!";
            }
            // If the offending token is an atom, then we combined two atoms when we shouldn't have.
            else if (tokId == NDTPLexer.ATOM) {
                errorMsg = "Missing operator at " + surroundingText + ". Did you forget a connective (or use an invalid one)?";
            }
            // Otherwise, just throw a generic error and let them figure it out ;D
            else {
                errorMsg = "Invalid input at " + surroundingText + " (usually because of invalid characters. Check your input!)";
            }
        }

        gotError = true;
        NDTPErrorListener.errors.add(new Message(errorMsg, col + 1));
    }

    /**
     * @param _input
     * @param _offTokPos
     * @return
     */
    private String getSurroundingText(String _input, int _offTokPos) {
        StringBuilder surroundingText = new StringBuilder("'...");
        int OFFEND_TOK_OFFSET = 3;
        for (int i = _offTokPos - OFFEND_TOK_OFFSET; i <= _offTokPos + OFFEND_TOK_OFFSET; i++) {
            if (i >= 0 && i < _input.length()) {
                surroundingText.append(_input.charAt(i));
            }
        }
        surroundingText.append("...'");
        return surroundingText.toString();
    }

    private boolean isBinaryOpToken(int _tokId) {
        return _tokId == NDTPLexer.AND || _tokId == NDTPLexer.OR
                || _tokId == NDTPLexer.IMP || _tokId == NDTPLexer.BICOND || _tokId == NDTPLexer.XOR;
    }

    private boolean isWhitespaceToken(int _tokId) {
        return _tokId == NDTPLexer.WHITESPACE;
    }

    private boolean isOpenParenthesisToken(int _tokId) {
        return _tokId == NDTPLexer.OPEN_PAREN;
    }

    private boolean isCloseParenthesisToken(int _tokId) {
        return _tokId == NDTPLexer.CLOSE_PAREN;
    }

    /**
     * @author joshuacrotts
     */
    public static class Message {

        private final String text;
        private final int colNo;

        public Message(String text, int colNo) {
            this.text = text;
            this.colNo = colNo;
        }

        @Override
        public boolean equals(Object msg) {
            Message oMsg = (Message) msg;
            return this.text.equals(oMsg.text)
                    && this.colNo == oMsg.colNo;
        }

        @Override
        public int hashCode() {
            return this.text.hashCode() + this.colNo;
        }

        public int getColNo() {
            return this.colNo;
        }

        @Override
        public String toString() {
            return "Position " + this.colNo + ": " + this.text;
        }
    }
}
