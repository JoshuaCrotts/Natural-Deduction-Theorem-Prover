package com.ndtp.algorithms.models;

/**
 *
 */
public enum NDStep {

    HS("HS", "Hypothetical Syllogism", "HS"),
    MT("MT", "Modus Tollens", "MT"),
    MP("MP", "Modus Ponens", "MP"),
    II("II", "Implication Introduction", "{$\\to$}I"),
    P("Ass.", "Assumption", "Ass."),
    PRAA("Ass. for RAA", "Assumption for Reductio Ad Absurdum", "Ass. for RAA"),
    C("C", "Conclusion", ""),
    DS("DS", "Disjunctive Syllogism", "DS"),
    DNI("DNI", "Double Negation Introduction", "DNI"),
    DNE("DNE", "Double Negation Elimination", "DNE"),
    AE("&E", "Conjunction Elimination", "{$\\varland$}E"),
    AI("&I", "Conjunction Introduction", "{$\\varland$}I"),
    RI("⊥I", "Contradiction", "{$\\bot$}I"),
    RE("⊥E", "Contradiction Elimination", "{$\\bot$}E"),
    OI("∨I", "Disjunction Introduction", "{$\\lor$}I"),
    DEM("DeM", "De Morgan", "DeM"),
    BCI("↔I", "Biconditional Introduction", "{$\\varliff$}I"),
    BCE("↔E", "Biconditional Elimination", "{$\\varliff$}E"),
    MI("MI", "Material Implication", "MI"),
    EI("∃I", "Existential Introduction", "{$\\exists$}I"),
    EE("∃E", "Existential Elimination", "{$\\exists$}E"),
    UI("UI", "Universal Introduction", "UI"),
    UE("UE", "Universal Elimination", "UE"),
    CD("CD", "Constructive Dilemma", "CD"),
    DD("DD", "Destructive Dilemma", "DD"),
    TP("TP", "Transposition", "Trans.");

    /**
     *
     */
    private final String STEP;

    /**
     *
     */
    private final String TEXT_STEP;

    /**
     *
     */
    private final String TEX_CMD;

    NDStep(String _step, String _textStep, String _texCommand) {
        this.STEP = _step;
        this.TEXT_STEP = _textStep;
        this.TEX_CMD = _texCommand;
    }

    public String getTextStep() {
        return this.TEXT_STEP;
    }

    public String getTexCommand() {
        return this.TEX_CMD;
    }

    @Override
    public String toString() {
        return STEP;
    }
}