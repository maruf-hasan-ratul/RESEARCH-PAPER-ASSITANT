package com.example.research_project.service;

import com.example.research_project.util.TextUtil;

import java.util.*;

/**
 * KeywordService.java - Extracts the most important keywords from text.
 *
 * ALGORITHM:
 *  1. Preprocess: lowercase, remove punctuation, tokenize, remove stop words
 *  2. Count word frequency using TextUtil
 *  3. Sort words by frequency (most frequent first)
 *  4. Return top N words
 */
public class KeywordService {

    /**
     * Extracts the top N keywords from the given text.
     *
     * @param text  Raw paper text (title + abstract + methodology + findings combined)
     * @param topN  How many keywords to return
     * @return      List of keyword strings, ordered by importance (highest first)
     */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.isBlank()) return List.of();

        // Step 1: Process text (lowercase -> remove punct -> tokenize -> remove stops -> frequency)
        Map<String, Integer> freq = TextUtil.processText(text);

        if (freq.isEmpty()) return List.of();

        // Step 2: Sort entries by frequency descending
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(freq.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue()); // highest count first

        // Step 3: Take the top N
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            keywords.add(sorted.get(i).getKey());
        }

        return keywords;
    }
}