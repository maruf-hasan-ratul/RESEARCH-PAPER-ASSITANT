package com.example.research_project.service;

import com.example.research_project.model.Paper;
import com.example.research_project.model.SimilarityResult;

import java.util.*;

/**
 * SimilarityService.java - TF-IDF + Cosine Similarity.
 *
 * ============================================================
 * THEORY (explained simply)
 * ============================================================
 *
 * TF-IDF stands for Term Frequency - Inverse Document Frequency.
 * It gives each word a weight that says "how important is this word
 * for THIS document compared to all other documents?"
 *
 * TF  (Term Frequency):
 *   = count of word in document / total words in document
 *   A word that appears 10 times in a 100-word paper has TF = 0.10
 *
 * IDF (Inverse Document Frequency):
 *   = log(total documents / documents containing this word)
 *   A word that appears in ALL documents gets IDF = log(1) = 0
 *   (it tells us nothing unique about any document)
 *   A rare word gets a high IDF (it is distinctive)
 *
 * TF-IDF = TF * IDF
 *
 * COSINE SIMILARITY:
 * Each paper becomes a vector of TF-IDF values.
 * Words are the dimensions. Similar papers have similar vectors.
 * Cosine similarity = dot product / (|vectorA| * |vectorB|)
 * Result: 0.0 = completely different, 1.0 = identical text
 *
 * ============================================================
 * EXAMPLE
 * ============================================================
 * Paper A: "deep learning image classification CNN"
 * Paper B: "deep learning NLP transformer"
 * Paper C: "SQL database schema indexing"
 *
 * A vs B -> moderate similarity (~0.5) because "deep learning" matches
 * A vs C -> very low similarity (~0.0) - no shared important words
 */
public class SimilarityService {

    /**
     * Compares two papers using TF-IDF + cosine similarity.
     *
     * @param paperA  First paper
     * @param paperB  Second paper
     * @return        SimilarityResult with score between 0.0 and 1.0
     */
    public SimilarityResult compare(Paper paperA, Paper paperB) {
        // Build text for each paper
        String textA = buildText(paperA);
        String textB = buildText(paperB);

        // Tokenize both (simple word split after cleanup)
        List<String> tokensA = tokenize(textA);
        List<String> tokensB = tokenize(textB);

        // Build vocabulary: all unique words from both papers
        Set<String> vocab = new LinkedHashSet<>();
        vocab.addAll(tokensA);
        vocab.addAll(tokensB);

        // Calculate TF for each paper
        Map<String, Double> tfA = termFrequency(tokensA);
        Map<String, Double> tfB = termFrequency(tokensB);

        // Calculate IDF (using just these 2 documents)
        Map<String, Double> idf = inverseDocumentFrequency(vocab, List.of(tokensA, tokensB));

        // Build TF-IDF vectors
        double[] vecA = buildVector(vocab, tfA, idf);
        double[] vecB = buildVector(vocab, tfB, idf);

        // Calculate cosine similarity
        double score = cosineSimilarity(vecA, vecB);

        return new SimilarityResult(
            paperA.getId(), paperB.getId(),
            paperA.getTitle(), paperB.getTitle(),
            score
        );
    }

    // ---- Private methods ----

    private String buildText(Paper p) {
        StringBuilder sb = new StringBuilder();
        if (p.getTitle()        != null) sb.append(p.getTitle()).append(" ");
        if (p.getAbstractText() != null) sb.append(p.getAbstractText()).append(" ");
        if (p.getMethodology()  != null) sb.append(p.getMethodology()).append(" ");
        if (p.getFindings()     != null) sb.append(p.getFindings()).append(" ");
        return sb.toString().trim();
    }

    /**
     * Converts text to a list of cleaned tokens.
     * Removes punctuation, lowercases, splits by whitespace.
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        String clean = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ");
        String[] parts = clean.trim().split("\\s+");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            if (p.length() > 2) result.add(p); // skip very short words
        }
        return result;
    }

    /**
     * Calculates term frequency for each word.
     * TF(word) = count(word) / total words
     */
    private Map<String, Double> termFrequency(List<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        if (tokens.isEmpty()) return tf;
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0.0) + 1.0);
        }
        // Divide each count by total token count
        double total = tokens.size();
        for (String key : tf.keySet()) {
            tf.put(key, tf.get(key) / total);
        }
        return tf;
    }

    /**
     * Calculates inverse document frequency for each word.
     * IDF(word) = log( N / df(word) )
     * N = total documents, df(word) = how many documents contain word
     */
    private Map<String, Double> inverseDocumentFrequency(
            Set<String> vocab, List<List<String>> documents) {
        Map<String, Double> idf = new HashMap<>();
        int N = documents.size();
        for (String word : vocab) {
            int df = 0;
            for (List<String> doc : documents) {
                if (doc.contains(word)) df++;
            }
            // Add 1 to avoid log(0); add 1 to df to avoid division by 0
            idf.put(word, Math.log((double)(N + 1) / (df + 1)) + 1);
        }
        return idf;
    }

    /**
     * Builds a TF-IDF vector for one document.
     * The vector has one dimension per word in the vocabulary.
     */
    private double[] buildVector(Set<String> vocab, Map<String,Double> tf, Map<String,Double> idf) {
        double[] vector = new double[vocab.size()];
        int i = 0;
        for (String word : vocab) {
            double tfVal  = tf.getOrDefault(word, 0.0);
            double idfVal = idf.getOrDefault(word, 1.0);
            vector[i++] = tfVal * idfVal;
        }
        return vector;
    }

    /**
     * Cosine similarity between two vectors.
     * Returns a value between 0.0 (completely different) and 1.0 (identical).
     *
     * Formula: dot(A,B) / (|A| * |B|)
     */
    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, magA = 0, magB = 0;
        for (int i = 0; i < a.length; i++) {
            dot  += a[i] * b[i];
            magA += a[i] * a[i];
            magB += b[i] * b[i];
        }
        if (magA == 0 || magB == 0) return 0.0;
        double result = dot / (Math.sqrt(magA) * Math.sqrt(magB));
        return Math.min(1.0, Math.max(0.0, result)); // clamp to [0,1]
    }
}