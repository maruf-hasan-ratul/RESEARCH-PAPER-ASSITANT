package com.example.research_project.dao;

import com.example.research_project.database.Database;
import com.example.research_project.model.Paper;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaperDAO {

    public int addPaper(Paper paper) {
        String sql = "INSERT INTO papers " +
                "(title, authors, year, abstract, methodology, findings, " +
                " category, source, file_path, created_at, favorite, reading_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    paper.setId(newId);
                    return newId;
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.addPaper() error: " + e.getMessage());
        }

        return -1;
    }

    public Paper getPaperById(int id) {
        String sql = "SELECT * FROM papers WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.getPaperById() error: " + e.getMessage());
        }

        return null;
    }

    public List<Paper> getAllPapers() {
        String sql = "SELECT * FROM papers ORDER BY id DESC";
        List<Paper> papers = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                papers.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("PaperDAO.getAllPapers() error: " + e.getMessage());
        }

        return papers;
    }

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
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("PaperDAO.updatePaper() error: " + e.getMessage());
        }

        return false;
    }

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

    public List<Paper> searchPapers(String keyword) {
        String sql = "SELECT * FROM papers " +
                "WHERE title LIKE ? OR authors LIKE ? " +
                "ORDER BY id DESC";
        List<Paper> results = new ArrayList<>();

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

    public List<Paper> filterPapers(String category, int year) {
        StringBuilder sql = new StringBuilder("SELECT * FROM papers WHERE 1=1");

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