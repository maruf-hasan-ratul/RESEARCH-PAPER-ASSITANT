package com.example.research_project.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * TextUtil.java - Utility class for basic text processing.
 *
 * WHAT DOES IT DO?
 * Prepares raw text for analysis by cleaning it up.
 * This is essential before keyword extraction or classification.
 *
 * STEPS:
 *   1. Lowercase:      "Deep Learning" -> "deep learning"
 *   2. Remove punct.:  "CNN, model." -> "CNN model"
 *   3. Tokenize:       "deep learning" -> ["deep", "learning"]
 *   4. Remove stops:   removes "the", "is", "a", "and", etc.
 *   5. Word frequency: counts how many times each word appears
 *
 * All methods are static - no need to create a TextUtil object.
 *
 * CURRENT STATUS: Stub - full implementation in Step 13.
 */
public class TextUtil {

    // Prevent instantiation
    private TextUtil() {}

    // Common English stop words (words that carry little meaning)
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the","a","an","and","or","but","in","on","at","to","for",
        "of","with","by","from","is","are","was","were","be","been",
        "has","have","had","do","does","did","not","this","that",
        "it","its","we","our","they","their","as","if","so","than",
        "more","also","which","can","may","will","would","could",
        "should","about","into","through","between","each","other"
    ));

    /**
     * Converts text to lowercase.
     */
    public static String toLowerCase(String text) {
        if (text == null) return "";
        return text.toLowerCase();
    }

    /**
     * Removes punctuation from text, leaving only letters, digits, spaces.
     */
    public static String removePunctuation(String text) {
        if (text == null) return "";
        return text.replaceAll("[^a-zA-Z0-9\\s]", " ");
    }

    /**
     * Splits text into individual words (tokens).
     *
     * @param text   Cleaned text (lowercase, no punctuation)
     * @return       List of word tokens
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.asList(text.trim().split("\\s+"));
    }

    /**
     * Removes stop words from a list of tokens.
     *
     * @param tokens  List of words
     * @return        Filtered list without stop words
     */
    public static List<String> removeStopWords(List<String> tokens) {
        // TODO: refine stop word list in Step 13
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (!STOP_WORDS.contains(token) && token.length() > 2) {
                result.add(token);
            }
        }
        return result;
    }

    /**
     * Counts how many times each word appears.
     *
     * @param tokens  List of words (after stop word removal)
     * @return        Map of word -> count, sorted by count descending
     */
    public static Map<String, Integer> wordFrequency(List<String> tokens) {
        // TODO: implement sorted frequency map in Step 13
        Map<String, Integer> freq = new HashMap<>();
        for (String token : tokens) {
            freq.put(token, freq.getOrDefault(token, 0) + 1);
        }
        return freq;
    }

    /**
     * Convenience method: runs all steps on raw text and returns
     * a frequency map ready for keyword extraction.
     *
     * @param rawText   Raw text from a paper
     * @return          Word frequency map
     */
    public static Map<String, Integer> processText(String rawText) {
        String lower = toLowerCase(rawText);
        String clean = removePunctuation(lower);
        List<String> tokens = tokenize(clean);
        List<String> filtered = removeStopWords(tokens);
        return wordFrequency(filtered);
    }
}