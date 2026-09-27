package com.example.research_project.service;

import com.example.research_project.dao.KeywordDAO;
import com.example.research_project.dao.NoteDAO;
import com.example.research_project.dao.PaperDAO;
import com.example.research_project.model.Keyword;
import com.example.research_project.model.Note;
import com.example.research_project.model.Paper;

import com.example.research_project.util.PdfUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * PaperService.java - Business logic layer for papers.
 *
 * ================================================================
 * WHAT IS BUSINESS LOGIC?
 * ================================================================
 * Business logic = rules about the data that go beyond just
 * "save it" or "load it".
 *
 * Examples implemented here:
 *   - Validate that title is not blank before saving
 *   - Validate that year is a realistic number
 *   - Automatically set the createdAt timestamp
 *   - Trim whitespace from text fields
 *   - Coordinate saving keywords at the same time as a paper
 *
 * ================================================================
 * WHERE DOES IT FIT?
 * ================================================================
 *
 *   Controller (handles button clicks)
 *       |
 *       v
 *   PaperService  <-- THIS FILE (validates + coordinates)
 *       |
 *       v
 *   PaperDAO / KeywordDAO / NoteDAO  (raw SQL)
 *       |
 *       v
 *   SQLite database
 *
 * The controller does NOT talk to the DAO directly.
 * The controller only calls PaperService methods.
 *
 * ================================================================
 * TIMESTAMP FORMAT
 * ================================================================
 * We store dates as strings in SQLite (TEXT column).
 * Format: "2025-01-10 14:30:00"
 * This is ISO-8601 date-time format, which sorts correctly as text.
 */
public class PaperService {

    // DAO objects - the service uses these to talk to the database
    private final PaperDAO paperDAO       = new PaperDAO();
    private final KeywordDAO keywordDAO   = new KeywordDAO();
    private final NoteDAO noteDAO         = new NoteDAO();

    // Date formatter for createdAt timestamps
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================================================================
    // ADD PAPER
    // ================================================================

    /**
     * Validates and saves a new paper to the database.
     *
     * @param paper  Paper to save (id will be set by this method)
     * @return       The generated id, or -1 if validation/save failed
     * @throws       IllegalArgumentException if validation fails
     */
    public int addPaper(Paper paper) {
        // Step 1: Validate
        validatePaper(paper);

        // Step 2: Clean up text fields (trim whitespace)
        sanitise(paper);

        // Step 3: Set the creation timestamp automatically
        paper.setCreatedAt(now());

        // Step 4: Save to database
        return paperDAO.addPaper(paper);
    }

    /**
     * Validates a paper AND saves its keywords at the same time.
     * Use this when you already have a list of keywords to save.
     *
     * @param paper     Paper to save
     * @param keywords  Keywords to associate with the paper
     * @return          The generated id, or -1 if failed
     */
    public int addPaperWithKeywords(Paper paper, List<String> keywords) {
        int id = addPaper(paper);
        if (id > 0 && keywords != null && !keywords.isEmpty()) {
            keywordDAO.addKeywords(id, keywords);
        }
        return id;
    }

    /**
     * Extracts text and metadata from a PDF file, parses it into a Paper model,
     * saves it directly to the SQLite database with extracted keywords, and logs an initial note.
     *
     * @param pdfFile The PDF file to import.
     * @return The saved Paper object with database ID populated.
     * @throws IOException If PDF reading fails.
     */
    public Paper importPaperFromPdf(File pdfFile) throws IOException {
        PdfUtil.PdfParseResult result = PdfUtil.parsePdf(pdfFile);
        Paper paper = result.getPaper();

        int id = addPaperWithKeywords(paper, result.getKeywords());
        if (id > 0) {
            paper.setId(id);
            long kbSize = pdfFile.length() / 1024;
            String noteText = "Imported from PDF: " + pdfFile.getName() + " (" +
                              result.getPageCount() + " pages, " + kbSize + " KB)";
            addNote(id, noteText);
        }
        return paper;
    }

    // ================================================================
    // GET PAPER
    // ================================================================

    public Paper getPaperById(int id) {
        return paperDAO.getPaperById(id);
    }

    public List<Paper> getAllPapers() {
        return paperDAO.getAllPapers();
    }

    public List<Paper> getRecentPapers(int limit) {
        return paperDAO.getRecentPapers(limit);
    }

    // ================================================================
    // UPDATE PAPER
    // ================================================================

    /**
     * Validates and updates an existing paper.
     *
     * @param paper  Paper with updated fields (must have a valid id)
     * @return       true if successful
     * @throws       IllegalArgumentException if validation fails
     */
    public boolean updatePaper(Paper paper) {
        validatePaper(paper);
        sanitise(paper);
        return paperDAO.updatePaper(paper);
    }

    // ================================================================
    // DELETE PAPER
    // ================================================================

    /**
     * Deletes a paper and all its keywords and notes.
     * (ON DELETE CASCADE in SQLite handles keywords/notes automatically)
     *
     * @param id  The paper id to delete
     * @return    true if successful
     */
    public boolean deletePaper(int id) {
        return paperDAO.deletePaper(id);
    }

    // ================================================================
    // SEARCH & FILTER
    // ================================================================

