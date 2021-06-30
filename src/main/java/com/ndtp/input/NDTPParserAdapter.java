package com.ndtp.input;

import com.ndtp.NDTPLexer;
import com.ndtp.NDTPParser;
import com.ndtp.models.treenode.WffTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;

public class NDTPParserAdapter {

    /**
     * Builds the abstract syntax tree(s) from the user input string. This
     * method should be called by any class that sends input from a front-end
     * and wants to create an AST, whether it be stdin or JavaFX.
     *
     * @param _wff - String of wff characters.
     * @return ArrayList<WffTree> representing abstract syntax trees returned. If
     * this list contains only one WffTree, then we can run most algorithms.
     */
    public static ArrayList<WffTree> getAbstractSyntaxTree(String _wff) {
        NDTPErrorListener.reset();
        CharStream charStream = CharStreams.fromString(_wff);
        NDTPParserListener parser = NDTPParserAdapter.parseStream(charStream);
        // For now, the errors are just printed in the tester class - if
        // JUnit is integrated, these should be removed so they align with the tests.
        NDTPErrorListener.printErrors();
        NDTPErrorListener.printWarnings();
        return parser.getSyntaxTrees();
    }

    /**
     * Runs the parser and syntax tree constructor for the provided input stream.
     * The returned object can be used to access the syntax tree for either further
     * processing or for checking results in automated tests.
     *
     * @param input an initialized CharStream
     */
    private static NDTPParserListener parseStream(CharStream input) {
        // "input" is the character-by-character input - connect to lexer
        NDTPLexer lexer = new NDTPLexer(input);
        NDTPErrorListener errorListener = new NDTPErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        // Connect token stream to lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Connect parser to token stream
        NDTPParser parser = new NDTPParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        ParseTree tree = parser.program();

        // Now do the parsing, and walk the parse tree with our listeners
        ParseTreeWalker walker = new ParseTreeWalker();
        NDTPParserListener compiler = new NDTPParserListener(parser);
        walker.walk(compiler, tree);

        return compiler;
    }
}
