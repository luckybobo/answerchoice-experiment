package de.unidue.haring.similarity.experiments.measures;

import java.io.IOException;

import org.apache.uima.cas.CAS;

import de.unidue.haring.similarity.experiments.types.QuestionAnswerPair;
import de.unidue.haring.similarity.experiments.types.QuestionAnswerProblem;

public class ConceptualJWeb1TMeasure
    extends JWeb1TMeasure
{
    private static final String MEASURE_METHOD_NAME = "ConceptualJWeb1TMeasure";
    private QuestionAnswerPair questionAnswerPair1;
    private QuestionAnswerPair questionAnswerPair2;
    
    private String questionText;

    public ConceptualJWeb1TMeasure() {
        super();
    }
    
    @Override
    public QuestionAnswerProblem measureSimilarity(CAS aCAS,
            QuestionAnswerProblem questionAnswerProblem)
    {
        questionAnswerPair1 = questionAnswerProblem.getPair1();
        questionAnswerPair2 = questionAnswerProblem.getPair2();

        if (web1t == null) {
            initJWeb1T();
        }
        questionText = questionAnswerProblem.getQuestionText().toLowerCase();

        if (questionText.startsWith("when")) {
            useInstanzeToAnswerPrediction();
        }
        else if (questionText.startsWith("how")) {
            useSimpleWeb1TPrediction();
        }
        else if (questionText.startsWith("what")) {
            useInstanzeToAnswerPrediction();
        }
        else if (questionText.startsWith("which")) {
            useInstanzeToAnswerPrediction();
        }
        else if (questionText.startsWith("where")) {
            useInstanzeToAnswerPrediction();
        }
        else if (questionText.startsWith("why")) {
            useInstanzeToAnswerPrediction();
        }
        else if (questionText.startsWith("who")) {
            useSimpleWeb1TPrediction();
        }
        else if (questionText.startsWith("whose")) {
            useSimpleWeb1TPrediction();
        }else {
            useInstanzeToAnswerPrediction();
        }

        return questionAnswerProblem;
    }

    private void useInstanzeToAnswerPrediction()
    {
        double cosineSimPair1 = computeCosineSimilarity(
                questionAnswerPair1.getInstanceLemmasEmbeddingAnnotationsList(),
                questionAnswerPair1.getAnswerLemmasEmbeddingAnnotationsList());
        double cosineSimPair2 = computeCosineSimilarity(
                questionAnswerPair2.getInstanceLemmasEmbeddingAnnotationsList(),
                questionAnswerPair2.getAnswerLemmasEmbeddingAnnotationsList());

        setSemanticRelatedness(MEASURE_METHOD_NAME, questionAnswerPair1, questionAnswerPair2,
                cosineSimPair1, cosineSimPair2);
    }

    private void useSimpleWeb1TPrediction()
    {
        try {
            double a1Freq = sumLemmataFrequency(questionAnswerPair1.getAnswerLemmas());
            double a2Freq = sumLemmataFrequency(questionAnswerPair2.getAnswerLemmas());

            setSemanticRelatedness(MEASURE_METHOD_NAME, questionAnswerPair1, questionAnswerPair2,
                    a1Freq, a2Freq);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getMeasureMethodName()
    {
        return MEASURE_METHOD_NAME;
    }
}
