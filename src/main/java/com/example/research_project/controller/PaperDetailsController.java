package com.example.research_project.controller;

import com.example.research_project.model.Note;
import com.example.research_project.model.Paper;
import com.example.research_project.service.CitationService;
import com.example.research_project.service.PaperService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * PaperDetailsController.java
 *
 * Displays all information about one paper:
 *   - title, authors, year, category, source
 *   - abstract, methodology, findings, keywords, notes
 *   - favorite toggle button (☆ / ★)
 *   - reading status dropdown (UNREAD, READING, COMPLETED)
 *   - academic citation generator (IEEE, APA, MLA, BibTeX)
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

    // Favorite & Reading Status controls
    @FXML private Button btnFavorite;
    @FXML private ComboBox<String> comboReadingStatus;

    // Citation controls
    @FXML private ComboBox<CitationService.CitationStyle> comboCitationStyle;
    @FXML private TextArea txtCitation;
    @FXML private Label lblCitationFeedback;

    // Notes controls
    @FXML private ListView<String> notesList;
    @FXML private TextField noteInput;

    private final PaperService paperService = new PaperService();
    private final CitationService citationService = new CitationService();
    private Paper currentPaper = null;

    @FXML
    public void initialize() {
        if (comboReadingStatus != null) {
            comboReadingStatus.setItems(FXCollections.observableArrayList("UNREAD", "READING", "COMPLETED"));
        }

        if (comboCitationStyle != null) {
            comboCitationStyle.setItems(FXCollections.observableArrayList(CitationService.CitationStyle.values()));
            comboCitationStyle.setValue(CitationService.CitationStyle.IEEE);
        }

        Platform.runLater(() -> {
            if (lblTitle.getScene() != null && lblTitle.getScene().getUserData() instanceof Paper p) {
                setPaper(p);
            }
        });
    }

    /**
     * Sets the paper to be displayed in details.
     */
    public void setPaper(Paper paper) {
        this.currentPaper = paper;
        populateFields();
        loadNotes();
        updateCitation();
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

        // Update Favorite UI
        updateFavoriteButtonState();

        // Update Reading Status
        if (comboReadingStatus != null) {
            comboReadingStatus.setValue(currentPaper.getReadingStatus());
        }

        List<String> kws = paperService.getKeywordsForPaper(currentPaper.getId());
        lblKeywords.setText(kws.isEmpty() ? "(none)" : String.join(", ", kws));
    }

    private void updateFavoriteButtonState() {
        if (btnFavorite == null || currentPaper == null) return;
        if (currentPaper.isFavorite()) {
            btnFavorite.setText("★ In Favorites");
            btnFavorite.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        } else {
            btnFavorite.setText("☆ Add to Favorites");
            btnFavorite.setStyle("-fx-background-color: #334155; -fx-text-fill: #F8FAFC; -fx-font-weight: 500;");
        }
    }

    @FXML
    private void onToggleFavorite() {
        if (currentPaper == null) return;
        boolean newFav = !currentPaper.isFavorite();
        boolean ok = paperService.setFavorite(currentPaper.getId(), newFav);
        if (ok) {
            currentPaper.setFavorite(newFav);
            updateFavoriteButtonState();
        } else {
            showAlert("Could not update favorite status.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onStatusChanged() {
        if (currentPaper == null || comboReadingStatus == null) return;
        String newStatus = comboReadingStatus.getValue();
        if (newStatus != null && !newStatus.equals(currentPaper.getReadingStatus())) {
            boolean ok = paperService.updateReadingStatus(currentPaper.getId(), newStatus);
            if (ok) {
                currentPaper.setReadingStatus(newStatus);
            } else {
                showAlert("Could not update reading status.", Alert.AlertType.ERROR);
            }
        }
    }

    // ================================================================
    // CITATIONS
    // ================================================================

    @FXML
    private void onCitationStyleChanged() {
        updateCitation();
    }

    private void updateCitation() {
        if (currentPaper == null || txtCitation == null || comboCitationStyle == null) return;
        CitationService.CitationStyle style = comboCitationStyle.getValue();
        if (style == null) style = CitationService.CitationStyle.IEEE;
        String citation = citationService.generateCitation(currentPaper, style);
        txtCitation.setText(citation);
        if (lblCitationFeedback != null) {
            lblCitationFeedback.setText("");
        }
    }

    @FXML
    private void onCopyCitation() {
        if (txtCitation == null || txtCitation.getText().isBlank()) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(txtCitation.getText());
        clipboard.setContent(content);

        if (lblCitationFeedback != null) {
            lblCitationFeedback.setText("✓ Citation copied to clipboard!");
            lblCitationFeedback.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    // ================================================================
    // NOTES
    // ================================================================

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
            URL loc = getClass().getResource("/com/example/research_project/fxml/" + fxml);
            if (loc == null) {
                loc = getClass().getResource("/com/example/researchassistant/fxml/" + fxml);
            }
            Node view = FXMLLoader.load(loc);
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