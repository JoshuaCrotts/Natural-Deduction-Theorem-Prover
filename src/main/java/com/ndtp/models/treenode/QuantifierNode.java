package com.ndtp.models.treenode;

public abstract class QuantifierNode extends WffTree {

    /**
     * Symbol used to define the variable that is being quantified by the universal quantifier.
     */
    private String variableSymbol;

    /**
     *
     */
    private boolean vacuous;

    public QuantifierNode(String _symbol, String _variableSymbol, NodeType _nodeType) {
        super(_symbol, _nodeType);
        if (_variableSymbol.length() != 1) {
            throw new IllegalArgumentException("Variable for quantifier can only be one character long.");
        }
        this.variableSymbol = _variableSymbol;
        this.vacuous = true;
    }

    public String getVariableSymbol() {
        return this.variableSymbol;
    }

    public void setVariableSymbol(String _s) {
        this.variableSymbol = _s;
    }

    public char getVariableSymbolChar() {
        return this.variableSymbol.charAt(0);
    }

    public boolean isVacuous() {
        return this.vacuous;
    }

    public void setVacuous(boolean _vacuous) {
        this.vacuous = _vacuous;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + this.variableSymbol;
    }
}
