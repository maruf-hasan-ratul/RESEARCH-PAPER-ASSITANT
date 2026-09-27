package com.example.research_project.service;

import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;

import java.util.List;
import java.util.regex.*;

/**
 * AnalysisService.java - Coordinates a full paper analysis.
 *
 * This service orchestrates the other services:
 *   KeywordService, SummaryService, ClassificationService
 *
 * It also extracts methodology and findings from the paper text
 * using simple section detection (looking for labelled headings).
 */
public class AnalysisService {

    private final KeywordService        keywordService        = new KeywordService();
    private final SummaryService        summaryService        = new SummaryService();
    private final ClassificationService classificationService = new ClassificationService();

    /**
     * Runs a full analysis on the given paper.
     * Returns an AnalysisResult with all findings bundled together.
     *
     * @param paper  The paper to analyse
     * @return       AnalysisResult containing summary, keywords, category, etc.
     */
    public AnalysisResult analysePaper(Paper paper) {
        long start = System.currentTimeMillis();

        // Build the full text: combine all text fields
        String fullText = buildFullText(paper);

        // 1. Extract keywords (top 10)
        List<String> keywords = keywordService.extractKeywords(fullText, 10);

        // 2. Generate summary (up to 5 sentences)
        String summary = summaryService.generateSummary(fullText, 5);

        // 3. Classify topic
        String category = classificationService.classify(fullText);

        // 4. Extract methodology
        String methodology = extractSection(fullText, "methodology");
        if (methodology.isBlank() && paper.getMethodology() != null && !paper.getMethodology().isBlank()) {
            methodology = paper.getMethodology();
        }

        // 5. Extract findings
        String findings = extractSection(fullText, "findings", "results", "conclusion");
        if (findings.isBlank() && paper.getFindings() != null && !paper.getFindings().isBlank()) {
            findings = paper.getFindings();
        }

        long elapsed = System.currentTimeMillis() - start;

        // Bundle into result
        AnalysisResult result = new AnalysisResult(
            paper.getId(), summary, keywords, category, methodology, findings
        );
        result.setProcessingTimeMs(elapsed);

        return result;
    }

    /**
     * Builds one combined text string from all paper fields.
     * Having all text in one place makes it easier to analyse.
     */
    private String buildFullText(Paper paper) {
        StringBuilder sb = new StringBuilder();
        append(sb, paper.getTitle());
        append(sb, paper.getAuthors());
        append(sb, paper.getAbstractText());
        append(sb, paper.getMethodology());
        append(sb, paper.getFindings());
        return sb.toString().trim();
    }

    private void append(StringBuilder sb, String text) {
        if (text != null && !text.isBlank()) {
            sb.append(text).append(" ");
        }
    }

    /**
     * Tries to extract a section from the text by looking for
     * labelled headings (e.g. "Methodology:", "Results:", "Findings:").
     *
     * Returns an empty string if no matching section is found.
     *
     * @param text      Full paper text
     * @param labels    One or more section heading names to look for
     * @return          The section content, or empty string
     */
    private String extractSection(String text, String... labels) {
        for (String label : labels) {
            // Look for "Label:" or "Label\n" (case-insensitive)
            Pattern p = Pattern.compile(
                "(?i)" + Pattern.quote(label) + "\\s*:?\\s*([\\s\\S]{20,500}?)(?=\\n[A-Z]|$)"
            );
            Matcher m = p.matcher(text);
            if (m.find()) {
                String found = m.group(1).trim();
                if (!found.isBlank()) return found;
            }
        }
        return "";
    }
}