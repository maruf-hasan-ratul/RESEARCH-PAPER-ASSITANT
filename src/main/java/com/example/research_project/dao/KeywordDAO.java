package com.example.research_project.dao;

import com.example.research_project.database.Database;
import com.example.research_project.model.Keyword;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * KeywordDAO.java - All database operations for the 'keywords' table.
 *
 * Each keyword is linked to a paper via paper_id (foreign key).
 * Deleting a paper deletes associated keywords via ON DELETE CASCADE.
 */
public class KeywordDAO {

    /**
     * Inserts a list of keywords for a given paper.
     *
     * @param paperId  The id of the paper
     * @param keywords List of keyword strings
     */
    public void addKeywords(int paperId, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return;

        String sql = "INSERT INTO keywords (paper_id, keyword) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String kw : keywords) {
                if (kw != null && !kw.isBlank()) {
                    ps.setInt(1, paperId);
                    ps.setString(2, kw.trim());
                    ps.addBatch();
                }
            }
            ps.executeBatch();

        } catch (SQLException e) {
            System.err.println("KeywordDAO.addKeywords() error: " + e.getMessage());
        }
    }

    /**
     * Retrieves all keywords for a paper as a list of strings.
     *
     * @param paperId The id of the paper
     * @return List of keyword strings
     */
    public List<String> getKeywordStringsByPaperId(int paperId) {
        String sql = "SELECT keyword FROM keywords WHERE paper_id = ? ORDER BY id ASC";
        List<String> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paperId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString("keyword"));
                }
            }

        } catch (SQLException e) {
            System.err.println("KeywordDAO.getKeywordStringsByPaperId() error: " + e.getMessage());
        }

        return results;
    }

    /**
     * Retrieves all keywords for a paper as Keyword model objects.
     *
     * @param paperId The id of the paper
     * @return List of Keyword model objects
     */
    public List<Keyword> getKeywordsByPaperId(int paperId) {
        String sql = "SELECT id, paper_id, keyword FROM keywords WHERE paper_id = ? ORDER BY id ASC";
        List<Keyword> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paperId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Keyword kw = new Keyword();
                    kw.setId(rs.getInt("id"));
                    kw.setPaperId(rs.getInt("paper_id"));
                    kw.setKeyword(rs.getString("keyword"));
                    results.add(kw);
                }
            }

        } catch (SQLException e) {
            System.err.println("KeywordDAO.getKeywordsByPaperId() error: " + e.getMessage());
        }

        return results;
    }

    /**
     * Deletes all keywords associated with a specific paper.
     *
     * @param paperId The paper id
     */
    public void deleteKeywordsByPaperId(int paperId) {
        String sql = "DELETE FROM keywords WHERE paper_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paperId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("KeywordDAO.deleteKeywordsByPaperId() error: " + e.getMessage());
        }
    }
}
