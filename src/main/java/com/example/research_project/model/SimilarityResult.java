package com.example.research_project.model;

/**
 * SimilarityResult.java - Holds the output of comparing two papers.
 *
 * WHAT IS IT?
 * When the user selects two papers and clicks "Compare",
 * SimilarityService calculates how similar they are using
 * TF-IDF and cosine similarity.
 *
 * The result (a number between 0.0 and 1.0) is stored here
 * along with labels for both papers so the UI can display:
 *
 *   Paper 1: Deep Learning for Medical Imaging
 *   Paper 2: CNN Image Classification
 *   Similarity: 67.43%
 *
 * NOTE: Similarity is purely textual, not scientific equivalence.
 */
public class SimilarityResult {

    private int paperIdA;      // ID of the first paper
    private int paperIdB;      // ID of the second paper
    private String titleA;     // Title of the first paper
    private String titleB;     // Title of the second paper
    private double score;      // Similarity score: 0.0 (no match) to 1.0 (identical)

    // ---- Constructors ----

    public SimilarityResult() {}

    public SimilarityResult(int paperIdA, int paperIdB,
                            String titleA, String titleB, double score) {
        this.paperIdA = paperIdA;
        this.paperIdB = paperIdB;
        this.titleA = titleA;
        this.titleB = titleB;
        this.score = score;
    }

    // ---- Getters and Setters ----

    public int getPaperIdA() { return paperIdA; }
    public void setPaperIdA(int paperIdA) { this.paperIdA = paperIdA; }

    public int getPaperIdB() { return paperIdB; }
    public void setPaperIdB(int paperIdB) { this.paperIdB = paperIdB; }

    public String getTitleA() { return titleA; }
    public void setTitleA(String titleA) { this.titleA = titleA; }

    public String getTitleB() { return titleB; }
    public void setTitleB(String titleB) { this.titleB = titleB; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    /**
     * Returns the score as a readable percentage string.
     * Example: score=0.6743 -> "67.43%"
     */
    public String getScoreAsPercent() {
        return String.format("%.2f%%", score * 100);
    }

    @Override
    public String toString() {
        return "SimilarityResult{" +
                "titleA='" + titleA + "'" +
                ", titleB='" + titleB + "'" +
                ", score=" + getScoreAsPercent() +
                "}";
    }
}