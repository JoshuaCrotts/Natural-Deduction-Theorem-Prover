package com.ndtp.input.tests;

import com.ndtp.NDTPLexer;
import com.ndtp.NDTPParser;
import com.ndtp.algorithms.ArgumentTruthTreeValidator;
import com.ndtp.algorithms.NaturalDeductionAlgorithm;
import com.ndtp.algorithms.models.NDWffTree;
import com.ndtp.algorithms.models.ProofType;
import com.ndtp.algorithms.predicate.PredicateNaturalDeductionValidator;
import com.ndtp.algorithms.propositional.PropositionalNaturalDeductionValidator;
import com.ndtp.input.NDTPErrorListener;
import com.ndtp.input.NDTPParserAdapter;
import com.ndtp.input.NDTPParserListener;
import com.ndtp.models.treenode.WffTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;

/**
 * Basic parser tester. Has a main() so can be run from the command line, with
 * one optional command line parameter. If provided, this is a filename to use
 * for input. Otherwise, input is taken from standard input. More importantly,
 * the parseFromFile and parseFromStdin methods are public static methods and
 * can be called from automated tests. They return the
 * NDTPParserListener object that was used in parsing, giving
 * access to both the final syntax tree and the final symbol table.
 *
 * @author Steve Tate (srtate@uncg.edu)
 * @modified Joshua Crotts
 * @date 3/30/2021
 */
public class ParserTest {

    /**
     * Public static method to run the parser on an input file.
     *
     * @param fileName the name of the file to use for input
     */
    public static NDTPParserListener parseFromFile(String fileName) {
        try {
            return parseStream(CharStreams.fromFileName(fileName));
        } catch (IOException e) {
            if (e instanceof NoSuchFileException) {
                System.err.println("Could not open file " + fileName);
            } else {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Command line interface -- one argument is filename, and if omitted then input
     * is taken from standard input.
     *
     * @param argv command line arguments
     */
    public static void main(String[] argv) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        ArrayList<WffTree> resultList = NDTPParserAdapter.getAbstractSyntaxTree(reader.readLine());
        if (resultList == null) {
            return;
        }

        // If we have at least two wffs, we can see if they form a valid or invalid argument.
        if (resultList.size() >= 2) {
            // Argument validator (truth tree test).
            ArgumentTruthTreeValidator validator = new ArgumentTruthTreeValidator(resultList);
            System.out.println("Deductively valid: " + validator.isValid());
            NaturalDeductionAlgorithm ndValidator = null;
            if (resultList.get(0).isPropositionalWff()) {
                System.out.println("PL Natural Deduction:");
                ndValidator = new PropositionalNaturalDeductionValidator(resultList, ProofType.DIRECT);
            } else if (resultList.get(0).isPredicateWff()) {
                System.out.println("FOPL Natural Deduction:");
                ndValidator = new PredicateNaturalDeductionValidator(resultList, ProofType.DIRECT);
            }

            // Natural deduction prover.
            ArrayList<NDWffTree> ndArgs = ndValidator.getNaturalDeductionProof();
            if (ndArgs == null) {
                System.err.println("Either the argument is invalid (check the above result) or it timed out!");
            } else {
                for (int i = 0; i < ndArgs.size(); i++) {
                    NDWffTree wff = ndArgs.get(i);
                    System.out.println((i + 1) + ": " + wff);
                }
                System.out.println("∴ " + ndArgs.get(ndArgs.size() - 1).getWffTree().getStringRep() + "  ■");
            }
        }
    }

    /**
     * Runs the parser and syntax tree constructor for the provided input stream.
     * The returned object can be used to access the syntax tree and the symbol table
     * for either further processing or for checking results in automated tests.
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
