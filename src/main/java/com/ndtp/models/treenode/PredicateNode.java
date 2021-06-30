package com.ndtp.models.treenode;

import com.ndtp.algorithms.TexPrinter;

import java.util.ArrayList;

/**
 *
 */
public class PredicateNode extends WffTree {

    /**
     * Symbol to define the predicate letter.
     */
    private final String PREDICATE_LETTER;

    /**
     * WffTree children for this predicate node - each child should be a constant or variable node.
     */
    private final ArrayList<WffTree> PARAMS;

    public PredicateNode(String _predicateLetter, ArrayList<WffTree> _params) {
        super(_predicateLetter, NodeType.PREDICATE);
        this.PREDICATE_LETTER = _predicateLetter;
        this.PARAMS = _params;

        for (WffTree tree : this.PARAMS) {
            if (tree != null) {
                super.addChild(tree);
            }
        }
    }

    public PredicateNode(String _predicateLetter) {
        super(_predicateLetter, NodeType.PREDICATE);
        this.PREDICATE_LETTER = _predicateLetter;
        this.PARAMS = new ArrayList<>();
    }

    @Override
    public WffTree copy() {
        PredicateNode predicateCopy = new PredicateNode(this.PREDICATE_LETTER);
        predicateCopy.setFlags(this.getFlags());
        for (WffTree ch : this.getChildren()) {
            predicateCopy.addChild(ch.copy());
        }

        return predicateCopy;
    }

    @Override
    public String getStringRep() {
        StringBuilder sb = new StringBuilder(this.PREDICATE_LETTER);
        for (WffTree ch : this.getChildren()) {
            sb.append(ch.getStringRep());
        }
        return sb.toString();
    }

    @Override
    public String getTexCommand() {
        StringBuilder sb = new StringBuilder(TexPrinter.removeMathMode(this.PREDICATE_LETTER));
        for (WffTree ch : this.getChildren()) {
            sb.append(ch.getTexCommand());
        }
        return sb.toString();
    }

    @Override
    public String getTexParseCommand() {
        return this.getTexCommand();
    }

    public String getPredicateLetter() {
        return this.PREDICATE_LETTER;
    }

    public ArrayList<WffTree> getParameters() {
        return this.PARAMS;
    }

    public int getArity() {
        return this.PARAMS.size();
    }

    @Override
    public String toString() {
        return this.getNodeType() + ": " + this.PREDICATE_LETTER;
    }
}
