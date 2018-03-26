package de.unidue.haring.similarity.experiments.customAnnotators;

/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;
import de.unidue.haring.similarity.experiments.utils.GeneralPipelineUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.dkpro.core.api.embeddings.binary.BinaryWordVectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper Methods for reading word embeddings from text format file and converting into binary
 * format.
 */
public class CustomTextFormatVectorizerUtils
{
    private static Set<String> usedWords;
    private static boolean tokenKnown;
    private static final Logger LOG = LoggerFactory
            .getLogger(CustomTextFormatVectorizerUtils.class);

    /**
     * Read an embeddings file in text format.
     * <p>
     * If hasHeader is set to true, the first line is expected to contain the size and
     * dimensionality of the vectors. This is typically true for files generated by Word2Vec (in
     * text format).
     *
     * @param file
     *            the input file
     * @param hasHeader
     *            if true, read size and dimensionality from the first line
     * @return a {@code Map<String, float[]>} mapping each token to a vector.
     * @throws IOException
     *             if the input file cannot be read
     * @see #readEmbeddingFileTxt(InputStream, boolean)
     */
    public static Map<String, float[]> readEmbeddingFileTxt(File file, boolean hasHeader)
        throws IOException
    {
        LOG.info("Reading embeddings from file " + file);
        InputStream is = CompressionUtils.getInputStream(file.getAbsolutePath(),
                new FileInputStream(file));

        return readEmbeddingFileTxt(is, hasHeader, false);
    }

    public static Map<String, float[]> readEmbeddingFileTxt(File file, boolean hasHeader,
            boolean usedTokens)
        throws IOException
    {
        LOG.info("Reading embeddings from file " + file);
        InputStream is = CompressionUtils.getInputStream(file.getAbsolutePath(),
                new FileInputStream(file));

        return readEmbeddingFileTxt(is, hasHeader, usedTokens);
    }

    /**
     * Read embeddings in text format from an InputStream. Each line is expected to have a
     * whitespace-separated list {@code <token> <value1> <value2> ...}.
     *
     * @param inputStream
     *            an {@link InputStream}
     * @param hasHeader
     *            if true, read size and dimensionality from the first line
     * @return a {@code Map<String, float[]>} mapping each token to a vector.
     * @throws IOException
     *             if the input file cannot be read
     */
    public static Map<String, float[]> readEmbeddingFileTxt(InputStream inputStream,
            boolean hasHeader, boolean usedTokens)
        throws IOException
    {
        tokenKnown = usedTokens;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final int dimensions;
        final int size;

        if (hasHeader) {
            String[] header = reader.readLine().split(" ");
            assert header.length == 2;
            size = Integer.parseInt(header[0]);
            dimensions = Integer.parseInt(header[1]);
        }
        else {
            dimensions = -1;
            size = -1;
        }
        Map<String, float[]> embeddings;
        if (usedTokens) {
            usedWords = GeneralPipelineUtils.getUsedWords();
            embeddings = reader.lines().map(CustomTextFormatVectorizerUtils::lineToEmbedding)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }
        else {
            embeddings = reader.lines().map(CustomTextFormatVectorizerUtils::lineToEmbedding)
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }
        reader.close();

        /* assert size and dimension */
        if (hasHeader) {
            /* check that size read matches header information */
            LOG.debug("Checking number and vector sizes for all embeddings.");
            assert size == embeddings.size();
            assert embeddings.values().stream().allMatch(vector -> dimensions == vector.length);
        }
        else {
            LOG.debug("Checking vector sizes for all embeddings.");
            int firstLength = embeddings.values().stream().findAny().get().length;
            assert embeddings.values().stream().allMatch(vector -> firstLength == vector.length);
        }

        return embeddings;
    }

    /**
     * Convert a single line in the expected format ({@code <token> <value1> ... <valueN>} int a
     * pair holding the token and the corresponding vector.
     *
     * @param line
     *            a line
     * @return a {@link Pair}
     */
    private static Pair<String, float[]> lineToEmbedding(String line)
    {
        String[] array = line.split(" ");
        int size = array.length;
        if (tokenKnown) {
            if (!usedWords.contains(array[0]))
                return null;
            double[] vector = Arrays.stream(array, 1, size).mapToDouble(Float::parseFloat)
                    .toArray();
            return Pair.of(array[0], doubleToFloatArray(vector));
        }
        else {
            double[] vector = Arrays.stream(array, 1, size).mapToDouble(Float::parseFloat)
                    .toArray();
            return Pair.of(array[0], doubleToFloatArray(vector));
        }

    }

    /**
     * Convert a double array into a float array
     *
     * @param doubles
     *            a double[]
     * @return a float[] of the same length as the input with each element casted to a float
     */
    private static float[] doubleToFloatArray(double[] doubles)
    {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }

    /**
     * Read a (compressed) Mallet embeddings file (in text format) and convert it into the binary
     * format using {@link BinaryWordVectorUtils}.
     *
     * @param malletEmbeddings
     *            a {@link File} holding embeddings in text format
     * @param targetFile
     *            the output {@link File}
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void convertMalletEmbeddingsToBinary(File malletEmbeddings, File targetFile)
        throws IOException
    {
        convertMalletEmbeddingsToBinary(malletEmbeddings, false, Locale.US, targetFile);
    }

    /**
     * Read a (compressed) Mallet embeddings file (in text format) and convert it into the binary
     * format using {@link BinaryWordVectorUtils}.
     *
     * @param malletEmbeddings
     *            a {@link File} holding embeddings in text format
     * @param aCaseless
     *            if true, all input tokens are expected to be caseless
     * @param aLocale
     *            the {@link Locale} to use
     * @param targetFile
     *            the output {@link File}
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void convertMalletEmbeddingsToBinary(File malletEmbeddings, boolean aCaseless,
            Locale aLocale, File targetFile)
        throws IOException
    {
        Map<String, float[]> embeddings = readEmbeddingFileTxt(malletEmbeddings, false);
        BinaryWordVectorUtils.convertWordVectorsToBinary(embeddings, aCaseless, aLocale,
                targetFile);
    }
}