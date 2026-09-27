package com.example.research_project.util;

import com.example.research_project.model.Paper;
import com.example.research_project.service.ClassificationService;
import com.example.research_project.service.KeywordService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfUtil.java - Extracts text, sections, and metadata from academic PDF papers.
 *
 * Responsibilities:
 *   1. Extracts text from PDF files using Apache PDFBox 3.x (with sortByPosition for multi-column papers).
 *   2. Extracts title, authors, year, abstract, methodology, findings, and keywords.
 *   3. Uses ClassificationService for automated topic categorization.
 *   4. Packages extracted content into a ready-to-save Paper model.
 */
public class PdfUtil {

    private static final ClassificationService classificationService = new ClassificationService();
    private static final KeywordService keywordService = new KeywordService();

    private PdfUtil() {}

    /**
     * Container holding the extracted Paper, detected keywords, page count, and full text.
     */
    public static class PdfParseResult {
        private final Paper paper;
        private final List<String> keywords;
        private final int pageCount;
        private final String fullText;

        public PdfParseResult(Paper paper, List<String> keywords, int pageCount, String fullText) {
            this.paper = paper;
            this.keywords = keywords;
            this.pageCount = pageCount;
            this.fullText = fullText;
        }

        public Paper getPaper() { return paper; }
        public List<String> getKeywords() { return keywords; }
        public int getPageCount() { return pageCount; }
        public String getFullText() { return fullText; }
    }

