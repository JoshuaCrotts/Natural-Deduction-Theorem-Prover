package com.ndtp.input.tests;

import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

public class SimpleNLPTest {
    public static void main(String[] args) {
        Lexicon lexicon = Lexicon.getDefaultLexicon();
        NLGFactory nlgFactory = new NLGFactory(lexicon);
        Realiser realiser = new Realiser(lexicon);
        NPPhraseSpec subject = nlgFactory.createNounPhrase("philosophers");
        subject.setPlural(true);
        subject.setPreModifier("if");
        VPPhraseSpec verb = nlgFactory.createVerbPhrase("is");
        verb.addModifier("smart");
        SPhraseSpec p = nlgFactory.createClause();
        verb.setNegated(true);
        p.setSubject(subject);
        p.setVerb(verb);
        String output = realiser.realiseSentence(p);

        System.out.println(output);
    }
}
