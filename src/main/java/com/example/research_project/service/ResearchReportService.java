package com.example.research_project.service;

import com.example.research_project.model.Paper;
import com.example.research_project.model.SimilarityResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ResearchReportService.java - Generates structured research analysis reports
 * and exports them as clean PDF documents using Apache PDFBox 3.x.
 */
public class ResearchReportService {

    private final PaperService paperService;
    private final SimilarityService similarityService;
    private final CitationService citationService;

    public ResearchReportService() {
        this.paperService = new PaperService();
        this.similarityService = new SimilarityService();
        this.citationService = new CitationService();
    }

    public ResearchReportService(PaperService paperService, SimilarityService similarityService, CitationService citationService) {
        this.paperService = paperService;
        this.similarityService = similarityService;
        this.citationService = citationService;
    }

    /**
     * Generates a plain-text formatted summary of the research report for UI display.
     */
    public String generateTextReport(List<Paper> papers) {
        if (papers == null || papers.isEmpty()) {
            return "No papers selected for analysis.";
        }

        StringBuilder sb = new StringBuilder();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("=======================================================================\n");
        sb.append("                       AI RESEARCH ANALYSIS REPORT                     \n");
        sb.append("=======================================================================\n");
        sb.append("Generated Date: ").append(dateStr).append("\n");
        sb.append("Number of Papers Analyzed: ").append(papers.size()).append("\n\n");

        sb.append("-----------------------------------------------------------------------\n");
        sb.append("1. SELECTED PAPERS DETAIL\n");
        sb.append("-----------------------------------------------------------------------\n\n");

        for (int i = 0; i < papers.size(); i++) {
            Paper p = papers.get(i);
            sb.append("[").append(i + 1).append("] ").append(p.getTitle()).append("\n");
            sb.append("    Authors:        ").append(nvl(p.getAuthors())).append("\n");
            sb.append("    Year:           ").append(p.getYear() > 0 ? String.valueOf(p.getYear()) : "N/A").append("\n");
            sb.append("    Category:       ").append(nvl(p.getCategory())).append("\n");
            sb.append("    Reading Status: ").append(p.getReadingStatus()).append("\n");
            sb.append("    Favorite:       ").append(p.isFavorite() ? "Yes ★" : "No").append("\n");

            List<String> kws = paperService.getKeywordsForPaper(p.getId());
            sb.append("    Keywords:       ").append(kws.isEmpty() ? "(none)" : String.join(", ", kws)).append("\n");

            String abs = p.getAbstractText();
            if (abs != null && !abs.isBlank()) {
                String trimmedAbs = abs.length() > 300 ? abs.substring(0, 300) + "..." : abs;
                sb.append("    Abstract:       ").append(trimmedAbs.replaceAll("\\r?\\n", " ")).append("\n");
            }
            sb.append("\n");
        }

        // 2. Common Keywords
        sb.append("-----------------------------------------------------------------------\n");
        sb.append("2. COMMON & DOMINANT KEYWORDS\n");
        sb.append("-----------------------------------------------------------------------\n");
        Map<String, Integer> kwCounts = getAggregatedKeywords(papers);
        if (kwCounts.isEmpty()) {
            sb.append("No keywords extracted.\n\n");
        } else {
            for (Map.Entry<String, Integer> e : kwCounts.entrySet()) {
                sb.append("  • ").append(e.getKey()).append(" (appears in ").append(e.getValue()).append(" paper(s))\n");
            }
            sb.append("\n");
        }

        // 3. Category Distribution
        sb.append("-----------------------------------------------------------------------\n");
        sb.append("3. RESEARCH CATEGORIES DISTRIBUTION\n");
        sb.append("-----------------------------------------------------------------------\n");
        Map<String, Integer> catCounts = getCategoryDistribution(papers);
        for (Map.Entry<String, Integer> e : catCounts.entrySet()) {
            sb.append("  • ").append(e.getKey()).append(": ").append(e.getValue()).append(" paper(s)\n");
        }
        sb.append("\n");

        // 4. Similarity Comparisons (if 2 or more papers)
        if (papers.size() >= 2) {
            sb.append("-----------------------------------------------------------------------\n");
            sb.append("4. PAIRWISE SIMILARITY ANALYSIS\n");
            sb.append("-----------------------------------------------------------------------\n");
            for (int i = 0; i < papers.size(); i++) {
                for (int j = i + 1; j < papers.size(); j++) {
                    Paper a = papers.get(i);
                    Paper b = papers.get(j);
                    SimilarityResult sim = similarityService.compare(a, b);
                    sb.append(String.format("  • [%s] vs [%s] -> %s Similarity\n",
                            truncate(a.getTitle(), 30), truncate(b.getTitle(), 30), sim.getScoreAsPercent()));
                }
            }
            sb.append("\n");
        }

        // 5. References / Citations
        sb.append("-----------------------------------------------------------------------\n");
        sb.append("5. REFERENCES & CITATIONS (IEEE)\n");
        sb.append("-----------------------------------------------------------------------\n");
        for (int i = 0; i < papers.size(); i++) {
            Paper p = papers.get(i);
            String cit = citationService.generateIeee(p);
            sb.append("[").append(i + 1).append("] ").append(cit).append("\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Exports the multi-paper analysis report to a PDF file using Apache PDFBox.
     */
    public void exportPdfReport(List<Paper> papers, File outputFile) throws IOException {
        if (papers == null || papers.isEmpty()) {
            throw new IllegalArgumentException("No papers selected for report export.");
        }

        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            float margin = 50;
            float yStart = 750;
            float bottomMargin = 50;
            float pageWidth = PDRectangle.A4.getWidth();
            float usableWidth = pageWidth - (2 * margin);

            PdfWriterHelper writer = new PdfWriterHelper(document, fontBold, fontRegular, fontItalic, margin, yStart, bottomMargin, usableWidth);

            // Title
            writer.writeTitle("AI Research Analysis Report");
            writer.writeSubtitle("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss")));
            writer.writeSubtitle("Total Papers Analyzed: " + papers.size());
            writer.addSpacing(15);

            // Section 1: Selected Papers
            writer.writeHeading("1. Selected Papers Overview");
            for (int i = 0; i < papers.size(); i++) {
                Paper p = papers.get(i);
                writer.writeSubHeading("[" + (i + 1) + "] " + p.getTitle());
                writer.writeLabelValue("Authors: ", nvl(p.getAuthors()));
                writer.writeLabelValue("Year: ", p.getYear() > 0 ? String.valueOf(p.getYear()) : "N/A");
                writer.writeLabelValue("Category: ", nvl(p.getCategory()));
                writer.writeLabelValue("Reading Status: ", p.getReadingStatus());
                writer.writeLabelValue("Favorite: ", p.isFavorite() ? "Yes (Favorite)" : "No");

                List<String> kws = paperService.getKeywordsForPaper(p.getId());
                writer.writeLabelValue("Keywords: ", kws.isEmpty() ? "(none)" : String.join(", ", kws));

                String abs = p.getAbstractText();
                if (abs != null && !abs.isBlank()) {
                    String cleanAbs = abs.replaceAll("\\r?\\n", " ").trim();
                    if (cleanAbs.length() > 400) cleanAbs = cleanAbs.substring(0, 400) + "...";
                    writer.writeParagraph("Abstract: " + cleanAbs);
                }
                writer.addSpacing(10);
            }

            // Section 2: Keywords
            writer.writeHeading("2. Common & Dominant Keywords");
            Map<String, Integer> kws = getAggregatedKeywords(papers);
            if (kws.isEmpty()) {
                writer.writeParagraph("No extracted keywords available.");
            } else {
                for (Map.Entry<String, Integer> e : kws.entrySet()) {
                    writer.writeBullet(e.getKey() + " (" + e.getValue() + " paper" + (e.getValue() > 1 ? "s" : "") + ")");
                }
            }
            writer.addSpacing(12);

            // Section 3: Categories
            writer.writeHeading("3. Domain Categories");
            Map<String, Integer> cats = getCategoryDistribution(papers);
            for (Map.Entry<String, Integer> e : cats.entrySet()) {
                writer.writeBullet(e.getKey() + ": " + e.getValue() + " paper" + (e.getValue() > 1 ? "s" : ""));
            }
            writer.addSpacing(12);

            // Section 4: Similarity (if 2 or more papers)
            if (papers.size() >= 2) {
                writer.writeHeading("4. Pairwise Text Similarity");
                for (int i = 0; i < papers.size(); i++) {
                    for (int j = i + 1; j < papers.size(); j++) {
                        Paper a = papers.get(i);
                        Paper b = papers.get(j);
                        SimilarityResult sim = similarityService.compare(a, b);
                        writer.writeBullet(truncate(a.getTitle(), 35) + " vs " + truncate(b.getTitle(), 35) +
                                " -> " + sim.getScoreAsPercent() + " similarity");
                    }
                }
                writer.addSpacing(12);
            }

            // Section 5: References
            writer.writeHeading("5. References (IEEE Style)");
            for (int i = 0; i < papers.size(); i++) {
                Paper p = papers.get(i);
                writer.writeParagraph("[" + (i + 1) + "] " + citationService.generateIeee(p));
            }

            writer.close();
            document.save(outputFile);
        }
    }