    /**
     * Extracts full raw text from a PDF file.
     *
     * @param file The PDF file.
     * @return Extracted text string.
     * @throws IOException If PDF cannot be read or is encrypted.
     */
    public static String extractRawText(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Crucial for two-column academic papers
            return cleanText(stripper.getText(document));
        } catch (InvalidPasswordException e) {
            throw new IOException("This PDF is password-protected and cannot be read without a password.", e);
        }
    }

    /**
     * Parses a PDF file into a populated Paper object and keywords.
     *
     * @param file The PDF file to inspect.
     * @return PdfParseResult containing the Paper, keywords, and metadata.
     * @throws IOException If reading the PDF fails.
     */
    public static PdfParseResult parsePdf(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            int pageCount = document.getNumberOfPages();

            // Extract entire text with coordinate sorting for columns
            PDFTextStripper fullStripper = new PDFTextStripper();
            fullStripper.setSortByPosition(true);
            String fullText = cleanText(fullStripper.getText(document));

            if (fullText.trim().length() < 20) {
                throw new IOException("No readable text found in PDF. The document may be a scanned image or empty.");
            }

            // Extract page 1 specifically for title, authors, year, and abstract heuristics
            PDFTextStripper page1Stripper = new PDFTextStripper();
            page1Stripper.setSortByPosition(true);
            page1Stripper.setStartPage(1);
            page1Stripper.setEndPage(1);
            String page1Text = cleanText(page1Stripper.getText(document));

            PDDocumentInformation info = document.getDocumentInformation();

            Paper paper = new Paper();
            paper.setFilePath(file.getAbsolutePath());

            // 1. Title
            String title = extractTitle(info, page1Text, file.getName());
            paper.setTitle(title);

            // 2. Authors
            String authors = extractAuthors(info, page1Text, title);
            paper.setAuthors(authors);

            // 3. Year
            int year = extractYear(info, page1Text, fullText);
            paper.setYear(year);

            // 4. Abstract
            String abstractText = extractAbstract(fullText, page1Text);
            paper.setAbstractText(abstractText);

            // 5. Methodology
            String methodology = extractMethodology(fullText);
            paper.setMethodology(methodology);

            // 6. Findings / Results
            String findings = extractFindings(fullText);
            paper.setFindings(findings);

            // 7. Domain Category (via ClassificationService)
            String category = classificationService.classify(fullText);
            if (category == null || category.equalsIgnoreCase("Other")) {
                // Try classifying just abstract + title
                category = classificationService.classify(title + " " + abstractText);
            }
            paper.setCategory(category != null ? category : "Other");

            // 8. Source
            String source = extractSource(fullText, file.getName());
            paper.setSource(source);

            // 9. Keywords
            List<String> keywords = extractKeywords(fullText);

            return new PdfParseResult(paper, keywords, pageCount, fullText);

        } catch (InvalidPasswordException e) {
            throw new IOException("The PDF file is encrypted with a password.", e);
        }
    }

    // ================================================================
    // SECTION EXTRACTION HEURISTICS
    // ================================================================

    /**
     * Extracts paper title by evaluating PDF metadata and page 1 text.
     */
    private static String extractTitle(PDDocumentInformation info, String page1Text, String fileName) {
        // First check document metadata
        if (info != null && info.getTitle() != null) {
            String metaTitle = info.getTitle().trim();
            if (isGoodTitle(metaTitle)) {
                return metaTitle;
            }
        }

        // Parse first non-empty lines of page 1
        String[] lines = page1Text.split("\\r?\\n");
        StringBuilder candidate = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            // Skip running headers, arXiv stamps, or journal notice lines
            if (isHeaderJunk(line)) continue;

            // Stop if we hit author affiliations, Abstract, or section headers
            if (line.matches("(?i)^(abstract|contents|table of contents|keywords|index terms).*") ||
                line.matches("(?i)^(\\d+\\s+)?introduction.*")) {
                break;
            }

            // Stop if line looks like an email or institution URL
            if (line.contains("@") || line.matches("(?i).*(university|institute|department|faculty|school|laboratory).*")) {
                break;
            }

            if (candidate.length() > 0) candidate.append(" ");
            candidate.append(line);

            // Academic titles are rarely more than 2-3 lines (~200 chars)
            if (candidate.length() > 220) break;
        }

        String extracted = candidate.toString().trim();
        if (isGoodTitle(extracted)) {
            return extracted;
        }

        // Fallback: clean filename
        return cleanFileName(fileName);
    }

    private static boolean isHeaderJunk(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("arxiv:") ||
               lower.startsWith("doi:") ||
               lower.startsWith("http://") ||
               lower.startsWith("https://") ||
               lower.matches(".*\\b(proceedings of|ieee transactions|acm transactions|journal of|vol\\.?\\s*\\d+|issn|isbn|preprint|under review)\\b.*") ||
               lower.matches("^\\d+$");
    }

    private static boolean isGoodTitle(String t) {
        if (t == null) return false;
        String s = t.trim();
        if (s.length() < 4 || s.length() > 300) return false;
        String lower = s.toLowerCase();
        if (lower.equals("untitled") || lower.startsWith("microsoft word") ||
            lower.endsWith(".pdf") || lower.endsWith(".docx") || lower.startsWith("latex")) {
            return false;
        }
        return true;
    }

    private static String cleanFileName(String fileName) {
        String clean = fileName.replaceAll("(?i)\\.pdf$", "");
        clean = clean.replace('_', ' ').replace('-', ' ');
        return clean.trim();
    }

    /**
     * Extracts authors from metadata or page 1.
     */
    private static String extractAuthors(PDDocumentInformation info, String page1Text, String title) {
        if (info != null && info.getAuthor() != null) {
            String metaAuthor = info.getAuthor().trim();
            if (!metaAuthor.isEmpty() && !metaAuthor.matches("(?i)^(admin|user|administrator|author|tex|latex)$")) {
                return metaAuthor;
            }
        }

        // Search in page 1: text between title and "Abstract"
        int titleIdx = page1Text.indexOf(title);
        int searchStart = (titleIdx >= 0) ? titleIdx + title.length() : 0;

        Pattern absPattern = Pattern.compile("(?im)^\\s*(abstract|keywords|index terms)\\b");
        Matcher absMatcher = absPattern.matcher(page1Text);
        int absIdx = absMatcher.find(searchStart) ? absMatcher.start() : -1;

        if (absIdx > searchStart) {
            String authorBlock = page1Text.substring(searchStart, absIdx).trim();
            String[] lines = authorBlock.split("\\r?\\n");
            List<String> validAuthorLines = new ArrayList<>();

            for (String l : lines) {
                String line = l.trim();
                if (line.isEmpty()) continue;
                if (line.contains("@")) continue; // email
                if (line.matches("(?i).*(university|institute|department|faculty|school|laboratory|college|center|centre|inc\\.|llc|corp).*")) {
                    continue; // affiliation
                }
                // Strip footnote symbols / superscripts like 1, 2, *, †, ‡
                String cleaned = line.replaceAll("[0-9*†‡,]+$", "")
                                     .replaceAll("^[0-9*†‡,\\s]+", "")
                                     .trim();
                if (!cleaned.isEmpty() && cleaned.length() < 120) {
                    validAuthorLines.add(cleaned);
                }
            }

            if (!validAuthorLines.isEmpty()) {
                return String.join(", ", validAuthorLines);
            }
        }

        return "Unknown";
    }

    /**
     * Extracts 4-digit publication year.
     */
    private static int extractYear(PDDocumentInformation info, String page1Text, String fullText) {
        // Check metadata creation date
        if (info != null && info.getCreationDate() != null) {
            int year = info.getCreationDate().get(Calendar.YEAR);
            if (year >= 1950 && year <= 2035) {
                return year;
            }
        }

        // Look for copyright or published year on page 1
        Pattern p = Pattern.compile("(?i)(?:published|accepted|proceedings|copyright|©|arxiv:\\s*\\d{2})(\\d{2})?\\b.*?\\b(19\\d{2}|20\\d{2})\\b");
        Matcher m = p.matcher(page1Text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(2));
            } catch (NumberFormatException ignored) {}
        }

        // Fallback: look for 4-digit year in page 1
        Pattern generalYear = Pattern.compile("\\b(19[89]\\d|20[0-2]\\d)\\b");
        Matcher gm = generalYear.matcher(page1Text);
        if (gm.find()) {
            try {
                return Integer.parseInt(gm.group(1));
            } catch (NumberFormatException ignored) {}
        }

        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Extracts abstract section.
     */
    private static String extractAbstract(String fullText, String page1Text) {
        // Regex search for "Abstract" section
        Pattern p = Pattern.compile(
            "(?im)^\\s*(?:abstract|a\\s*b\\s*s\\s*t\\s*r\\s*a\\s*c\\s*t)\\s*[:\\.\\—\\-]?\\s*\\r?\\n?([\\s\\S]+?)(?=\\r?\\n\\s*(?:(?:[1I]\\.?\\s+)?introduction|keywords|index terms|key words|1\\.\\s*background|\\z))"
        );

        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String abs = m.group(1).trim();
            if (abs.length() > 50) {
                return cleanAbstract(abs);
            }
        }

        // Search page 1 if not matched full text
        Matcher m1 = p.matcher(page1Text);
        if (m1.find()) {
            String abs = m1.group(1).trim();
            if (abs.length() > 50) {
                return cleanAbstract(abs);
            }
        }

        // Fallback: take first 1000 characters from page 1 after title
        String[] paragraphs = page1Text.split("\\r?\\n\\r?\\n+");
        for (String para : paragraphs) {
            String pTrim = para.trim();
            if (pTrim.length() > 150 && !pTrim.contains("@") && !isHeaderJunk(pTrim)) {
                return cleanAbstract(pTrim);
            }
        }

        return fullText.length() > 800 ? fullText.substring(0, 800).trim() + "..." : fullText.trim();
    }

    private static String cleanAbstract(String text) {
        // Limit to reasonable abstract size (up to 3000 chars)
        if (text.length() > 3000) {
            text = text.substring(0, 3000).trim();
        }
        return text;
    }

    /**
     * Extracts Methodology or Proposed System section.
     */
    private static String extractMethodology(String fullText) {
        Pattern p = Pattern.compile(
            "(?im)^\\s*(?:(?:\\d+\\.?\\s+)?(?:methodology|methods?|proposed\\s+(?:method|system|framework|model|architecture)|model\\s+architecture|system\\s+design|our\\s+approach|materials\\s+and\\s+methods))\\s*[:\\.\\—\\-]?\\s*\\r?\\n([\\s\\S]+?)(?=\\r?\\n\\s*(?:(?:\\d+\\.?\\s+)?(?:results?|findings?|experiments?|experimental\\s+results|evaluation|discussion|conclusion|related\\s+work)|\\z))"
        );

        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String block = m.group(1).trim();
            if (block.length() > 40) {
                return truncate(block, 2000);
            }
        }
        return "";
    }

    /**
     * Extracts Findings, Experiments, or Results section.
     */
    private static String extractFindings(String fullText) {
        Pattern p = Pattern.compile(
            "(?im)^\\s*(?:(?:\\d+\\.?\\s+)?(?:findings?|results?(?:\\s+and\\s+discussion)?|experimental\\s+results?|experiments?|evaluation|conclusions?))\\s*[:\\.\\—\\-]?\\s*\\r?\\n([\\s\\S]+?)(?=\\r?\\n\\s*(?:(?:\\d+\\.?\\s+)?(?:references?|acknowledgments?|acknowledgements?|discussion|appendix)|\\z))"
        );

        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String block = m.group(1).trim();
            if (block.length() > 40) {
                return truncate(block, 2000);
            }
        }
        return "";
    }

    /**
     * Extracts source URL or DOI if present in the text, otherwise notes the PDF filename.
     */
    private static String extractSource(String fullText, String fileName) {
        // Check for DOI
        Pattern doi = Pattern.compile("(?i)\\b(10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+)\\b");
        Matcher dm = doi.matcher(fullText);
        if (dm.find()) {
            return "https://doi.org/" + dm.group(1);
        }

        // Check for arXiv
        Pattern arxiv = Pattern.compile("(?i)\\barxiv:\\s*(\\d{4}\\.\\d{4,5}(?:v\\d+)?)\\b");
        Matcher am = arxiv.matcher(fullText);
        if (am.find()) {
            return "https://arxiv.org/abs/" + am.group(1);
        }

        return "PDF: " + fileName;
    }

    /**
     * Extracts keywords from PDF text, or generates them via KeywordService.
     */
    private static List<String> extractKeywords(String fullText) {
        Pattern p = Pattern.compile("(?im)^\\s*(?:keywords|index\\s+terms|key\\s*words)\\s*[:\\.\\—\\-]?\\s*([^\\r\\n]+)");
        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String line = m.group(1).trim();
            String[] tokens = line.split("[,;•·]+");
            List<String> list = new ArrayList<>();
            for (String tok : tokens) {
                String clean = tok.replaceAll("[^a-zA-Z0-9\\s-]", "").trim();
                if (!clean.isEmpty() && clean.length() < 40) {
                    list.add(clean);
                }
            }
            if (!list.isEmpty()) {
                return list.subList(0, Math.min(list.size(), 7));
            }
        }

        // Fallback to NLP KeywordService
        return keywordService.extractKeywords(fullText, 5);
    }

    /**
     * Normalizes text, fixes hyphenated line wraps (e.g. trans-\nformer -> transformer).
     */
    private static String cleanText(String text) {
        if (text == null) return "";
        // Rejoin words split across lines by hyphens
        String unhyphenated = text.replaceAll("(?<=\\b[a-zA-Z]{2,})-\\r?\\n(?=[a-zA-Z]{2,}\\b)", "");
        // Normalize multiple spaces (preserving newlines)
        return unhyphenated.replaceAll("[ \\t\\x0B\\f]+", " ").trim();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength).trim() + "...";
    }
}
