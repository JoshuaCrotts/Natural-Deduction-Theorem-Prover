package com.ndtp.algorithms.predicate;

import com.ndtp.algorithms.BaseNaturalDeductionValidator;
import com.ndtp.algorithms.BaseTruthTreeGenerator;
import com.ndtp.algorithms.models.NDFlag;
import com.ndtp.algorithms.models.NDStep;
import com.ndtp.algorithms.models.NDWffTree;
import com.ndtp.algorithms.models.ProofType;
import com.ndtp.models.treenode.*;
import com.ndtp.tools.NDTPUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public final class PredicateNaturalDeductionValidator extends BaseNaturalDeductionValidator {

    /**
     * Timeout iterator - if the number of iterations goes beyond this, we return
     * a null proof.
     */
    private static int timeout = 1000;

    /**
     * Set to keep track of all constants that are in the premises.
     */
    private final HashSet<Character> constants;

    /**
     * Set to keep track of all constants that are only in the conclusion.
     */
    private final HashSet<Character> conclusionConstants;

    public PredicateNaturalDeductionValidator(ArrayList<WffTree> _wffTreeList, ProofType _proofType) {
        super(_wffTreeList, _proofType);
        // Get all constants and conclusion constants...
        this.constants = new HashSet<>();
        this.conclusionConstants = new HashSet<>();
        for (int i = 0; i < _wffTreeList.size() - 1; i++)
            this.addAllConstantsToSet(_wffTreeList.get(i), this.constants);
        this.addAllConstantsToSet(_wffTreeList.get(_wffTreeList.size() - 1), this.conclusionConstants);
    }

    /**
     * Computes a natural deduction proof for a predicate logic formula. We use a couple of heuristics to ensure
     * the runtime/search space isn't COMPLETELY insane (those being that we only apply certain rules if others fail
     * to produce meaningful results, etc.).
     * <p>
     * Note that, for FOPL, we do NOT check to see if the argument is invalid with a truth tree first. This is because
     * it is sometimes easier to prove a formula in ND than it is to algorithmically generate a closed truth tree.
     *
     * @return list of NDWffTree "args". These serve as the premises, with the last element in the list being
     * the conclusion.
     */
    @Override
    public ArrayList<NDWffTree> getNaturalDeductionProof() {
        return null;
    }

    /**
     * @param _existentialNDWffTree
     * @param _variableToReplace
     */
    private void addExistentialConstant(NDWffTree _existentialNDWffTree, char _variableToReplace) {
        // Find the next available constant to use.
        char constant = 'a';
        while (this.constants.contains(constant) || this.conclusionConstants.contains(constant)) {
            // This could wrap around...
            constant++;
        }

        // Replace all variables found with the constant.
        WffTree _newRoot = _existentialNDWffTree.getWffTree().getChild(0).copy();
        this.replaceSymbol(_newRoot, _variableToReplace, constant, ReplaceType.CONSTANT);
        this.addPremise(new NDWffTree(_newRoot, NDFlag.EX, NDStep.EE, _existentialNDWffTree));
        this.constants.add(constant);
    }

    /**
     * @param _universalNDWffTree
     * @param _variableToReplace
     */
    private void addUniversalConstants(NDWffTree _universalNDWffTree, char _variableToReplace) {
        // Add a default constant if one is not available to the universal quantifier.
        if (this.constants.isEmpty()) {
            this.constants.add('a');
        }
        Set<Character> replaceConstants = NDTPUtils.union(this.constants, this.conclusionConstants);

        for (char c : replaceConstants) {
            // Create a copy and replace the selected variable.
            WffTree _newRoot = _universalNDWffTree.getWffTree().getChild(0).copy();
            this.replaceSymbol(_newRoot, _variableToReplace, c, ReplaceType.CONSTANT);
            this.addPremise(new NDWffTree(_newRoot, NDStep.UE, _universalNDWffTree));
        }
    }

    /**
     * Recursively adds all constants found in a WffTree to a HashSet. The constants
     * should be listed as a ConstantNode.
     *
     * @param _tree    - WffTree to recursively check.
     * @param _charSet - HashSet of characters to add the discovered constants to.
     */
    private void addAllConstantsToSet(WffTree _tree, HashSet<Character> _charSet) {
        if (_tree != null && _tree.isConstant()) {
            _charSet.add(_tree.getSymbol().charAt(0));
        }
        for (WffTree child : _tree.getChildren()) {
            this.addAllConstantsToSet(child, _charSet);
        }
    }

    /**
     * Replaces a variable or a constant with a constant node in a WffTree. This is used when performing
     * existential, universal decomposition, or identity decomposition.
     *
     * @param _newRoot         - root of WffTree to modify.
     * @param _symbolToReplace - constant or variable that we want to replace e.g. (x) = x
     * @param _symbol          - symbol to replace _symbolToReplace with.
     * @param _type            - type of node to insert to the tree. This should either be ReplaceType.CONSTANT or ReplaceType.VARIABLE.
     */
    private void replaceSymbol(WffTree _newRoot, char _symbolToReplace, char _symbol, ReplaceType _type) {
        for (int i = 0; i < _newRoot.getChildrenSize(); i++) {
            if (_newRoot.getChild(i).isVariable() || _newRoot.getChild(0).isConstant()) {
                char s = _newRoot.getChild(i).getSymbol().charAt(0);
                if (s == _symbolToReplace) {
                    if (_type == ReplaceType.CONSTANT) {
                        _newRoot.setChild(i, new ConstantNode("" + _symbol));
                    } else if (_type == ReplaceType.VARIABLE) {
                        _newRoot.setChild(i, new VariableNode("" + _symbol));
                    }
                }
            }
            this.replaceSymbol(_newRoot.getChild(i), _symbolToReplace, _symbol, _type);
        }
    }

    /**
     * ReplaceType enum for the replaceSymbols method. More details are found there.
     */
    private enum ReplaceType {
        VARIABLE,
        CONSTANT
    }
}
