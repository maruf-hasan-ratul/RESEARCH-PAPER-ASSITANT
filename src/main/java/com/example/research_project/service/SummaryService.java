package com.example.research_project.service;

import com.example.research_project.util.TextUtil;

import java.util.*;

/**
 * SummaryService.java - Generates an extractive summary.
 *
 * HOW IT WORKS (Extractive Summary):
 * We do NOT make up new sentences. We pick the BEST existing
 * sentences from the text based on how many important words they contain.
 *
 * ALGORITHM:
 *  1. Split text into sentences (by . ! ?)
 *  2. Calculate word frequency across the whole text
 *  3. Score each sentence: sum the frequency of each word in the sentence
 *  4. Pick the top N highest-scoring sentences
 *  5. Sort them back into original order (for coherence)
 *  6. Return them joined as a summary paragraph
 */
public class SummaryService {

    /**
     * Generates a summary from raw text.
     *
     * @param text      Full paper text
     * @param maxSents  Maximum sentences in the summary (e.g. 4)
     * @return          Summary string
     */
    public String generateSummary(String text, int maxSents) {
        if (text == null || text.isBlank()) return "No text available to summarise.";

        // Step 1: Split into sentences
        String[] sentences = text.split("[.!?]+");
        List<String> validSentences = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 30) { // ignore very short fragments
                validSentences.add(trimmed);
            }
        }

        if (validSentences.isEmpty()) return text.length() > 300 ? text.substring(0, 300) + "..." : text;
        if (validSentences.size() <= maxSents) {
            return String.join(". ", validSentences) + ".";
        }

        // Step 2: Build word frequency map
        Map<String, Integer> freq = TextUtil.processText(text);

        // Step 3: Score each sentence
        Map<String, Double> scores = new LinkedHashMap<>();
        for (String sentence : validSentences) {
            double score = 0;
            String[] words = sentence.toLowerCase().split("\\s+");
            for (String word : words) {
                word = word.replaceAll("[^a-zA-Z]", "");
                score += freq.getOrDefault(word, 0);
            }
            // Normalize by sentence length to avoid favouring very long sentences
            if (words.length > 0) score = score / words.length;
            scores.put(sentence, score);
        }

        // Step 4: Sort by score descending, take top N
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<String> top = new ArrayList<>();
        for (int i = 0; i < Math.min(maxSents, ranked.size()); i++) {
            top.add(ranked.get(i).getKey());
        }

        // Step 5: Sort back into original order
        top.sort(Comparator.comparingInt(validSentences::indexOf));

        return String.join(". ", top) + ".";
    }
}