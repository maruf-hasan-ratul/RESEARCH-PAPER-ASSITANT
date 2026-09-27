package com.example.research_project.dao;

import com.example.research_project.database.Database;
import com.example.research_project.model.Paper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                " category, source, file_path, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                "findings=?, category=?, source=?, file_path=? " +
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
            ps.setInt   (10, paper.getId());

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
    // PRIVATE HELPER
    // ================================================================

    /**
     * Converts one ResultSet row into a Paper object.
     *
     * CALLED BY: getPaperById, getAllPapers, searchPapers, filterPapers
     *
     * WHY A SEPARATE METHOD?
     * Without this, every method would repeat the same 10 lines of
     * rs.getString("title"), rs.getInt("year"), etc.
     * Putting it here means we only write it once (DRY principle).
     *
     * @param rs  A ResultSet positioned at a valid row
     * @return    A fully populated Paper object
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
        return paper;
    }
}