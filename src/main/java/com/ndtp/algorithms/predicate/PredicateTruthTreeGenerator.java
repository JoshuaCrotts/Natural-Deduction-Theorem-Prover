package com.ndtp.algorithms.predicate;

import com.ndtp.algorithms.BaseTruthTreeGenerator;
import com.ndtp.algorithms.models.TruthTree;
import com.ndtp.models.treenode.ExistentialQuantifierNode;
import com.ndtp.models.treenode.NodeType;
import com.ndtp.models.treenode.UniversalQuantifierNode;
import com.ndtp.models.treenode.WffTree;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * TODO Document
 */
public final class PredicateTruthTreeGenerator extends BaseTruthTreeGenerator {

    /**
     * We should definitely make this a setting...
     */
    private static int timeout = 1000;

    public PredicateTruthTreeGenerator(WffTree _tree) {
        super(_tree);
    }

    /**
     * Builds the propositional truth tree. A truth tree is characterized by
     * branches and stacks. More information is presented in the documentation.
     *
     * @param _node - TruthTree root.
     */
    @Override
    public void buildTreeHelper(TruthTree _node) {
        int iterations = 0;
        // Initialize the min-heap and ArrayList of leaves.
        PriorityQueue<TruthTree> queue = new PriorityQueue<>();
        ArrayList<TruthTree> leaves = new ArrayList<>();

        // Add the root to these structures and compute all constants in the root.
        leaves.add(_node);
        queue.add(_node);
        this.getAllConstants(leaves);

        // Poll the heap and build the tree.
        while (!queue.isEmpty()) {
            if (++iterations >= PredicateTruthTreeGenerator.timeout) {
                System.err.println("Timeout error: cannot compute a tree this complex.");
                return;
            }

            TruthTree tree = queue.poll();
            WffTree curr = tree.getWff();
            leaves = BaseTruthTreeGenerator.getLeaves(tree);
            BaseTruthTreeGenerator.computeClosedBranches(leaves);

            // If the tree is closed then we don't need to continue...
            if (tree.isClosed()) {
                continue;
            } else if (curr.isNegation() && curr.getChild(0).isBicond()) {
                // We handle biconditional negations differently since they're harder.
                this.branchNegationBiconditional(tree, leaves, queue);
            } else if (curr.isNegation() && curr.getChild(0).isImp()) {
                // We handle a negated implication differently.
                this.stackNegationImplication(tree, leaves, queue);
            } else if (curr.isNegExclusiveOr()) {
                this.branchNegationExclusiveOr(tree, leaves, queue);
            } else if (curr.isNegation() && !curr.getChild(0).isPredicate() && !curr.getChild(0).isQuantifier() && !curr.getChild(0).isIdentity()) {
                // If the node is not a simple negation (~A) AND it's not a quantifier, negate it.
                this.distributeNegation(tree, leaves, queue);
            } else if (curr.isNegation() && tree.getWff().getChild(0).isQuantifier()) {
                // If the node is not a simple negation (~A), negate it.
                this.distributeNegationQuantifier(tree, leaves, queue);
            } else if (curr.isExistential()) {
                this.existentialDecomposition(tree, leaves, queue);
            } else if (curr.isUniversal()) {
                this.universalDecomposition(tree, leaves, queue);
            } else if (curr.isIdentity()) {
                this.identityDecomposition(tree, leaves, queue);
            } else if (curr.isAnd()) {
                this.stackConjunction(tree, leaves, queue);
            } else if (curr.isOr()) {
                this.branchDisjunction(tree, leaves, queue);
            } else if (curr.isImp()) {
                this.branchImplication(tree, leaves, queue);
            } else if (curr.isBicond()) {
                this.branchBiconditional(tree, leaves, queue);
            }
        }
    }

    /**
     * Computes existential decomposition on any arbitrary node in the tree.
     * <p>
     * Existential decomposition is defined by the replacement of a variable
     * with a constant not previously used in the tree.
     *
     * @param _existentialTruthTree - Existential node.
     * @param _leaves               - list of leaves.
     * @param _queue                - priority queue of nodes left to process.
     */
    private void existentialDecomposition(TruthTree _existentialTruthTree, ArrayList<TruthTree> _leaves, PriorityQueue<TruthTree> _queue) {
        if (_existentialTruthTree.getWff().getNodeType() != NodeType.EXISTENTIAL) {
            throw new IllegalArgumentException("Error: existential quantifier node expects existential node but got " + _existentialTruthTree.getClass());
        }

        // Add all possible constants to our list of them.
        for (TruthTree leaf : _leaves) {
            _existentialTruthTree.getAvailableConstants().addAll(leaf.getAvailableConstants());
        }

        char variableToReplace = ((ExistentialQuantifierNode) _existentialTruthTree.getWff()).getVariableSymbol().charAt(0);
        _existentialTruthTree.addExistentialConstant(_existentialTruthTree, _leaves, _queue, variableToReplace);
    }

