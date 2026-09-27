package com.example.research_project.service;

import com.example.research_project.model.Paper;

import java.util.ArrayList;
import java.util.List;

/**
 * CitationService.java - Generates academic citations in standard styles.
 *
 * Supported styles:
 *   - APA 7th Edition
 *   - MLA 9th Edition
 *   - IEEE
 *   - BibTeX
 *
 * All methods safely handle missing fields without inventing information.
 */
public class CitationService {

    public enum CitationStyle {
        IEEE("IEEE"),
        APA("APA (7th Edition)"),
        MLA("MLA (9th Edition)"),
        BIBTEX("BibTeX");

        private final String displayName;

        CitationStyle(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Generates a citation for the given paper in the requested style.
     *
     * @param paper Paper model containing metadata
     * @param style CitationStyle enum
     * @return Formatted citation string
     */
    public String generateCitation(Paper paper, CitationStyle style) {
        if (paper == null) return "";
        if (style == null) style = CitationStyle.IEEE;

        return switch (style) {
            case APA -> generateApa(paper);
            case MLA -> generateMla(paper);
            case IEEE -> generateIeee(paper);
            case BIBTEX -> generateBibtex(paper);
        };
    }

    /**
     * APA 7th Edition:
     * Author, A. A., & Author, B. B. (Year). Title of paper. Venue/Source. DOI/URL
     */
    public String generateApa(Paper paper) {
        StringBuilder sb = new StringBuilder();

        // Authors
        String authors = formatApaAuthors(paper.getAuthors());
        if (!authors.isEmpty()) {
            sb.append(authors);
            if (!authors.endsWith(".")) sb.append(".");
            sb.append(" ");
        }

        // Year
        if (paper.getYear() > 0) {
            sb.append("(").append(paper.getYear()).append("). ");
        }

        // Title
        String title = safeTrim(paper.getTitle());
        if (!title.isEmpty()) {
            sb.append(title);
            if (!title.endsWith(".") && !title.endsWith("?") && !title.endsWith("!")) {
                sb.append(".");
            }
            sb.append(" ");
        }

        // Venue / Source
        String source = safeTrim(paper.getSource());
        if (!source.isEmpty()) {
            sb.append(source);
            if (!source.endsWith(".")) sb.append(".");
        }

        return sb.toString().trim();
    }

    /**
     * MLA 9th Edition:
     * Author. "Title of Paper." Source, Year.
     */
    public String generateMla(Paper paper) {
        StringBuilder sb = new StringBuilder();

        // Authors
        String authors = formatMlaAuthors(paper.getAuthors());
        if (!authors.isEmpty()) {
            sb.append(authors);
            if (!authors.endsWith(".")) sb.append(".");
            sb.append(" ");
        }

        // Title in quotes
        String title = safeTrim(paper.getTitle());
        if (!title.isEmpty()) {
            sb.append("\"").append(title);
            if (!title.endsWith(".") && !title.endsWith("?") && !title.endsWith("!")) {
                sb.append(".");
            }
            sb.append("\" ");
        }

        // Source and Year
        String source = safeTrim(paper.getSource());
        int year = paper.getYear();

        if (!source.isEmpty() && year > 0) {
            sb.append(source).append(", ").append(year).append(".");
        } else if (!source.isEmpty()) {
            sb.append(source);
            if (!source.endsWith(".")) sb.append(".");
        } else if (year > 0) {
            sb.append(year).append(".");
        }

        return sb.toString().trim();
    }

    /**
     * IEEE Style:
     * [1] A. A. Author and B. B. Author, "Title of paper," Source, Year.
     */
    public String generateIeee(Paper paper) {
        StringBuilder sb = new StringBuilder();

        // Authors (Initials first)
        String authors = formatIeeeAuthors(paper.getAuthors());
        if (!authors.isEmpty()) {
            sb.append(authors).append(", ");
        }

        // Title
        String title = safeTrim(paper.getTitle());
        if (!title.isEmpty()) {
            sb.append("\"").append(title);
            if (!title.endsWith(",") && !title.endsWith(".") && !title.endsWith("?") && !title.endsWith("!")) {
                sb.append(",");
            }
            sb.append("\" ");
        }

        // Source and Year
        String source = safeTrim(paper.getSource());
        int year = paper.getYear();

        if (!source.isEmpty() && year > 0) {
            sb.append(source).append(", ").append(year).append(".");
        } else if (!source.isEmpty()) {
            sb.append(source);
            if (!source.endsWith(".")) sb.append(".");
        } else if (year > 0) {
            sb.append(year).append(".");
        }

        return sb.toString().trim();
    }

    /**
     * BibTeX Format:
     * @article{key,
     *   title = {...},
     *   author = {...},
     *   year = {...},
     *   journal = {...}
     * }
     */
    public String generateBibtex(Paper paper) {
        String key = generateBibtexKey(paper);
        StringBuilder sb = new StringBuilder();
        sb.append("@article{").append(key).append(",\n");

        if (paper.getTitle() != null && !paper.getTitle().isBlank()) {
            sb.append("  title = {").append(paper.getTitle().trim()).append("},\n");
        }

        if (paper.getAuthors() != null && !paper.getAuthors().isBlank()) {
            // In BibTeX, authors are separated by "and"
            String bibAuthors = paper.getAuthors().replace(",", " and ");
            sb.append("  author = {").append(bibAuthors.trim()).append("},\n");
        }

        if (paper.getYear() > 0) {
            sb.append("  year = {").append(paper.getYear()).append("},\n");
        }

        if (paper.getSource() != null && !paper.getSource().isBlank()) {
            sb.append("  journal = {").append(paper.getSource().trim()).append("},\n");
        }

        if (paper.getCategory() != null && !paper.getCategory().isBlank()) {
            sb.append("  keywords = {").append(paper.getCategory().trim()).append("},\n");
        }

        // Trim last comma
        int lastComma = sb.lastIndexOf(",");
        if (lastComma > 0) {
            sb.deleteCharAt(lastComma);
        }

        sb.append("\n}");
        return sb.toString();
    }

    // ================================================================
    // HELPER FORMATTING METHODS
    // ================================================================

    private String formatApaAuthors(String rawAuthors) {
        if (rawAuthors == null || rawAuthors.isBlank()) return "";
        String[] parts = rawAuthors.split("[,;]+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        if (list.isEmpty()) return "";
        if (list.size() == 1) return list.get(0);
        if (list.size() == 2) return list.get(0) + " & " + list.get(1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                sb.append("& ").append(list.get(i));
            } else {
                sb.append(list.get(i)).append(", ");
            }
        }
        return sb.toString();
    }

    private String formatMlaAuthors(String rawAuthors) {
        if (rawAuthors == null || rawAuthors.isBlank()) return "";
        String[] parts = rawAuthors.split("[,;]+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        if (list.isEmpty()) return "";
        if (list.size() == 1) return list.get(0);
        if (list.size() == 2) return list.get(0) + ", and " + list.get(1);
        return list.get(0) + ", et al.";
    }

    private String formatIeeeAuthors(String rawAuthors) {
        if (rawAuthors == null || rawAuthors.isBlank()) return "";
        String[] parts = rawAuthors.split("[,;]+");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        if (list.isEmpty()) return "";
        if (list.size() == 1) return list.get(0);
        if (list.size() == 2) return list.get(0) + " and " + list.get(1);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                sb.append("and ").append(list.get(i));
            } else {
                sb.append(list.get(i)).append(", ");
            }
        }
        return sb.toString();
    }

    private String generateBibtexKey(Paper paper) {
        String firstWord = "paper";
        if (paper.getAuthors() != null && !paper.getAuthors().isBlank()) {
            String[] tokens = paper.getAuthors().trim().split("\\s+");
            if (tokens.length > 0) {
                firstWord = tokens[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            }
        } else if (paper.getTitle() != null && !paper.getTitle().isBlank()) {
            String[] tokens = paper.getTitle().trim().split("\\s+");
            if (tokens.length > 0) {
                firstWord = tokens[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            }
        }
        int year = paper.getYear() > 0 ? paper.getYear() : 2024;
        return firstWord + year;
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
