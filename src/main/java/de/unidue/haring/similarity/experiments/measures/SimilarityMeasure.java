package de.unidue.haring.similarity.experiments.measures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.unidue.haring.similarity.experiments.types.QuestionAnswerPair;
import de.unidue.haring.similarity.experiments.types.QuestionAnswerProblem;
import de.unidue.haring.similarity.experiments.types.SemanticRelatedness;
import de.unidue.haring.similarity.experiments.utils.CustomXmlReader;
import de.unidue.haring.similarity.experiments.utils.GeneralPipelineUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.mallet.type.WordEmbedding;

public class SimilarityMeasure
    extends AbstractSimilarityMeasure
{
    private static final String MEASURE_METHOD_NAME = "DefaultSimilarityMeasure";
    private static List<String> missingEmbeddingAnnotations;
    private static List<String> totalTokens;

    public SimilarityMeasure()
    {
        missingEmbeddingAnnotations = new ArrayList<String>();
        totalTokens = new ArrayList<String>();
    }

    @Override
    public QuestionAnswerProblem measureSimilarity(CAS aCas,
            QuestionAnswerProblem questionAnswerProblem)
    {
        return questionAnswerProblem;
    }

    /**
     * Sets the computed semantic relatedness to the given QuestionAnswerPairs.
     * 
     * @param relatednessName
     *            the name of the measure method
     * @param pair1
     *            the QuestionAnswerPair 1
     * @param pair2
     *            the QuestionAnswerPair 1
     * @param valuePair1
     *            the computed semantic relatedness of pair 1
     * @param valuePair2
     *            the computed semantic relatedness of pair 2
     */
    protected void setSemanticRelatedness(String relatednessName, QuestionAnswerPair pair1,
            QuestionAnswerPair pair2, double valuePair1, double valuePair2)
    {
        SemanticRelatedness semanticRelatednessPair1 = new SemanticRelatedness(relatednessName);
        SemanticRelatedness semanticRelatednessPair2 = new SemanticRelatedness(relatednessName);

        semanticRelatednessPair1.setSemanticRelatednessValue(valuePair1);
        semanticRelatednessPair2.setSemanticRelatednessValue(valuePair2);

        pair1.setRelatedness(semanticRelatednessPair1);
        pair2.setRelatedness(semanticRelatednessPair2);
    }

    /**
     * Prepares the QuestionAnswerPairs for further processing.
     * 
     * @param aCAS
     *            the current CAS
     * @param questionAnswerProblem
     *            the current QuestionAnswerProblem
     */
    public void prepareQuestionAnswerPairs(CAS aCAS, QuestionAnswerProblem questionAnswerProblem)
    {
        try {
            CAS instanceView = aCAS.getView(CustomXmlReader.INSTANCE_VIEW);
            JCas iJCas = instanceView.getJCas();
            CAS questionView = aCAS.getView(CustomXmlReader.QUESTION_VIEW);
            JCas qJCas = questionView.getJCas();
            CAS answerView1 = aCAS.getView(CustomXmlReader.ANSWER_VIEW_1);
            JCas a1JCas = answerView1.getJCas();
            CAS answerView2 = aCAS.getView(CustomXmlReader.ANSWER_VIEW_2);
            JCas a2JCas = answerView2.getJCas();

            prepareQuestionAnswerPairs(questionAnswerProblem, iJCas, qJCas, a1JCas, a2JCas);
        }
        catch (CASException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepares the QuestionAnswerPairs for further processing. Sets Lemmas for each
     * QuestionAnswerPair.
     * 
     * @param QuestionAnswerProblem
     *            the QuestionAnswerProblem
     * @param qJCas
     *            the JCas representing the QuestionView
     * @param a1JCas
     *            the JCas representing the AnswerView1
     * @param a2JCas
     *            the JCas representing the AnswerView2
     */
    private void prepareQuestionAnswerPairs(QuestionAnswerProblem questionAnswerProblem, JCas iCas,
            JCas qJCas, JCas a1JCas, JCas a2JCas)
    {
        setQuestionAnswerPairAnnotations(questionAnswerProblem.getPair1(), iCas, qJCas, a1JCas);
        setQuestionAnswerPairAnnotations(questionAnswerProblem.getPair2(), iCas, qJCas, a2JCas);
    }

    /**
     * Sets the annotations for a QuestionAnswerPair. First, the corresponding lemmas are annotated.
     * After that, the mallet embedding annotations are set.
     * 
     * @param questionAnswerPair
     *            the question
     * @param questionJcas
     * @param answerJcas
     */
    private void setQuestionAnswerPairAnnotations(QuestionAnswerPair questionAnswerPair,
            JCas instanceJCas, JCas questionJcas, JCas answerJcas)
    {
        // Sets token annotations for QuestionAnswerPair
        questionAnswerPair.setInstanceToken(getTokenList(instanceJCas));
        questionAnswerPair.setQuestionToken(getTokenList(questionJcas));
        questionAnswerPair.setAnswerToken(getTokenList(answerJcas));

        // Sets lemma annotations for QuestionAnswerPair
        questionAnswerPair.setInstanceLemmas(getLemmaList(instanceJCas));
        questionAnswerPair.setQuestionLemmas(getLemmaList(questionJcas));
        questionAnswerPair.setAnswerLemmas(getLemmaList(answerJcas));

        // Sets embedding annotations for QuestionAnswerPair
        questionAnswerPair.setInstanceLemmasEmbeddingAnnotationsList(
                getMalletEmbeddingsAnnotations(instanceJCas));
        questionAnswerPair.setQuestionLemmasEmbeddingAnnotationsList(
                getMalletEmbeddingsAnnotations(questionJcas));
        questionAnswerPair.setAnswerLemmasEmbeddingAnnotationsList(
                getMalletEmbeddingsAnnotations(answerJcas));
    }

    /**
     * Gets the mallet embedding annotations from jcas.
     * 
     * @param jCas
     *            the jcas
     * @return List containing all embedding annotations
     */
    private List<float[]> getMalletEmbeddingsAnnotations(JCas jCas)
    {
        List<float[]> embeddingsAnnotationsList = new ArrayList<float[]>();
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);
        for (Sentence s : sentences) {
            List<Token> tokenOfSentence = JCasUtil.selectCovered(jCas, Token.class, s.getBegin(),
                    s.getEnd());
            GeneralPipelineUtils.addLemmaListToUsedWordSet(tokenOfSentence);
            for (Token token : tokenOfSentence) {
                if (!totalTokens.contains(token.getCoveredText())) {
                    totalTokens.add(token.getCoveredText());
                }
                try {
                    embeddingsAnnotationsList.add(JCasUtil.selectCovered(WordEmbedding.class, token)
                            .get(0).getWordEmbedding().toArray());
                }
                catch (IndexOutOfBoundsException e) {
                    if (!missingEmbeddingAnnotations.contains(token.getCoveredText())) {
                        missingEmbeddingAnnotations.add(token.getCoveredText());
                    }
                }

            }
        }
        return embeddingsAnnotationsList;
    }

    /**
     * Gets all lemmas from jcas.
     * 
     * @param jCas
     *            the jcas
     * @return List containing all lemmas
     */
    private List<Lemma> getLemmaList(JCas jCas)
    {
        List<Lemma> lemmaList = new ArrayList<Lemma>();
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);

        for (Sentence s : sentences) {
            List<Token> tokenOfSentence = JCasUtil.selectCovered(jCas, Token.class, s.getBegin(),
                    s.getEnd());
            for (Token token : tokenOfSentence) {
                GeneralPipelineUtils.addWordToUsedWordSet(token.getLemmaValue());
                lemmaList.add(token.getLemma());
            }
        }
        return lemmaList;
    }

    /**
     * Gets tokens from each sentence in jCas.
     * 
     * @param jCas
     *            the jcas
     * @return List containing all tokens
     */
    private List<Token> getTokenList(JCas jCas)
    {
        List<Token> tokenList = new ArrayList<Token>();
        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);

        for (Sentence s : sentences) {
            List<Token> tokenOfSentence = JCasUtil.selectCovered(jCas, Token.class, s.getBegin(),
                    s.getEnd());
            for (Token token : tokenOfSentence) {
                tokenList.add(token);
            }
        }
        return tokenList;
    }

    public List<String> getTotalTokens()
    {
        return totalTokens;
    }

    public List<String> getMissingEmbeddingsAnnotation()
    {
        return missingEmbeddingAnnotations;
    }

    @Override
    public String getMeasureMethodName()
    {
        return MEASURE_METHOD_NAME;
    }
}
