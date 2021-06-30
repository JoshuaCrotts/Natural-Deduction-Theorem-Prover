package com.ndtp.input;

import com.ndtp.NDTPBaseListener;
import com.ndtp.NDTPParser;
import com.ndtp.models.treenode.*;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.Stack;

/**
 * NDTPParserListener is the connector that defines the syntax and,
 * more importantly, the semantic analysis - how errors are defined
 * and reported. Each method inherited from NDTPBaseListener comes
 * from the grammar NDTP.g4 file and correspond to parser rules.
 *
 * @author Joshua Crotts
 * @date 2/20/2021
 */
public class NDTPParserListener extends NDTPBaseListener {

    /**
     *
     */
    public static final int MAXIMUM_NEGATED_NODES = 4;

    /**
     * Map to keep track of nodes across the different listener
     * methods.
     */
    private final ParseTreeProperty<WffTree> PARSE_TREE;

    /**
     * NDTPParser object brought from the ParserTest.
     */
    private final NDTPParser NDTP_PARSER;

    /**
     *
     */
    private final NDTPPredicateTable predicateTable;

    /**
     * Stack to keep track of all in-progress subwffs.
     */
    private final Stack<WffTree> treeRoots;

    /**
     * ArrayList to return to the user of all WffTrees that were inputted.
     */
    private final ArrayList<WffTree> currentTrees;

    /**
     * Current root of the wff tree being constructed.
     */
    private WffTree wffTree;

    /**
     * Keeps track of how many negation nodes we currently have stacked. Any other node resets this to 0.
     * 4 is the max.
     */
    private int negationCount = 0;

    public NDTPParserListener(NDTPParser _flatParser) {
        super();

        this.NDTP_PARSER = _flatParser;
        this.PARSE_TREE = new ParseTreeProperty<>();
        this.predicateTable = new NDTPPredicateTable();
        this.treeRoots = new Stack<>();
        this.currentTrees = new ArrayList<>();
    }

    @Override
    public void enterPropositionalWff(NDTPParser.PropositionalWffContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        if (this.wffTree != null && this.wffTree.isPredicateWff()) {
            NDTPErrorListener.syntaxError(ctx, "Wff cannot be both propositional and predicate.");
            return;
        }

        this.wffTree = new WffTree();
        this.wffTree.setFlags(NodeFlag.PROPOSITIONAL);
    }

    @Override
    public void exitPropositionalWff(NDTPParser.PropositionalWffContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.currentTrees.add(this.wffTree.copy());
    }

    @Override
    public void enterAtom(NDTPParser.AtomContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        WffTree atomNode = new AtomNode(ctx.ATOM().getText());
        this.PARSE_TREE.put(ctx, atomNode);
    }

    @Override
    public void enterPropWff(NDTPParser.PropWffContext ctx) {
        if (NDTPErrorListener.sawError()) return;
    }

    @Override
    public void exitPropWff(NDTPParser.PropWffContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        if (ctx.atom() != null) {
            this.wffTree.addChild(this.PARSE_TREE.get(ctx.atom()));
        }
    }

    @Override
    public void enterPropNegRule(NDTPParser.PropNegRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.negationCount++;
        if (this.negationCount > NDTPParserListener.MAXIMUM_NEGATED_NODES) {
            NDTPErrorListener.syntaxError(ctx, "Error: cannot have more than four stacked negated nodes");
            return;
        }

        String symbol = ctx.NEG().getText();
        NegNode negNode = new NegNode(symbol);
        this.treeRoots.push(this.wffTree);
        this.wffTree = negNode;
    }

    @Override
    public void exitPropNegRule(NDTPParser.PropNegRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.negationCount--;
        this.popTreeRoot();
    }

