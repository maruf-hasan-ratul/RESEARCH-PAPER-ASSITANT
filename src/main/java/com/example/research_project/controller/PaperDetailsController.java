package com.example.researchassistant.controller;

import com.example.researchassistant.model.Note;
import com.example.researchassistant.model.Paper;
import com.example.researchassistant.service.PaperService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

/**
 * PaperDetailsController.java
 *
 * Displays all information about one paper:
 *   title, authors, year, category, source,
 *   abstract, methodology, findings, keywords, notes
 * Also allows adding/deleting notes.
 */
public class PaperDetailsController {

    @FXML private Label lblPageTitle;
    @FXML private Label lblTitle;
    @FXML private Label lblAuthors;
    @FXML private Label lblYear;
    @FXML private Label lblCategory;
    @FXML private Label lblSource;
    @FXML private Label lblAbstract;
    @FXML private Label lblMethodology;
    @FXML private Label lblFindings;
    @FXML private Label lblKeywords;
    @FXML private ListView<String> notesList;
    @FXML private TextField noteInput;

    private final PaperService paperService = new PaperService();
    private Paper currentPaper = null;

    @FXML
    public void initialize() {
        // Check if a paper was passed via scene userData
        // (set by PaperController before navigating here)
    }

    /**
     * JavaFX scene loading trick: after the FXML loads,
     * controllers elsewhere call this method to pass the paper.
     * We use scene.getUserData() to detect the paper.
     */
    public void setPaper(Paper paper) {
        this.currentPaper = paper;
        populateFields();
        loadNotes();
    }

    private void populateFields() {
        if (currentPaper == null) return;
        lblPageTitle.setText("Paper Details");
        lblTitle.setText(nvl(currentPaper.getTitle()));
        lblAuthors.setText(nvl(currentPaper.getAuthors()));
        lblYear.setText(currentPaper.getYear() > 0 ? String.valueOf(currentPaper.getYear()) : "-");
        lblCategory.setText(nvl(currentPaper.getCategory()));
        lblSource.setText(nvl(currentPaper.getSource()));
        lblAbstract.setText(nvl(currentPaper.getAbstractText()));
        lblMethodology.setText(nvl(currentPaper.getMethodology()));
        lblFindings.setText(nvl(currentPaper.getFindings()));

        List<String> kws = paperService.getKeywordsForPaper(currentPaper.getId());
        lblKeywords.setText(kws.isEmpty() ? "(none)" : String.join(", ", kws));
    }

    private void loadNotes() {
        if (currentPaper == null) return;
        List<Note> notes = paperService.getNotesForPaper(currentPaper.getId());
        List<String> display = notes.stream()
            .map(n -> "[" + nvl(n.getCreatedAt()) + "]  " + nvl(n.getNote()))
            .toList();
        notesList.setItems(FXCollections.observableArrayList(display));
    }

    @FXML
    private void onAddNote() {
        if (currentPaper == null) return;
        String text = noteInput.getText().trim();
        if (text.isEmpty()) {
            showAlert("Note cannot be empty.", Alert.AlertType.WARNING);
            return;
        }
        try {
            paperService.addNote(currentPaper.getId(), text);
            noteInput.clear();
            loadNotes();
        } catch (IllegalArgumentException e) {
            showAlert(e.getMessage(), Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void onDeleteNote() {
        int idx = notesList.getSelectionModel().getSelectedIndex();
        if (idx < 0) { showAlert("Select a note to delete.", Alert.AlertType.WARNING); return; }
        List<Note> notes = paperService.getNotesForPaper(currentPaper.getId());
        if (idx < notes.size()) {
            paperService.deleteNote(notes.get(idx).getId());
            loadNotes();
        }
    }

    @FXML
    private void onBack() { navigateTo("papers.fxml"); }

    @FXML
    private void onEdit() {
        if (currentPaper == null) return;
        lblTitle.getScene().setUserData(currentPaper);
        navigateTo("add-paper.fxml");
    }

    @FXML
    private void onAnalyse() {
        if (currentPaper == null) return;
        lblTitle.getScene().setUserData(currentPaper);
        navigateTo("analysis.fxml");
    }

    private void navigateTo(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                "/com/example/researchassistant/fxml/" + fxml));
            StackPane root = (StackPane) lblTitle.getScene().lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("PaperDetailsController navigate error: " + e.getMessage());
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}