    public List<Paper> searchPapers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllPapers();
        }
        return paperDAO.searchPapers(keyword.trim());
    }

    public List<Paper> filterPapers(String category, int year) {
        return paperDAO.filterPapers(category, year);
    }

    public List<Paper> searchAdvanced(String query, String category, int year, String readingStatus, Boolean favoriteOnly) {
        return paperDAO.searchAdvanced(query, category, year, readingStatus, favoriteOnly);
    }

    // ================================================================
    // FAVORITES & READING STATUS
    // ================================================================

    public boolean setFavorite(int paperId, boolean favorite) {
        return paperDAO.setFavorite(paperId, favorite);
    }

    public boolean updateReadingStatus(int paperId, String readingStatus) {
        return paperDAO.updateReadingStatus(paperId, readingStatus);
    }

    public List<Paper> getFavoritePapers() {
        return paperDAO.getFavoritePapers();
    }

    public List<Paper> getPapersByStatus(String status) {
        return paperDAO.findByStatus(status);
    }

    // ================================================================
    // STATISTICS (used by DashboardController)
    // ================================================================

    public int getTotalPaperCount() {
        return paperDAO.countPapers();
    }

    public int getTotalCategoryCount() {
        return paperDAO.countCategories();
    }

    public int getAnalysedPaperCount() {
        return paperDAO.countAnalysedPapers();
    }

    public int getFavoritePaperCount() {
        return paperDAO.countFavoritePapers();
    }

    public int getUnreadPaperCount() {
        return paperDAO.countUnreadPapers();
    }

    public int getReadingPaperCount() {
        return paperDAO.countReadingPapers();
    }

    public int getCompletedPaperCount() {
        return paperDAO.countCompletedPapers();
    }

    public int getTotalNoteCount() {
        return noteDAO.countNotes();
    }

    public Map<String, Integer> getCategoryStatistics() {
        return paperDAO.getCategoryStatistics();
    }

    public Map<String, Integer> getTopKeywords(int limit) {
        return paperDAO.getTopKeywords(limit);
    }

    // ================================================================
    // KEYWORDS
    // ================================================================

    /**
     * Replaces all keywords for a paper with a new list.
     * Called after analysis to update the stored keywords.
     *
     * @param paperId   The paper to update
     * @param keywords  New list of keyword strings
     */
    public void updateKeywords(int paperId, List<String> keywords) {
        keywordDAO.deleteKeywordsByPaperId(paperId); // remove old keywords
        keywordDAO.addKeywords(paperId, keywords);   // add new keywords
    }

    public List<String> getKeywordsForPaper(int paperId) {
        return keywordDAO.getKeywordStringsByPaperId(paperId);
    }

    public List<Keyword> getKeywordObjectsForPaper(int paperId) {
        return keywordDAO.getKeywordsByPaperId(paperId);
    }

    // ================================================================
    // NOTES
    // ================================================================

    /**
     * Adds a user note to a paper.
     * The timestamp is set automatically.
     *
     * @param paperId   The paper to annotate
     * @param noteText  The note text written by the user
     */
    public void addNote(int paperId, String noteText) {
        if (noteText == null || noteText.isBlank()) {
            throw new IllegalArgumentException("Note text cannot be empty.");
        }
        Note note = new Note(paperId, noteText.trim(), now());
        noteDAO.addNote(note);
    }

    public List<Note> getNotesForPaper(int paperId) {
        return noteDAO.getNotesByPaperId(paperId);
    }

    public void deleteNote(int noteId) {
        noteDAO.deleteNote(noteId);
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    /**
     * Validates a paper before saving or updating.
     * Throws IllegalArgumentException with a clear message if invalid.
     * The controller catches this and shows an alert to the user.
     */
    private void validatePaper(Paper paper) {
        if (paper == null) {
            throw new IllegalArgumentException("Paper cannot be null.");
        }
        if (paper.getTitle() == null || paper.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        if (paper.getYear() < 0) {
            throw new IllegalArgumentException("Year cannot be negative.");
        }
        if (paper.getYear() > 0 && (paper.getYear() < 1000 || paper.getYear() > 2100)) {
            throw new IllegalArgumentException("Year must be between 1000 and 2100.");
        }
    }

    /**
     * Trims leading/trailing whitespace from all String fields.
     * Prevents saving "  Deep Learning  " instead of "Deep Learning".
     */
    private void sanitise(Paper paper) {
        if (paper.getTitle()       != null) paper.setTitle      (paper.getTitle().trim());
        if (paper.getAuthors()     != null) paper.setAuthors    (paper.getAuthors().trim());
        if (paper.getCategory()    != null) paper.setCategory   (paper.getCategory().trim());
        if (paper.getSource()      != null) paper.setSource     (paper.getSource().trim());
        if (paper.getAbstractText()!= null) paper.setAbstractText(paper.getAbstractText().trim());
        if (paper.getMethodology() != null) paper.setMethodology(paper.getMethodology().trim());
        if (paper.getFindings()    != null) paper.setFindings   (paper.getFindings().trim());
    }

    /**
     * Returns the current date-time as a formatted string.
     * Example: "2025-01-10 14:30:00"
     */
    private String now() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}