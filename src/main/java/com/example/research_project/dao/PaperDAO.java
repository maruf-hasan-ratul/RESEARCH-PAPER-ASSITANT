package com.example.research_project.dao;

import com.example.research_project.database.Database;
import com.example.research_project.model.Paper;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PaperDAO.java - All database operations for the 'papers' table.
 *
 * IMPORTANT CONCEPTS USED HERE:
 *
 * 1. PreparedStatement (NOT Statement)
 *    We NEVER build SQL by concatenating user input like:
 *      "SELECT * FROM papers WHERE title = '" + userInput + "'"  <- WRONG!
 *    That is called SQL Injection - a serious security bug.
 *    Instead we use:
 *      PreparedStatement ps = conn.prepareStatement("SELECT * FROM papers WHERE title = ?");
 *      ps.setString(1, userInput);
 *    The '?' is a placeholder. JDBC escapes the value safely.
 *
 * 2. try-with-resources
 *    Every Connection, PreparedStatement, and ResultSet is opened
 *    inside a try-with-resources block. Java automatically calls
 *    .close() on them when the block ends, even if an exception occurs.
 *    This prevents connection/memory leaks.
 *
 * 3. ResultSet -> Paper (mapRow helper)
 *    A ResultSet is like a table cursor that moves row by row.
 *    The private method mapRow() reads one row and builds a Paper object.
 *    This avoids repeating the same column-reading code in every method.
 *
 * 4. AUTOINCREMENT id
 *    SQLite generates the id automatically. After INSERT we use
 *    getGeneratedKeys() to find out what id was assigned.
 */
public class PaperDAO {

    // ================================================================
    // CREATE
    // ================================================================

    /**
     * Adds a new paper to the database.
     *
     * @param paper  Paper object with title, authors, year, etc.
     * @return       The auto-generated id, or -1 if insertion failed.
     */
    public int addPaper(Paper paper) {
        String sql = "INSERT INTO papers " +
                "(title, authors, year, abstract, methodology, findings, " +
                " category, source, file_path, created_at, favorite, reading_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // GENERATED_KEYS tells JDBC to return the auto-generated id after INSERT
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, paper.getTitle());
            ps.setString(2, paper.getAuthors());
            ps.setInt   (3, paper.getYear());
            ps.setString(4, paper.getAbstractText());
            ps.setString(5, paper.getMethodology());
            ps.setString(6, paper.getFindings());
            ps.setString(7, paper.getCategory());
            ps.setString(8, paper.getSource());
            ps.setString(9, paper.getFilePath());
            ps.setString(10, paper.getCreatedAt());
            ps.setInt   (11, paper.isFavorite() ? 1 : 0);
            ps.setString(12, paper.getReadingStatus());

            ps.executeUpdate(); // run the INSERT

