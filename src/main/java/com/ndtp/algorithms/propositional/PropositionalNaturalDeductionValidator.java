package com.ndtp.algorithms.propositional;

import com.ndtp.algorithms.ArgumentTruthTreeValidator;
import com.ndtp.algorithms.BaseNaturalDeductionValidator;
import com.ndtp.algorithms.BaseTruthTreeGenerator;
import com.ndtp.algorithms.models.NDFlag;
import com.ndtp.algorithms.models.NDStep;
import com.ndtp.algorithms.models.NDWffTree;
import com.ndtp.algorithms.models.ProofType;
import com.ndtp.models.treenode.*;

import java.util.ArrayList;

/**
 *
 */
public final class PropositionalNaturalDeductionValidator extends BaseNaturalDeductionValidator {

    public PropositionalNaturalDeductionValidator(ArrayList<WffTree> _wffTreeList, ProofType _proofType) {
        super(_wffTreeList, _proofType);
    }

    /**
     * Computes a natural deduction proof for a propositional logic formula. We use a couple of heuristics to ensure
     * the runtime/search space isn't COMPLETELY insane (those being that we only apply certain rules if others fail
     * to produce meaningful results, etc.).
     *
     * @return list of NDWffTree "args". These serve as the premises, with the last element in the list being
     * the conclusion.
     */
    @Override
    public ArrayList<NDWffTree> getNaturalDeductionProof() {
        return null;
    }
}