    @Override
    public void enterPropAndRule(NDTPParser.PropAndRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        AndNode andNode = new AndNode(ctx.AND().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = andNode;
    }

    @Override
    public void exitPropAndRule(NDTPParser.PropAndRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPropOrRule(NDTPParser.PropOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        OrNode orNode = new OrNode(ctx.OR().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = orNode;
    }

    @Override
    public void exitPropOrRule(NDTPParser.PropOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPropImpRule(NDTPParser.PropImpRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        ImpNode impNode = new ImpNode(ctx.IMP().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = impNode;
    }

    @Override
    public void exitPropImpRule(NDTPParser.PropImpRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPropBicondRule(NDTPParser.PropBicondRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        BicondNode bicondNode = new BicondNode(ctx.BICOND().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = bicondNode;
    }

    @Override
    public void exitPropBicondRule(NDTPParser.PropBicondRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPropExclusiveOrRule(NDTPParser.PropExclusiveOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        ExclusiveOrNode xorNode = new ExclusiveOrNode(ctx.XOR().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = xorNode;
    }

    @Override
    public void exitPropExclusiveOrRule(NDTPParser.PropExclusiveOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

//========================== PREDICATE LOGIC LISTENERS =============================//

    @Override
    public void enterPredicateWff(NDTPParser.PredicateWffContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        if (this.wffTree != null && this.wffTree.isPropositionalWff()) {
            NDTPErrorListener.syntaxError(ctx, "Wff cannot be both propositional and predicate.");
            return;
        }

        this.wffTree = new WffTree();
        this.wffTree.setFlags(NodeFlag.PREDICATE);
    }

    @Override
    public void exitPredicateWff(NDTPParser.PredicateWffContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.currentTrees.add(this.wffTree.copy());
    }

    @Override
    public void exitPredicate(NDTPParser.PredicateContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        AtomNode atomNode = (AtomNode) this.PARSE_TREE.get(ctx.atom());

        // Loop through the children and add them to the list.
        // Each parameter is either a constant or variable.
        ArrayList<WffTree> parameters = new ArrayList<>();
        for (int i = 1; i < ctx.children.size(); i++) {
            parameters.add(this.PARSE_TREE.get(ctx.getChild(i)));
        }

        String atomLetter = atomNode.toString().replaceAll("ATOM: ", "");
        PredicateNode predicate = new PredicateNode(atomLetter, parameters);

        // Check to see if a definition exists and differs from this one. Throws a syntax error if so.
        if (predicateTable.isDifferent(predicate)) {
            NDTPErrorListener.syntaxError(ctx, "Predicate " + atomLetter + " has arity " + parameters.size()
                    + " but a previous version of " + atomLetter
                    + " has arity " + this.predicateTable.getArity(predicate) + ".");
        }

        this.wffTree.addChild(predicate);
        this.predicateTable.addPredicate(predicate);
    }

    @Override
    public void enterConstant(NDTPParser.ConstantContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        WffTree constantNode = new ConstantNode(ctx.CONSTANT().getText());
        this.PARSE_TREE.put(ctx, constantNode);
    }

    @Override
    public void enterVariable(NDTPParser.VariableContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        WffTree variableNode = new VariableNode(ctx.VARIABLE().getText());
        this.PARSE_TREE.put(ctx, variableNode);
    }

    @Override
    public void exitPredQuantifier(NDTPParser.PredQuantifierContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void exitUniversal(NDTPParser.UniversalContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        VariableNode variableNode = null;

        if (ctx.variable() != null) {
            variableNode = (VariableNode) this.PARSE_TREE.get(ctx.variable());
        } else {
            NDTPErrorListener.syntaxError(ctx, "Cannot use non-variable " + ctx.constant().getText() + " as variable in universal quantifier.");
            return;
        }

        // Since we don't HAVE to use the "forall" symbol, we need to check if it's null.
        String symbol = "(" + (ctx.UNIVERSAL() != null ? ctx.UNIVERSAL().getText() : "")
                + (ctx.variable().getText()) + ")";

        UniversalQuantifierNode uqn = new UniversalQuantifierNode(symbol, variableNode.getSymbol());
        this.treeRoots.push(this.wffTree);
        this.wffTree = uqn;
    }

    @Override
    public void exitExistential(NDTPParser.ExistentialContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        VariableNode variableNode = null;

        if (ctx.variable() != null) {
            variableNode = (VariableNode) this.PARSE_TREE.get(ctx.variable());
        } else {
            NDTPErrorListener.syntaxError(ctx, "Cannot use non-variable " + ctx.constant().getText() + " as variable in existential quantifier.");
            return;
        }

        String symbol = "(" + ctx.EXISTENTIAL().getText() + ctx.variable().getText() + ")";
        ExistentialQuantifierNode uqn = new ExistentialQuantifierNode(symbol, variableNode.getSymbol());
        this.treeRoots.push(this.wffTree);
        this.wffTree = uqn;
    }

    @Override
    public void enterPredNegRule(NDTPParser.PredNegRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.negationCount++;

        NegNode negNode = new NegNode(ctx.NEG().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = negNode;
    }

    @Override
    public void exitPredNegRule(NDTPParser.PredNegRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        if (this.negationCount > NDTPParserListener.MAXIMUM_NEGATED_NODES) {
            NDTPErrorListener.syntaxError(ctx, "Error: cannot have more than four stacked negated nodes");
            return;
        }

        this.negationCount--;
        this.popTreeRoot();
    }

    @Override
    public void enterPredAndRule(NDTPParser.PredAndRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        AndNode andNode = new AndNode(ctx.AND().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = andNode;
    }

    @Override
    public void exitPredAndRule(NDTPParser.PredAndRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPredOrRule(NDTPParser.PredOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        OrNode orNode = new OrNode(ctx.OR().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = orNode;
    }

    @Override
    public void exitPredOrRule(NDTPParser.PredOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPredImpRule(NDTPParser.PredImpRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        ImpNode impNode = new ImpNode(ctx.IMP().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = impNode;
    }

    @Override
    public void exitPredImpRule(NDTPParser.PredImpRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPredBicondRule(NDTPParser.PredBicondRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        BicondNode bicondNode = new BicondNode(ctx.BICOND().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = bicondNode;
    }

    @Override
    public void exitPredBicondRule(NDTPParser.PredBicondRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void enterPredExclusiveOrRule(NDTPParser.PredExclusiveOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        ExclusiveOrNode xorNode = new ExclusiveOrNode(ctx.XOR().getText());
        this.treeRoots.push(this.wffTree);
        this.wffTree = xorNode;
    }

    @Override
    public void exitPredExclusiveOrRule(NDTPParser.PredExclusiveOrRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        this.popTreeRoot();
    }

    @Override
    public void exitPredIdentityRule(NDTPParser.PredIdentityRuleContext ctx) {
        if (NDTPErrorListener.sawError()) return;
        IdentityNode identityNode = new IdentityNode();
        identityNode.addChild(this.PARSE_TREE.get(ctx.getChild(0)));
        identityNode.addChild(this.PARSE_TREE.get(ctx.getChild(2)));

        this.wffTree.addChild(identityNode);
    }

    /**
     * Returns the list of WffTrees that were constructed during parsing. Multiple are
     * possible if a comma is used as the delimiter.
     *
     * @return ArrayList of WffTrees. If there is only one, then only one WffTree should
     * be evaluated. Otherwise, use an algorithm for 2+.
     */
    public ArrayList<WffTree> getSyntaxTrees() {
        return NDTPErrorListener.sawError() ? null : this.currentTrees;
    }

    /**
     * Pops the root of the tree - each time a node with a potentially
     * left-recursive child is called (namely wff), we need to start adding
     * onto that specific WffTree. So, we save the old root into a stack,
     * and continue to add onto the new running "root". When we finish, we pop the
     * stack, add the running root as a child of the old root, and finally
     * reassign the links.
     */
    private void popTreeRoot() {
        WffTree oldRoot = this.treeRoots.pop(); // Remove the old root from the stack.
        oldRoot.addChild(this.wffTree); // Add the current running-node as a child of the old root.
        this.wffTree = oldRoot; // Reassign the root to be the old one.
    }
}