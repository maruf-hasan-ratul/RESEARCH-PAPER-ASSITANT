package com.example.research_project.util;

import com.example.research_project.model.Paper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FileUtil.java - Reads text files and parses paper sections.
 *
 * Supports the structured format:
 *   Title: Deep Learning for Medical Imaging
 *   Authors: John Smith, Jane Doe
 *   Year: 2024
 *   Abstract:
 *   This paper investigates...
 *   Methodology:
 *   ...
 *   Findings:
 *   ...
 */
public class FileUtil {

    private FileUtil() {}

    /**
     * Reads the entire content of a text file as a String.
     */
    public static String readTextFile(String filePath) {
        try {
            return Files.readString(Path.of(filePath));
        } catch (IOException e) {
            System.err.println("FileUtil: Cannot read file: " + filePath + " -> " + e.getMessage());
            return "";
        }
    }

    /**
     * Writes text to a file. Returns true if successful.
     */
    public static boolean writeTextFile(String filePath, String content) {
        try {
            Files.writeString(Path.of(filePath), content);
            return true;
        } catch (IOException e) {
            System.err.println("FileUtil: Cannot write file: " + filePath + " -> " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads a .txt file and parses it into a Paper object.
     * Looks for "Label:" or "Label:\n..." patterns.
     * Returns null if the file cannot be read.
     */
    public static Paper parseFromFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;

        // Check if PDF file
        if (filePath.toLowerCase().endsWith(".pdf")) {
            try {
                PdfUtil.PdfParseResult result = PdfUtil.parsePdf(new java.io.File(filePath));
                return result != null ? result.getPaper() : null;
            } catch (Exception e) {
                System.err.println("FileUtil: Error parsing PDF file " + filePath + " -> " + e.getMessage());
                return null;
            }
        }

        String content = readTextFile(filePath);
        if (content == null || content.isBlank()) return null;

        Paper paper = new Paper();
        paper.setFilePath(filePath);

        paper.setTitle(extractInline(content, "Title"));
        paper.setAuthors(extractInline(content, "Authors", "Author"));
        paper.setAbstractText(extractBlock(content, "Abstract"));
        paper.setMethodology(extractBlock(content, "Methodology", "Method"));
        paper.setFindings(extractBlock(content, "Findings", "Results", "Conclusion"));

        // Parse year
        String yearStr = extractInline(content, "Year");
        if (yearStr != null && !yearStr.isBlank()) {
            try {
                paper.setYear(Integer.parseInt(yearStr.trim()));
            } catch (NumberFormatException ignored) {}
        }

        // If no structured title found, use first non-empty line
        if (paper.getTitle() == null || paper.getTitle().isBlank()) {
            String firstLine = content.lines()
                .filter(l -> !l.isBlank())
                .findFirst().orElse("");
            paper.setTitle(firstLine);
        }

        return paper;
    }

    // ---- Extract "Label: value" from the same line ----
    private static String extractInline(String text, String... labels) {
        for (String label : labels) {
            Pattern p = Pattern.compile("(?im)^" + Pattern.quote(label) + "\\s*:\\s*(.+)$");
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1).trim();
        }
        return null;
    }

    // ---- Extract multi-line block after "Label:" heading ----
    private static String extractBlock(String text, String... labels) {
        for (String label : labels) {
            // Match label on its own line, then capture until next label or end
            Pattern p = Pattern.compile(
                "(?im)^" + Pattern.quote(label) + "\\s*:?\\s*$(\\r?\\n)([\\s\\S]*?)(?=^[A-Z][a-z]+\\s*:?\\s*$|\\z)",
                Pattern.MULTILINE
            );
            Matcher m = p.matcher(text);
            if (m.find()) {
                String block = m.group(2).trim();
                if (!block.isBlank()) return block;
            }
            // Fallback: inline
            String inline = extractInline(text, label);
            if (inline != null && !inline.isBlank()) return inline;
        }
        return null;
    }
}