    /**
     * Computes universal decomposition on any arbitrary node in the tree.
     * <p>
     * Universal decomposition is defined by the replacement of a quantified
     * variable in the tree by a constant PREVIOUSLY used in the tree.
     * <p>
     * Note that this method of decomposition adds all instances of previous
     * constants to the tree, which is inefficient and generally unnecessary.
     *
     * @param _universalTruthTree - Universal node.
     * @param _leaves             - list of leaves.
     * @param _queue              - priority queue of nodes left to process.
     */
    private void universalDecomposition(TruthTree _universalTruthTree, ArrayList<TruthTree> _leaves, PriorityQueue<TruthTree> _queue) {
        if (_universalTruthTree.getWff().getNodeType() != NodeType.UNIVERSAL) {
            throw new IllegalArgumentException("Error: universal quantifier node expects universal node but got " + _universalTruthTree.getClass());
        }

        // Add all possible constants to our list of them.
        for (TruthTree leaf : _leaves) {
            _universalTruthTree.getAvailableConstants().addAll(leaf.getAvailableConstants());
        }

        char variableToReplace = ((UniversalQuantifierNode) _universalTruthTree.getWff()).getVariableSymbol().charAt(0);
        _universalTruthTree.addUniversalConstant(_universalTruthTree, _leaves, _queue, variableToReplace);
    }

    /**
     * Performs an identity decomposition. An identity decomposition is an equivalence
     * that can be applied infinitely many times. For instance,
     * <p>
     * a = d
     * <p>
     * We can substitute any d for an a, and vice versa in the tree.
     *
     * @param _identityTruthTree - root of Identity subtree.
     * @param _leaves            - linked list of leaf nodes.
     * @param _queue             - priority queue of truth tree nodes left to check.
     */
    private void identityDecomposition(TruthTree _identityTruthTree, ArrayList<TruthTree> _leaves, PriorityQueue<TruthTree> _queue) {
        if (_identityTruthTree.getWff().getNodeType() != NodeType.IDENTITY) {
            throw new IllegalArgumentException("Error: identity truth tree node expects identity node but got " + _identityTruthTree.getWff().getNodeType());
        }

        // Add all possible constants to our list of them.
        for (TruthTree leaf : _leaves) {
            _identityTruthTree.getAvailableConstants().addAll(leaf.getAvailableConstants());
        }

        _identityTruthTree.addIdentityConstant(_identityTruthTree, _leaves, _queue);
    }

    /**
     * Flips the quantifier with a negation in front as follows:
     * <p>
     * ~(x)P = (Ex)~P
     * ~(Ex)P = (x)~P
     *
     * @param _negRoot - negation node itself.
     * @param _leaves  - linked list of leave nodes for this current TruthTree node.
     *                 This is computed before this method is called (and can
     *                 be abstracted to this method...)
     * @param _queue   - priority queue of nodes - each child created in this method
     *                 is added to this priority queue.
     */
    private void distributeNegationQuantifier(TruthTree _negRoot, ArrayList<TruthTree> _leaves, PriorityQueue<TruthTree> _queue) {
        WffTree negatedQuantifier = getFlippedNode(_negRoot.getWff().getChild(0));
        for (TruthTree tt : _leaves) {
            if (!tt.isClosed()) {
                tt.addCenter(new TruthTree(negatedQuantifier, tt, _negRoot));
                _queue.add(tt.getCenter());
            }
        }
    }

    /**
     * Recursively finds all constants and stores them in the constants set
     * in the TruthTree objects.
     *
     * @param _leaves - ArrayList of leaves.
     */
    private void getAllConstants(ArrayList<TruthTree> _leaves) {
        for (TruthTree leaf : _leaves) {
            this.getAllConstantsHelper(leaf);
        }
    }

    /**
     * Recursively searches through the tree from a leaf to the parent to find
     * all constants in use. A constant is a lower-case letter from a-t.
     *
     * @param _tree - TruthTree (should be a leaf node).
     */
    private void getAllConstantsHelper(TruthTree _tree) {
        TruthTree curr = _tree;
        while (curr != null) {
            String str = curr.getWff().getStringRep();
            for (int c = 0; c < str.length(); c++) {
                char ch = str.charAt(c);
                if (ch >= 'a' && ch <= 't') {
                    _tree.addConstant(ch);
                }
            }
            curr = curr.getParent();
        }
    }
}