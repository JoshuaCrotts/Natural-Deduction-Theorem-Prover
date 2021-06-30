package com.ndtp.models.treenode;

import com.ndtp.algorithms.TexPrinter;

/**
 *
 */
public class VariableNode extends WffTree {

    public VariableNode(String _symbol) {
        super(_symbol, NodeType.VARIABLE);
    }

    @Override
    public WffTree copy() {
        VariableNode variableNode = new VariableNode(this.getSymbol());
        variableNode.setFlags(this.getFlags());
        return variableNode;
    }

    @Override
    public String getStringRep() {
        return this.getSymbol();
    }

    @Override
    public String getTexCommand() {
        return TexPrinter.removeMathMode(this.getSymbol());
    }

    @Override
    public String getTexParseCommand() {
        return this.getTexCommand();
    }

    @Override
    public String toString() {
        return super.toString() + ": " + super.getSymbol();
    }
}
