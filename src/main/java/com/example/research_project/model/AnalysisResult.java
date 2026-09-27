package com.example.research_project.model;
import java.util.List;

/**
 * AnalysisResult.java - Holds the output of analysing a paper.
 *
 * WHAT IS IT?
 * When the user clicks "Analyse", our services run several algorithms
 * on the paper text:
 *   - KeywordService   extracts keywords
 *   - SummaryService   generates a summary
 *   - ClassificationService determines the topic category
 *   - AnalysisService  extracts methodology and findings
 *
 * All of those results are bundled into this single object,
 * which is then passed back to the controller to display on screen.
 *
 * This class is also used by AnalysisTask (the background thread).
 *
 * FLOW:
 *   AnalysisTask (background thread)
 *       |
 *       | runs services
 *       |
 *       v
 *   AnalysisResult (this class) <-- all results stored here
 *       |
 *       v
 *   AnalysisController (displays on JavaFX screen)
 */
public class AnalysisResult {

    // The paper that was analysed
    private int paperId;

    // Generated summary of the paper
    private String summary;

    // List of top keywords extracted from the text
    private List<String> keywords;

    // Topic category (e.g. "Machine Learning", "Computer Vision")
    private String category;

    // Methodology extracted from the text
    private String methodology;

    // Findings extracted from the text
    private String findings;

    // Optional: how confident the algorithm is (0.0 to 1.0)
    private double confidence;

    // Optional: how long the analysis took in milliseconds
    private long processingTimeMs;

    // ---- Constructors ----

    public AnalysisResult() {}

    public AnalysisResult(int paperId, String summary, List<String> keywords,
                          String category, String methodology, String findings) {
        this.paperId = paperId;
        this.summary = summary;
        this.keywords = keywords;
        this.category = category;
        this.methodology = methodology;
        this.findings = findings;
    }

    // ---- Getters and Setters ----

    public int getPaperId() { return paperId; }
    public void setPaperId(int paperId) { this.paperId = paperId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMethodology() { return methodology; }
    public void setMethodology(String methodology) { this.methodology = methodology; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "paperId=" + paperId +
                ", category='" + category + "'" +
                ", keywords=" + keywords +
                ", processingTimeMs=" + processingTimeMs +
                "}";
    }
}
