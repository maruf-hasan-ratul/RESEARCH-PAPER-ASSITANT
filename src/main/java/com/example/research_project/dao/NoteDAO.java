package com.example.research_project.dao;

import com.example.research_project.database.Database;
import com.example.research_project.model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    public void addNote(Note note) {
        String sql = "INSERT INTO notes (paper_id, note, created_at) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, note.getPaperId());
            ps.setString(2, note.getNote());
            ps.setString(3, note.getCreatedAt());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    note.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            System.err.println("NoteDAO.addNote() error: " + e.getMessage());
        }
    }

    public List<Note> getNotesByPaperId(int paperId) {
        String sql = "SELECT * FROM notes WHERE paper_id = ? ORDER BY id ASC";
        List<Note> results = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, paperId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note note = new Note();
                    note.setId       (rs.getInt   ("id"));
                    note.setPaperId  (rs.getInt   ("paper_id"));
                    note.setNote     (rs.getString("note"));
                    note.setCreatedAt(rs.getString("created_at"));
                    results.add(note);
                }
            }

        } catch (SQLException e) {
            System.err.println("NoteDAO.getNotesByPaperId() error: " + e.getMessage());
        }

        return results;
    }

    public void deleteNote(int noteId) {
        String sql = "DELETE FROM notes WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, noteId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("NoteDAO.deleteNote() error: " + e.getMessage());
        }
    }

    public int countNotes() {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.countNotes() error: " + e.getMessage());
        }
        return 0;
    }
}