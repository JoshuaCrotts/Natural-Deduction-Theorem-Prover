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
     * Determines if we can simplify a conjunction node. A simplification is only possible when we haven't previously
     * done it to this node AND we haven't built this node from a &I rule.
     *
     * @param _andTree
     * @param _parent
     * @return true if we apply a simplification (&E) rule, false otherwise.
     */
    protected boolean findSimplification(WffTree _andTree, NDWffTree _parent) {
        if (!_parent.isAndEActive() && !_parent.isAndIActive()) {
            _parent.setFlags(NDFlag.AE);
            NDWffTree andLhs = new NDWffTree(_andTree.getChild(0), NDStep.AE, _parent);
            NDWffTree andRhs = new NDWffTree(_andTree.getChild(1), NDStep.AE, _parent);
            this.addPremise(andLhs);
            this.addPremise(andRhs);
            return true;
        }
        return false;
    }

    /**
     * Determines if we can apply a modus ponens rule to this implication. This is only possible if we haven't previously
     * applied MP or an implication introduction. We iterate through the rest of the premises to see if we have the
     * antecedent listed as a current premise, meaning it is already satisfied.
     *
     * @param _mpTree
     * @param _parent
     * @return true if we apply a modus ponens rule, false otherwise.
     */
    protected boolean findModusPonens(WffTree _mpTree, NDWffTree _parent) {
        if (!_parent.isMPActive()) {
            for (NDWffTree ndWffTree : this.premisesList) {
                // Check to see if we have the antecedent satisfied.
                if (ndWffTree.getWffTree().stringEquals(_mpTree.getChild(0))) {
                    NDWffTree consequentNode = new NDWffTree(_mpTree.getChild(1), NDStep.MP, _parent, ndWffTree);
                    this.addPremise(consequentNode);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if we can apply a modus tollens rule to this implication node. This is only possible if we have
     * not used MT previously nor used II. We iterate through to see if we have the negation of the consequent
     * satisfied.
     *
     * @param _mtTree
     * @param _parent
     * @return true if we apply a modus tollens rule, false otherwise.
     */
    protected boolean findModusTollens(WffTree _mtTree, NDWffTree _parent) {
        if (!_parent.isMTActive()) {
            for (NDWffTree ndWffTree : this.premisesList) {
                // Check to see if we have the negated consequent satisfied.
                if (_mtTree.getChild(1).stringEquals(BaseTruthTreeGenerator.getNegatedNode(ndWffTree.getWffTree()))
                        || ndWffTree.getWffTree().stringEquals(BaseTruthTreeGenerator.getNegatedNode(_mtTree.getChild(1)))) {
                    WffTree flippedWff = BaseTruthTreeGenerator.getNegatedNode(_mtTree.getChild(0));
                    NDWffTree flippedNode = new NDWffTree(flippedWff, NDStep.MT, _parent, ndWffTree);
                    this.addPremise(flippedNode);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if we can apply a disjunctive syllogism. We can apply DS twice if we have both nodes, but since this
     * generally results in a contradiction it isn't used. So, to prevent unnecessary computations, we just prevent
     * it from being used if it has already been applied twice.
     *
     * @param _disjTree
     * @param _parent
     * @return true if we apply a disjunctive syllogism rule, false otherwise.
     */
    protected boolean findDisjunctiveSyllogism(WffTree _disjTree, NDWffTree _parent) {
        if (!_parent.isDSActive() && _disjTree.stringEquals(_parent.getWffTree())) {
            WffTree flippedLhs = BaseTruthTreeGenerator.getFlippedNode(_disjTree.getChild(0));
            WffTree flippedRhs = BaseTruthTreeGenerator.getFlippedNode(_disjTree.getChild(1));
            boolean lhs = this.premisesList.contains(this.getPremiseNDWffTree(flippedLhs));
            boolean rhs = this.premisesList.contains(this.getPremiseNDWffTree(flippedRhs));
            // If we do not satisfy one of them but do satisfy the other, then we can perform DS.
            if (Boolean.logicalOr(lhs, rhs)) {
                NDWffTree ndWffTree = null;
                if (lhs) {
                    ndWffTree = new NDWffTree(_disjTree.getChild(1), NDStep.DS, _parent, this.getPremiseNDWffTree(flippedLhs));
                } else {
                    ndWffTree = new NDWffTree(_disjTree.getChild(0), NDStep.DS, _parent, this.getPremiseNDWffTree(flippedRhs));
                }

                _parent.setFlags(NDFlag.DS);
                this.addPremise(ndWffTree);
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if we can apply hypothetical syllogism. This is only possible if we have three distinct
     * implication nodes, and at NOT BOTH of the others haven't been used in a hypothetical syllogism. This is to
     * prevent rules from (A->B), (B->C) from deriving (A->C) twice, but ALLOWS rules like (A->B), (B->D) to derive
     * (A->D).
     *
     * @param _impNode
     * @param _parent
     * @return true if we apply a hypothetical syllogism rule, false otherwise.
     */
    protected boolean findHypotheticalSyllogism(WffTree _impNode, NDWffTree _parent) {
        for (NDWffTree othNdWffTree : this.premisesList) {
            WffTree othImp = othNdWffTree.getWffTree();
            if (_parent != othNdWffTree && othImp.isImp()
                    && (!_parent.isHSActive() || !othNdWffTree.isHSActive())) {
                // X == Y && Y == Z OR Y == Z && X == Y check to see if the antecedent of one
                // is equal to the consequent of the other.
                ImpNode impNode = null;
                if (_impNode.getChild(1).stringEquals(othImp.getChild(0))) {
                    impNode = new ImpNode();
                    impNode.addChild(_impNode.getChild(0));
                    impNode.addChild(othImp.getChild(1));
                } else if (othImp.getChild(1).stringEquals(_impNode.getChild(0))) {
                    impNode = new ImpNode();
                    impNode.addChild(othImp.getChild(0));
                    impNode.addChild(_impNode.getChild(1));
                }

                // If we found one, then we add it.
                if (impNode != null) {
                    _parent.setFlags(NDFlag.HS);
                    othNdWffTree.setFlags(NDFlag.HS);
                    this.addPremise(new NDWffTree(impNode, NDStep.HS, _parent, othNdWffTree));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if we can remove a biconditional operator, creating the conjunction of two implications.
     *
     * @param _bicondTree
     * @param _parent
     * @return true if we apply a biconditional elimination rule, false otherwise.
     */
    protected boolean findBiconditionalElimination(WffTree _bicondTree, NDWffTree _parent) {
        if (!_parent.isBCActive()) {
            _parent.setFlags(NDFlag.BC);
            AndNode and = new AndNode();
            ImpNode impLhs = new ImpNode();
            ImpNode impRhs = new ImpNode();
            impLhs.addChild(_bicondTree.getChild(0));
            impLhs.addChild(_bicondTree.getChild(1));
            impRhs.addChild(_bicondTree.getChild(1));
            impRhs.addChild(_bicondTree.getChild(0));
            and.addChild(impLhs);
            and.addChild(impRhs);
            this.addPremise(new NDWffTree(and, NDStep.BCE, _parent));
            return true;
        }
        return false;
    }

    /**
     * Determines if we can apply the transposition rule to an implication node. Transposition is the contrapositive
     * of an implication.
     *
     * @param _impNode
     * @param _parent
     * @return
     */
    protected boolean findTransposition(WffTree _impNode, NDWffTree _parent) {
        if (_impNode.isImp() && !_parent.isTPActive() && !this.isConclusion(_parent)) {
            NegNode antecedent = new NegNode();
            NegNode consequent = new NegNode();
            ImpNode transpositionNode = new ImpNode();
            antecedent.addChild(_impNode.getChild(1));
            consequent.addChild(_impNode.getChild(0));
            transpositionNode.addChild(antecedent);
            transpositionNode.addChild(consequent);
            _parent.setFlags(NDFlag.TP);
            this.addPremise(new NDWffTree(transpositionNode, NDFlag.TP, NDStep.TP, _parent));
            return true;
        }
        return false;
    }

    /**
     * @param _disjNode
     * @param _parent
     * @return
     */
    protected boolean findConstructiveDilemma(WffTree _disjNode, NDWffTree _parent) {
        if (_disjNode.isOr() && !_parent.isCDActive()) {
            WffTree lhs = _disjNode.getChild(0);
            WffTree rhs = _disjNode.getChild(1);
            NDWffTree lhsImp = null;
            NDWffTree rhsImp = null;
            for (NDWffTree ndWffTree : this.premisesList) {
                WffTree wff = ndWffTree.getWffTree();
                if (wff.isImp()) {
                    if (lhs.stringEquals(wff.getChild(0))) {
                        lhsImp = ndWffTree;
                    } else if (rhs.stringEquals(wff.getChild(0))) { rhsImp = ndWffTree; }
                }
            }

            // Since OR is reflexive and non-reconstructible, we need to add both.
            if (lhsImp != null && rhsImp != null) {
                _parent.setFlags(NDFlag.CD);
                // Left to right.
                OrNode orNodeLhs = new OrNode();
                orNodeLhs.addChild(lhsImp.getWffTree().getChild(1));
                orNodeLhs.addChild(rhsImp.getWffTree().getChild(1));
                // Right to left.
                OrNode orNodeRhs = new OrNode();
                orNodeRhs.addChild(rhsImp.getWffTree().getChild(1));
                orNodeRhs.addChild(lhsImp.getWffTree().getChild(1));
                this.addPremise(new NDWffTree(orNodeLhs, NDFlag.CD, NDStep.CD, lhsImp, rhsImp, _parent));
                this.addPremise(new NDWffTree(orNodeRhs, NDFlag.CD, NDStep.CD, rhsImp, lhsImp, _parent));
                return true;
            }
        }
        return false;
    }

    /**
     * @param _disjNode
     * @param _parent
     * @return
     */
    protected boolean findDestructiveDilemma(WffTree _disjNode, NDWffTree _parent) {
        if (_disjNode.isOr() && !_parent.isDDActive()) {
            WffTree lhs = _disjNode.getChild(0);
            WffTree rhs = _disjNode.getChild(1);
            NDWffTree lhsImp = null;
            NDWffTree rhsImp = null;
            for (NDWffTree ndWffTree : this.premisesList) {
                WffTree wff = ndWffTree.getWffTree();
                if (wff.isImp()) {
                    if (lhs.stringEquals(BaseTruthTreeGenerator.getFlippedNode(wff.getChild(1)))) {
                        lhsImp = ndWffTree;
                    } else if (rhs.stringEquals(BaseTruthTreeGenerator.getFlippedNode(wff.getChild(1)))) {
                        rhsImp = ndWffTree;
                    }
                }
            }

            // Since OR is reflexive and non-reconstructible, we need to add both.
            if (lhsImp != null && rhsImp != null) {
                _parent.setFlags(NDFlag.DD);
                // Left to right.
                OrNode orNodeLhs = new OrNode();
                orNodeLhs.addChild(BaseTruthTreeGenerator.getFlippedNode(lhsImp.getWffTree().getChild(0)));
                orNodeLhs.addChild(BaseTruthTreeGenerator.getFlippedNode(rhsImp.getWffTree().getChild(0)));
                // Right to left.
                OrNode orNodeRhs = new OrNode();
                orNodeRhs.addChild(BaseTruthTreeGenerator.getFlippedNode(rhsImp.getWffTree().getChild(0)));
                orNodeRhs.addChild(BaseTruthTreeGenerator.getFlippedNode(lhsImp.getWffTree().getChild(0)));
                this.addPremise(new NDWffTree(orNodeLhs, NDFlag.DD, NDStep.DD, lhsImp, rhsImp, _parent));
                this.addPremise(new NDWffTree(orNodeRhs, NDFlag.DD, NDStep.DD, rhsImp, lhsImp, _parent));
                return true;
            }
        }
        return false;
    }

    /**
     * @param _binopTree
     * @param _parent
     * @return true if we apply a De Morgan's rule, false otherwise.
     */
    protected boolean findDeMorganEquivalence(WffTree _binopTree, NDWffTree _parent) {
        if (!_parent.isDEMActive() && !this.isConclusion(_parent)) {
            WffTree deMorganNode = null;
            // Negate a biconditional to get ~(X <-> Y) => ~((X->Y) & (Y->X)).
            if (_binopTree.isNegation() && _binopTree.getChild(0).isBicond()) {
                NegNode neg = new NegNode();
                AndNode and = new AndNode();
                ImpNode lhs = new ImpNode();
                ImpNode rhs = new ImpNode();
                lhs.addChild(_binopTree.getChild(0).getChild(0));
                lhs.addChild(_binopTree.getChild(0).getChild(1));
                rhs.addChild(_binopTree.getChild(0).getChild(1));
                rhs.addChild(_binopTree.getChild(0).getChild(0));
                and.addChild(lhs);
                and.addChild(rhs);
                neg.addChild(and);
                deMorganNode = neg;
            }
            // "Unnegate" a conjunction with two implications to get the negated biconditional.
            else if (_binopTree.isNegation() && _binopTree.getChild(0).isAnd()
                    && _binopTree.getChild(0).getChild(0).isImp() && _binopTree.getChild(0).getChild(1).isImp()
                    && _binopTree.getChild(0).getChild(0).getChild(0).stringEquals(_binopTree.getChild(0).getChild(1).getChild(1))
                    && _binopTree.getChild(0).getChild(0).getChild(1).stringEquals(_binopTree.getChild(0).getChild(1).getChild(0))) {
                NegNode negNode = new NegNode();
                BicondNode bicondNode = new BicondNode();
                bicondNode.addChild(_binopTree.getChild(0).getChild(0).getChild(0));
                bicondNode.addChild(_binopTree.getChild(0).getChild(0).getChild(1));
                negNode.addChild(bicondNode);
                deMorganNode = negNode;
            }
            // Two types: one is ~(X B Y) => (~X ~B ~Y)
            else if (_binopTree.isNegation() && (_binopTree.getChild(0).isOr() || _binopTree.getChild(0).isAnd() || _binopTree.getChild(0).isImp())) {
                deMorganNode = BaseTruthTreeGenerator.getNegatedBinaryNode(_binopTree.getChild(0)); // B
                deMorganNode.addChild(_binopTree.getChild(0).isImp() ? _binopTree.getChild(0).getChild(0)
                        : BaseTruthTreeGenerator.getFlippedNode(_binopTree.getChild(0).getChild(0))); // LHS X
                deMorganNode.addChild(BaseTruthTreeGenerator.getFlippedNode(_binopTree.getChild(0).getChild(1))); // RHS Y
            }
            // Other is (X B Y) => ~(~X ~B ~Y)
            else if ((_binopTree.isOr() || _binopTree.isAnd() || _binopTree.isImp())) {
                WffTree negBinaryNode = BaseTruthTreeGenerator.getNegatedBinaryNode(_binopTree); // B
                negBinaryNode.addChild(_binopTree.isImp() ? _binopTree.getChild(0) : BaseTruthTreeGenerator.getFlippedNode(_binopTree.getChild(0))); // LHS X
                negBinaryNode.addChild(BaseTruthTreeGenerator.getFlippedNode(_binopTree.getChild(1))); // RHS Y
                deMorganNode = new NegNode();
                deMorganNode.addChild(negBinaryNode);
            }
            // If we found a node, then it'll be applied/inserted here.
            if (deMorganNode != null) {
                _parent.setFlags(NDFlag.DEM);
                this.addPremise(new NDWffTree(deMorganNode, NDFlag.DEM, NDStep.DEM, _parent));
                return true;
            }
        }
        return false;
    }

    /**
     * @param _binopNode
     * @param _parent
     * @return true if we apply a material implication rule, false otherwise.
     */
    protected boolean findMaterialImplication(WffTree _binopNode, NDWffTree _parent) {
        if (!_parent.isMIActive() && !this.isConclusion(_parent)) {
            WffTree newWff = null;
            // Convert (P -> Q) to (~P V Q).
            if (_binopNode.isImp()) {
                OrNode orNode = new OrNode();
                NegNode negLhs = new NegNode();
                negLhs.addChild(_binopNode.getChild(0));
                orNode.addChild(negLhs);
                orNode.addChild(_binopNode.getChild(1));
                newWff = orNode;
            }
            // Convert (~P V Q) to (P -> Q)
            else if (_binopNode.isOr()) {
                WffTree lhs = _binopNode.getChild(0);
                WffTree rhs = _binopNode.getChild(1);
                if (lhs.isNegation()) {
                    ImpNode impNode = new ImpNode();
                    impNode.addChild(lhs.getChild(0)); // Un-negate the lhs.
                    impNode.addChild(rhs);
                    newWff = impNode;
                }
            }
            // If we performed a MI then add it.
            if (newWff != null && this.isGoal(newWff)) {
                _parent.setFlags(NDFlag.MI);
                NDWffTree ndWffTree = new NDWffTree(newWff, NDFlag.MI, NDStep.MI, _parent);
                this.addPremise(ndWffTree);
                return true;
            }
        }
        return false;
    }

    /**
     * @param _node
     * @param _parent
     * @return
     */
    protected boolean findDoubleNegations(WffTree _node, NDWffTree _parent) {
        if (_node.stringEquals(_parent.getWffTree()) && !this.isConclusion(_parent)) {
            NDWffTree dnNDWffTree = null;
            // Double negation elimination - always possible if we haven't done it yet.
            if (_node.isDoubleNegation() && !_parent.isDNIActive()) {
                _parent.setFlags(NDFlag.DNE);
                dnNDWffTree = new NDWffTree(_node.getChild(0).getChild(0), NDFlag.DNE, NDStep.DNE, _parent);
            } else if (!_parent.isDNEActive()) {
                // // Double negation introduction (only if it's a goal! Don't add more than is necessary!).
                NegNode doubleNeg = new NegNode();
                NegNode neg = new NegNode();
                doubleNeg.addChild(neg);
                neg.addChild(_node);
                _parent.setFlags(NDFlag.DNI);
                if (this.isGoal(doubleNeg) || this.isEventualNegatedGoal(doubleNeg, 0)) {
                    dnNDWffTree = new NDWffTree(doubleNeg, NDFlag.DNI, NDStep.DNI, _parent);
                }
            }

            // If we created a node, then add it.
            if (dnNDWffTree != null) {
                this.addPremise(dnNDWffTree);
                return true;
            }
        }
        return false;
    }

    /**
     * Looks through our list of premises to determine if we have found the conclusion yet. We also assign the
     * derived parents of that node and the derivation step to the conclusion wff object (since they aren't the same
     * reference). This is used in the activateLinks method.
     *
     * @return true if the premise list has the conclusion, false otherwise.
     */
    protected boolean findConclusion() {
        for (NDWffTree ndWffTree : this.premisesList) {
            if (ndWffTree.getWffTree().stringEquals(this.conclusionWff.getWffTree())) {
                this.conclusionWff.setActive(true);
                this.conclusionWff.setDerivedParents(ndWffTree.getDerivedParents());
                this.conclusionWff.setDerivationStep(ndWffTree.getDerivationStep());
                return true;
            }
        }
        return false;
    }

    /**
     * @param _ndWffTree
     * @return true if the NDWffTree is the conclusion, false otherwise.
     */
    protected boolean isConclusion(NDWffTree _ndWffTree) {
        return this.conclusionWff.getWffTree().stringEquals(_ndWffTree.getWffTree())
                || this.conclusionWff == _ndWffTree || _ndWffTree.isAltConclusion();
    }

    /**
     * Finds contradictions in the premises. A contradiction, like with truth trees, occurs when we have a wff w1, and
     * somewhere in the premises, ~w1 also exists. This derives a contradiction and then concludes our, well, conclusion!
     * This is used in indirect proofs.
     *
     * @return true if a contradiction is found (and thus the proof is complete), false otherwise.
     */
    protected boolean findContradictions() {
        for (int i = 0; i < this.premisesList.size(); i++) {
            for (int j = i + 1; j < this.premisesList.size(); j++) {
                if (i != j) {
                    NDWffTree wffOne = this.premisesList.get(i);
                    NDWffTree wffTwo = this.premisesList.get(j);
                    // Compute the negated of one of the nodes and see if they're equivalent.
                    if (((!wffOne.getWffTree().isDoubleNegation() && !wffTwo.getWffTree().isDoubleNegation())
                            && BaseTruthTreeGenerator.getFlippedNode(wffOne.getWffTree()).stringEquals(wffTwo.getWffTree()))) {
                        NDWffTree falseNode = new NDWffTree(new FalseNode(), NDFlag.ACTIVE, NDStep.RI, wffOne, wffTwo);
                        NDWffTree conclusionNode = new NDWffTree(this.conclusionWff.getWffTree(), NDFlag.ACTIVE, NDStep.RE, falseNode);
                        // Assign this as the conclusion node.
                        this.addPremise(falseNode);
                        this.conclusionWff.setDerivationStep(conclusionNode.getDerivationStep());
                        this.conclusionWff.setDerivedParents(conclusionNode.getDerivedParents());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param _conclusionNode
     */
    protected void activateLinks(NDWffTree _conclusionNode) {
        if (_conclusionNode == null) {
            return;
        }
        _conclusionNode.setActive(true);
        for (NDWffTree ndWffTree : _conclusionNode.getDerivedParents()) {
            this.activateLinks(ndWffTree);
        }
    }

    /**
     * Attempts to add a premise (NDWffTree) to our running list of premises. A premise is NOT added if there is already
     * an identical premise in the tree OR it's a "redundant node". The definition for that is below.
     *
     * @param _ndWffTree NDWffTree to insert as a premise.
     */
    protected void addPremise(NDWffTree _ndWffTree) {
        // THIS NEEDS TO BE ADAPTED TO WORK WITH CONTRADICTIONS SINCE THOSE WILL FAIL!!!!!!!
        if (!this.premisesList.contains(_ndWffTree) && !this.isRedundantTree(_ndWffTree)) {
            this.premisesList.add(_ndWffTree);
        }
    }

    /**
     * @param _wffTree
     * @return true if _wffTree is a goal, false otherwise.
     */
    protected boolean isGoal(WffTree _wffTree) {
        for (NDWffTree ndWffTree : this.premisesList) {
            if (_wffTree.stringEquals(ndWffTree.getWffTree())) {
                return true;
            }
        }
        return _wffTree.stringEquals(this.conclusionWff.getWffTree());
    }

    /**
     * A tree is "redundant" if it implies a tautology. For instance, (A & A), (A V A), (A -> A), (A <-> A), etc.
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
     * Recursively determines if, by applying double negations, we can find a goal. This can blow up the running time if
     * MAXIMUM_NEGATED_NODES is a large value, so we use 3 since it makes no sense to have any more.
     *
     * @param _tree
     * @param maxIterations
     * @return true if we can eventually derive a goal by applying negation introductions, false otherwise.
     */
    protected boolean isEventualNegatedGoal(WffTree _tree, int maxIterations) {
        if (maxIterations > NDTPParserListener.MAXIMUM_NEGATED_NODES) return false;
        NegNode neg = new NegNode();
        neg.addChild(_tree);
        for (NDWffTree ndWffTree : this.premisesList) { if (neg.stringEquals(ndWffTree.getWffTree())) { return true; } }
        return neg.stringEquals(this.conclusionWff.getWffTree()) || this.isEventualNegatedGoal(neg, maxIterations + 1);
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
