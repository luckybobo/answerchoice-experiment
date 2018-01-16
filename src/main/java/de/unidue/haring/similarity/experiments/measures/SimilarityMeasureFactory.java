package de.unidue.haring.similarity.experiments.measures;

import java.util.ArrayList;
import java.util.List;

public class SimilarityMeasureFactory
{
    private ArrayList<SimilarityMeasure> similarityMeasureMethods;

    private enum MeasureMethod
    {
        RANDOM
    };

    public ArrayList<SimilarityMeasure> initializeSimilarityMeasureMethods()
    {
        similarityMeasureMethods = new ArrayList<SimilarityMeasure>();
        SimilarityMeasure sm = null;

        for (MeasureMethod measureMethod : MeasureMethod.values()) {
            sm = null;
            switch (measureMethod) {
            case RANDOM:
                sm = new RandomSimilarityMeasure();
                similarityMeasureMethods.add(sm);
                break;
            default:
                System.out.println("No similarity measure method found");
            }
        }

        return similarityMeasureMethods;
    }
}