    // ================================================================
    // HELPER ANALYSIS METHODS
    // ================================================================

    public Map<String, Integer> getAggregatedKeywords(List<Paper> papers) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Paper p : papers) {
            List<String> list = paperService.getKeywordsForPaper(p.getId());
            Set<String> seenForThisPaper = new HashSet<>();
            for (String kw : list) {
                if (kw == null || kw.isBlank()) continue;
                String normalized = kw.trim();
                String key = normalized.toLowerCase();
                if (seenForThisPaper.add(key)) {
                    counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
                }
            }
        }

        // Sort descending by occurrence
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    public Map<String, Integer> getCategoryDistribution(List<Paper> papers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Paper p : papers) {
            String cat = (p.getCategory() == null || p.getCategory().isBlank()) ? "Uncategorized" : p.getCategory().trim();
            map.put(cat, map.getOrDefault(cat, 0) + 1);
        }
        return map;
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "N/A" : s.trim();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    // ================================================================
    // INTERNAL PDF WRITER HELPER CLASS
    // ================================================================

    private static class PdfWriterHelper {
        private final PDDocument document;
        private final PDType1Font fontBold;
        private final PDType1Font fontRegular;
        private final PDType1Font fontItalic;
        private final float margin;
        private final float topMargin;
        private final float bottomMargin;
        private final float usableWidth;

        private PDPage currentPage;
        private PDPageContentStream contentStream;
        private float currentY;

        public PdfWriterHelper(PDDocument document, PDType1Font fontBold, PDType1Font fontRegular,
                               PDType1Font fontItalic, float margin, float topMargin,
                               float bottomMargin, float usableWidth) throws IOException {
            this.document = document;
            this.fontBold = fontBold;
            this.fontRegular = fontRegular;
            this.fontItalic = fontItalic;
            this.margin = margin;
            this.topMargin = topMargin;
            this.bottomMargin = bottomMargin;
            this.usableWidth = usableWidth;
            newPage();
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            contentStream = new PDPageContentStream(document, currentPage);
            currentY = topMargin;
        }

        private void ensureSpace(float spaceNeeded) throws IOException {
            if (currentY - spaceNeeded < bottomMargin) {
                newPage();
            }
        }

        public void writeTitle(String text) throws IOException {
            ensureSpace(35);
            contentStream.beginText();
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(clean(text));
            contentStream.endText();
            currentY -= 24;
        }

        public void writeSubtitle(String text) throws IOException {
            ensureSpace(18);
            contentStream.beginText();
            contentStream.setFont(fontItalic, 10);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(clean(text));
            contentStream.endText();
            currentY -= 14;
        }

        public void writeHeading(String text) throws IOException {
            ensureSpace(28);
            addSpacing(8);
            contentStream.beginText();
            contentStream.setFont(fontBold, 13);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(clean(text));
            contentStream.endText();
            currentY -= 18;
        }

        public void writeSubHeading(String text) throws IOException {
            ensureSpace(20);
            contentStream.beginText();
            contentStream.setFont(fontBold, 10);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText(clean(text));
            contentStream.endText();
            currentY -= 14;
        }

        public void writeLabelValue(String label, String value) throws IOException {
            writeParagraph(label + value);
        }

        public void writeBullet(String text) throws IOException {
            writeParagraph("•  " + text);
        }

        public void writeParagraph(String text) throws IOException {
            List<String> lines = wrapText(text, fontRegular, 9, usableWidth);
            for (String line : lines) {
                ensureSpace(14);
                contentStream.beginText();
                contentStream.setFont(fontRegular, 9);
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText(clean(line));
                contentStream.endText();
                currentY -= 13;
            }
        }

        public void addSpacing(float pts) {
            currentY -= pts;
        }

        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }

        private String clean(String text) {
            if (text == null) return "";
            // Replace non-ascii or unencodable characters with ascii equivalents
            return text.replace("★", "*")
                       .replace("☆", "")
                       .replace("—", "-")
                       .replace("–", "-")
                       .replace("“", "\"")
                       .replace("”", "\"")
                       .replace("‘", "'")
                       .replace("’", "'")
                       .replaceAll("[^\\x20-\\x7E]", " ");
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth) {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isBlank()) return lines;

            String[] words = clean(text).split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
                float width;
                try {
                    width = font.getStringWidth(candidate) / 1000 * fontSize;
                } catch (IOException e) {
                    width = candidate.length() * (fontSize * 0.5f);
                }

                if (width <= maxWidth) {
                    currentLine = new StringBuilder(candidate);
                } else {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                    }
                    currentLine = new StringBuilder(word);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            return lines;
        }
    }
}
