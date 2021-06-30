package com.ndtp.algorithms;

import com.ndtp.algorithms.models.NDFlag;
import com.ndtp.algorithms.models.NDStep;
import com.ndtp.algorithms.models.NDWffTree;
import com.ndtp.algorithms.models.ProofType;
import com.ndtp.input.NDTPParserListener;
import com.ndtp.models.treenode.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 */
public abstract class BaseNaturalDeductionValidator implements NaturalDeductionAlgorithm {

    /**
     *
     */
    protected final ArrayList<WffTree> originalWffTreeList;

    /**
     *
     */
    protected final ArrayList<NDWffTree> originalPremisesList;

    /**
     *
     */
    protected final ArrayList<NDWffTree> premisesList;

    /**
     *
     */
    protected final NDWffTree conclusionWff;

    /**
     *
     */
    protected final ProofType proofType;

    public BaseNaturalDeductionValidator(ArrayList<WffTree> _wffTreeList, ProofType _proofType) {
        this.originalWffTreeList = _wffTreeList;
        this.proofType = _proofType;
        this.premisesList = new ArrayList<>();
        this.originalPremisesList = new ArrayList<>();
        this.conclusionWff = new NDWffTree(_wffTreeList.get(_wffTreeList.size() - 1).getNodeType() == NodeType.ROOT
                ? _wffTreeList.get(_wffTreeList.size() - 1).getChild(0)
                : _wffTreeList.get(_wffTreeList.size() - 1), NDStep.C);

        // Add all premises to the list. The invariant is that the last element is guaranteed to be the conclusion.
        for (int i = 0; i < _wffTreeList.size() - 1; i++) {
            // Trim ROOT off the node if it's still there from ANTLR processing.
            WffTree wff = _wffTreeList.get(i).getNodeType() == NodeType.ROOT
                    ? _wffTreeList.get(i).getChild(0)
                    : _wffTreeList.get(i);
            this.addPremise(new NDWffTree(wff, NDFlag.ACTIVE, NDStep.P));
        }

        // Under the hood, we want the premises to be sorted from least to most
        // complex. This will increase the likelihood of a more efficient solution.
        this.originalPremisesList.addAll(this.premisesList);
        Collections.sort(this.premisesList, new NaturalDeductionComparator());
    }

    /**
     * Computes a natural deduction proof for a logic formula. The details should be listed in the
     * subclasses for FOPL and PL respectively.
     *
     * @return list of NDWffTree "args". These serve as the premises, with the last element in the list being
     * the conclusion.
     */
    @Override
    public abstract ArrayList<NDWffTree> getNaturalDeductionProof();

    /**
     * @param _tree
     * @return NDWffTree object with _tree as its WffTree instance, null if it is not a current premise.
     */
    protected NDWffTree getPremiseNDWffTree(WffTree _tree) {
        for (NDWffTree ndWffTree : this.premisesList) {
            if (ndWffTree.getWffTree().stringEquals(_tree)) {
                return ndWffTree;
            }
        }
        return null;
    }

    /**
     * Attempts to add a premise (NDWffTree) to our running list of premises. A premise is NOT added if there is already
     * an identical premise in the tree OR it's a "redundant node". The definition for that is below.
     *
     * @TODO Fix this...
     *
     * @param _ndWffTree NDWffTree to insert as a premise.
     */
    protected void addPremise(NDWffTree _ndWffTree) {
        // THIS NEEDS TO BE ADAPTED TO WORK WITH CONTRADICTIONS SINCE THOSE WILL FAIL!!!!!!!
        if (!this.premisesList.contains(_ndWffTree)) {
            this.premisesList.add(_ndWffTree);
        }
    }

    /**
     * A tree is "redundant" if it implies a tautology. For instance, (A & A), (A V A), (A -> A), (A <-> A), etc.
     *
     * @TODO Rework this definition since tautologies may be required for proofs... e.g., A / (A V A)
     *
     * @param _ndWffTree NDWffTree to check.
     * @return true if the node is redundant, false otherwise.
     */
    protected boolean isRedundantTree(NDWffTree _ndWffTree) {
        return _ndWffTree.getWffTree().isBinaryOp()
                && _ndWffTree.getWffTree().getChild(0).stringEquals(_ndWffTree.getWffTree().getChild(1));
    }

    /**
     * @return
     */
    protected ArrayList<NDWffTree> assignParentIndices() {
        ArrayList<NDWffTree> tempArguments = new ArrayList<>();
        ArrayList<NDWffTree> arguments = new ArrayList<>();
        // First assign the trees to a temporary list.
        for (NDWffTree ndWffTree : this.premisesList) {
            if (ndWffTree.isActive()) {
                tempArguments.add(ndWffTree);
            }
        }

        // Now assign the derived parent indices.
        for (NDWffTree ndWffTree : tempArguments) {
            ArrayList<Integer> indices = new ArrayList<>();
            for (NDWffTree p : ndWffTree.getDerivedParents()) {
                int idx = tempArguments.indexOf(p);
                if (idx != -1) {
                    indices.add(idx + 1);
                }
            }
            Collections.sort(indices);
            ndWffTree.setDerivedParentIndices(indices);
            arguments.add(ndWffTree);
        }

        // Finally, add the conclusion.
        ArrayList<Integer> indices = new ArrayList<>();
        for (NDWffTree p : this.conclusionWff.getDerivedParents()) {
            int idx = tempArguments.indexOf(p);
            if (idx != -1) {
                indices.add(idx + 1);
            }
        }

        Collections.sort(indices);
        this.conclusionWff.setDerivedParentIndices(indices);
        arguments.add(this.conclusionWff);
        return arguments;
    }

    /**
     *
     */
    private static class NaturalDeductionComparator implements Comparator<NDWffTree> {
        @Override
        public int compare(NDWffTree _o1, NDWffTree _o2) {
            if (_o1.getWffTree().getNodeType() == _o2.getWffTree().getNodeType())
                return _o1.getWffTree().getStringRep().length() - _o2.getWffTree().getStringRep().length();
            return _o1.getValue() - _o2.getValue();
        }
    }
}