            // Retrieve the id that SQLite assigned
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    paper.setId(newId); // update the paper object with its new id
                    return newId;
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.addPaper() error: " + e.getMessage());
        }

        return -1; // means it failed
    }

    // ================================================================
    // READ (single)
    // ================================================================

    /**
     * Returns a single paper by its id, or null if not found.
     *
     * @param id  The paper's primary key
     * @return    Paper object, or null
     */
    public Paper getPaperById(int id) {
        String sql = "SELECT * FROM papers WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs); // convert the row to a Paper object
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.getPaperById() error: " + e.getMessage());
        }

        return null;
    }

    // ================================================================
    // READ (all)
    // ================================================================

    /**
     * Returns all papers in the database, ordered by most recently added.
     *
     * @return  List of Paper objects (empty list if none found)
     */
    public List<Paper> getAllPapers() {
        String sql = "SELECT * FROM papers ORDER BY id DESC";
        List<Paper> papers = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // rs.next() moves the cursor to the next row
            // It returns false when there are no more rows
            while (rs.next()) {
                papers.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.getAllPapers() error: " + e.getMessage());
        }

        return papers;
    }

    // ================================================================
    // UPDATE
    // ================================================================

    /**
     * Updates all fields of an existing paper.
     * The paper must already exist in the database (must have a valid id).
     *
     * @param paper  Paper with updated values (id must be set)
     * @return       true if the update was successful
     */
    public boolean updatePaper(Paper paper) {
        String sql = "UPDATE papers SET " +
                "title=?, authors=?, year=?, abstract=?, methodology=?, " +
                "findings=?, category=?, source=?, file_path=?, favorite=?, reading_status=? " +
                "WHERE id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, paper.getTitle());
            ps.setString(2, paper.getAuthors());
            ps.setInt   (3, paper.getYear());
            ps.setString(4, paper.getAbstractText());
            ps.setString(5, paper.getMethodology());
            ps.setString(6, paper.getFindings());
            ps.setString(7, paper.getCategory());
            ps.setString(8, paper.getSource());
            ps.setString(9, paper.getFilePath());
            ps.setInt   (10, paper.isFavorite() ? 1 : 0);
            ps.setString(11, paper.getReadingStatus());
            ps.setInt   (12, paper.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0; // true if at least one row was updated

        } catch (SQLException e) {
            System.err.println("PaperDAO.updatePaper() error: " + e.getMessage());
        }

        return false;
    }

    // ================================================================
    // DELETE
    // ================================================================

    /**
     * Deletes a paper by its id.
     * Because we have "ON DELETE CASCADE" in the database schema,
     * SQLite will automatically delete all linked keywords and notes too.
     *
     * @param id  The paper id to delete
     * @return    true if deletion was successful
     */
    public boolean deletePaper(int id) {
        String sql = "DELETE FROM papers WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("PaperDAO.deletePaper() error: " + e.getMessage());
        }

        return false;
    }

    // ================================================================
    // SEARCH
    // ================================================================

    /**
     * Searches papers by a keyword in the title or authors.
     *
     * LIKE with % is SQL pattern matching:
     *   %deep%  -> matches "Deep Learning", "Deep NLP", etc.
     *
     * @param keyword  Search term (partial match allowed)
     * @return         List of matching papers
     */
    public List<Paper> searchPapers(String keyword) {
        String sql = "SELECT * FROM papers " +
                "WHERE title LIKE ? OR authors LIKE ? " +
                "ORDER BY id DESC";
        List<Paper> results = new ArrayList<>();

        // Wrap the keyword with % for partial matching
        String pattern = "%" + keyword + "%";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.searchPapers() error: " + e.getMessage());
        }

        return results;
    }

    // ================================================================
    // FILTER
    // ================================================================

    /**
     * Returns papers filtered by category and/or year.
     * Pass null or 0 to skip a filter.
     *
     * Examples:
     *   filterPapers("Machine Learning", 0)  -> all ML papers, any year
     *   filterPapers(null, 2024)             -> all papers from 2024
     *   filterPapers("NLP", 2025)            -> NLP papers from 2025
     *   filterPapers(null, 0)                -> same as getAllPapers()
     *
     * @param category  Category name, or null to skip
     * @param year      Publication year, or 0 to skip
     * @return          Filtered list of papers
     */
    public List<Paper> filterPapers(String category, int year) {
        // Build the SQL dynamically based on which filters are active
        StringBuilder sql = new StringBuilder("SELECT * FROM papers WHERE 1=1");

        // "WHERE 1=1" is a trick: it's always true, so we can safely
        // append "AND ..." conditions without worrying about the first one

        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
        }
        if (year > 0) {
            sql.append(" AND year = ?");
        }
        sql.append(" ORDER BY id DESC");

        List<Paper> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Set parameters in the correct order
            int paramIndex = 1;
            if (category != null && !category.isBlank()) {
                ps.setString(paramIndex++, category);
            }
            if (year > 0) {
                ps.setInt(paramIndex, year);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.filterPapers() error: " + e.getMessage());
        }

        return results;
    }

    // ================================================================
    // STATISTICS
    // ================================================================

    /**
     * Returns the total number of papers in the database.
     * Used by the dashboard.
     */
    public int countPapers() {
        String sql = "SELECT COUNT(*) FROM papers";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.countPapers() error: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Returns the number of distinct categories in the database.
     * Used by the dashboard.
     */
    public int countCategories() {
        String sql = "SELECT COUNT(DISTINCT category) FROM papers WHERE category IS NOT NULL";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.countCategories() error: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Returns the N most recently added papers.
     * Used by the dashboard's "Recent Papers" section.
     *
     * @param limit  How many recent papers to return
     * @return       List of recent papers
     */
    public List<Paper> getRecentPapers(int limit) {
        String sql = "SELECT * FROM papers ORDER BY id DESC LIMIT ?";
        List<Paper> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.getRecentPapers() error: " + e.getMessage());
        }

        return results;
    }

    /**
     * Returns the count of papers that have a non-null category.
     * We use this as "analysed papers" count on the dashboard,
     * because a paper that has been classified has a category.
     */
    public int countAnalysedPapers() {
        String sql = "SELECT COUNT(*) FROM papers WHERE category IS NOT NULL AND category != ''";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.countAnalysedPapers() error: " + e.getMessage());
        }

        return 0;
    }

    // ================================================================
    // FAVORITES & READING STATUS
    // ================================================================

    /**
     * Toggles or sets the favorite flag for a paper.
     */
    public boolean setFavorite(int paperId, boolean favorite) {
        String sql = "UPDATE papers SET favorite = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, favorite ? 1 : 0);
            ps.setInt(2, paperId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PaperDAO.setFavorite() error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the reading status of a paper ("UNREAD", "READING", or "COMPLETED").
     */
    public boolean updateReadingStatus(int paperId, String readingStatus) {
        String sql = "UPDATE papers SET reading_status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (readingStatus == null || readingStatus.isBlank()) ? "UNREAD" : readingStatus);
            ps.setInt(2, paperId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PaperDAO.updateReadingStatus() error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns all papers marked as favorites.
     */
    public List<Paper> getFavoritePapers() {
        String sql = "SELECT * FROM papers WHERE favorite = 1 ORDER BY id DESC";
        List<Paper> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("PaperDAO.getFavoritePapers() error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Returns all papers with a specific reading status.
     */
    public List<Paper> findByStatus(String status) {
        String sql = "SELECT * FROM papers WHERE reading_status = ? ORDER BY id DESC";
        List<Paper> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("PaperDAO.findByStatus() error: " + e.getMessage());
        }
        return list;
    }

    // ================================================================
    // ADVANCED SEARCH
    // ================================================================

    /**
     * Advanced search across title, author, keyword, category, year, reading status, and favorite status.
     * All matching uses parameterized PreparedStatement to prevent SQL injection.
     */
    public List<Paper> searchAdvanced(String query, String category, int year, String readingStatus, Boolean favoriteOnly) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT p.* FROM papers p ");
        sql.append("LEFT JOIN keywords k ON p.id = k.paper_id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim() + "%";
            sql.append("AND (p.title LIKE ? OR p.authors LIKE ? OR p.abstract LIKE ? OR k.keyword LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("All")) {
            sql.append("AND p.category = ? ");
            params.add(category.trim());
        }

        if (year > 0) {
            sql.append("AND p.year = ? ");
            params.add(year);
        }

        if (readingStatus != null && !readingStatus.isBlank() && !readingStatus.equalsIgnoreCase("All")) {
            sql.append("AND p.reading_status = ? ");
            params.add(readingStatus.trim());
        }

        if (favoriteOnly != null && favoriteOnly) {
            sql.append("AND p.favorite = 1 ");
        }

        sql.append("ORDER BY p.id DESC");

        List<Paper> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s) {
                    ps.setString(i + 1, s);
                } else if (p instanceof Integer n) {
                    ps.setInt(i + 1, n);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.searchAdvanced() error: " + e.getMessage());
        }

        return list;
    }

    // ================================================================
    // EXTENDED DASHBOARD STATISTICS
    // ================================================================

    public int countFavoritePapers() {
        String sql = "SELECT COUNT(*) FROM papers WHERE favorite = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("PaperDAO.countFavoritePapers() error: " + e.getMessage());
        }
        return 0;
    }

    public int countPapersByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM papers WHERE reading_status = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("PaperDAO.countPapersByStatus() error: " + e.getMessage());
        }
        return 0;
    }

    public int countUnreadPapers() {
        return countPapersByStatus("UNREAD");
    }

    public int countReadingPapers() {
        return countPapersByStatus("READING");
    }

    public int countCompletedPapers() {
        return countPapersByStatus("COMPLETED");
    }

    /**
     * Returns paper counts grouped by category, ordered from highest to lowest.
     */
    public Map<String, Integer> getCategoryStatistics() {
        String sql = "SELECT category, COUNT(*) as cnt FROM papers " +
                     "WHERE category IS NOT NULL AND category != '' " +
                     "GROUP BY category ORDER BY cnt DESC";
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("category"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.err.println("PaperDAO.getCategoryStatistics() error: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Returns the most common keywords and their occurrence counts.
     */
    public Map<String, Integer> getTopKeywords(int limit) {
        String sql = "SELECT keyword, COUNT(*) as cnt FROM keywords " +
                     "GROUP BY keyword ORDER BY cnt DESC LIMIT ?";
        Map<String, Integer> top = new LinkedHashMap<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(rs.getString("keyword"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            System.err.println("PaperDAO.getTopKeywords() error: " + e.getMessage());
        }
        return top;
    }

    // ================================================================
    // PRIVATE HELPER
    // ================================================================

    /**
     * Converts one ResultSet row into a Paper object.
     */
    private Paper mapRow(ResultSet rs) throws SQLException {
        Paper paper = new Paper();
        paper.setId          (rs.getInt   ("id"));
        paper.setTitle       (rs.getString("title"));
        paper.setAuthors     (rs.getString("authors"));
        paper.setYear        (rs.getInt   ("year"));
        paper.setAbstractText(rs.getString("abstract"));
        paper.setMethodology (rs.getString("methodology"));
        paper.setFindings    (rs.getString("findings"));
        paper.setCategory    (rs.getString("category"));
        paper.setSource      (rs.getString("source"));
        paper.setFilePath    (rs.getString("file_path"));
        paper.setCreatedAt   (rs.getString("created_at"));

        try {
            paper.setFavorite(rs.getInt("favorite") == 1);
        } catch (SQLException ignored) {
            paper.setFavorite(false);
        }

        try {
            String status = rs.getString("reading_status");
            paper.setReadingStatus(status != null ? status : "UNREAD");
        } catch (SQLException ignored) {
            paper.setReadingStatus("UNREAD");
        }

        return paper;
    }
}