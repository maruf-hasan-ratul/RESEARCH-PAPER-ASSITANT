package com.example.research_project.controller;

import com.example.research_project.model.Note;
import com.example.research_project.model.Paper;
import com.example.research_project.service.CitationService;
import com.example.research_project.service.PaperService;
import com.example.research_project.util.TaskUtil;
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

    @FXML private Button btnFavorite;
    @FXML private ComboBox<String> comboReadingStatus;

    @FXML private ComboBox<CitationService.CitationStyle> comboCitationStyle;
    @FXML private TextArea txtCitation;
    @FXML private Label lblCitationFeedback;

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

    public void setPaper(Paper paper) {
        this.currentPaper = paper;
        populateStaticFields();
        loadKeywordsAsync();
        loadNotesAsync();
        updateCitation();
    }

    private void populateStaticFields() {
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

        updateFavoriteButtonState();

        if (comboReadingStatus != null) {
            comboReadingStatus.setValue(currentPaper.getReadingStatus());
        }
    }

    private void loadKeywordsAsync() {
        if (currentPaper == null) return;
        lblKeywords.setText("Loading…");
        TaskUtil.run(
            () -> paperService.getKeywordsForPaper(currentPaper.getId()),
            kws -> lblKeywords.setText(kws.isEmpty() ? "(none)" : String.join(", ", kws)),
            err -> lblKeywords.setText("(error loading keywords)")
        );
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
        TaskUtil.run(
            () -> paperService.setFavorite(currentPaper.getId(), newFav),
            ok -> {
                if (ok) {
                    currentPaper.setFavorite(newFav);
                    updateFavoriteButtonState();
                } else {
                    showAlert("Could not update favorite status.", Alert.AlertType.ERROR);
                }
            },
            err -> showAlert("Error: " + (err != null ? err.getMessage() : "unknown"), Alert.AlertType.ERROR)
        );
    }

    @FXML
    private void onStatusChanged() {
        if (currentPaper == null || comboReadingStatus == null) return;
        String newStatus = comboReadingStatus.getValue();
        if (newStatus != null && !newStatus.equals(currentPaper.getReadingStatus())) {
            TaskUtil.run(
                () -> paperService.updateReadingStatus(currentPaper.getId(), newStatus),
                ok -> {
                    if (ok) {
                        currentPaper.setReadingStatus(newStatus);
                    } else {
                        showAlert("Could not update reading status.", Alert.AlertType.ERROR);
                    }
                },
                err -> showAlert("Error: " + (err != null ? err.getMessage() : "unknown"), Alert.AlertType.ERROR)
            );
        }
    }

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

    private void loadNotesAsync() {
        if (currentPaper == null) return;
        TaskUtil.run(
            () -> paperService.getNotesForPaper(currentPaper.getId()),
            notes -> {
                List<String> display = notes.stream()
                        .map(n -> "[" + nvl(n.getCreatedAt()) + "]  " + nvl(n.getNote()))
                        .toList();
                notesList.setItems(FXCollections.observableArrayList(display));
            },
            err -> System.err.println("PaperDetailsController loadNotes error: " +
                    (err != null ? err.getMessage() : "?"))
        );
    }

    @FXML
    private void onAddNote() {
        if (currentPaper == null) return;
        String text = noteInput.getText().trim();
        if (text.isEmpty()) {
            showAlert("Note cannot be empty.", Alert.AlertType.WARNING);
            return;
        }
        noteInput.clear();
        TaskUtil.run(
            (java.util.concurrent.Callable<Void>) () -> {
                paperService.addNote(currentPaper.getId(), text);
                return null;
            },
            ignored -> loadNotesAsync(),
            err -> {
                noteInput.setText(text);
                showAlert(err != null ? err.getMessage() : "Failed to add note.", Alert.AlertType.WARNING);
            }
        );
    }

    @FXML
    private void onDeleteNote() {
        int idx = notesList.getSelectionModel().getSelectedIndex();
        if (idx < 0) { showAlert("Select a note to delete.", Alert.AlertType.WARNING); return; }

        TaskUtil.run(
            () -> {
                List<Note> notes = paperService.getNotesForPaper(currentPaper.getId());
                if (idx < notes.size()) {
                    paperService.deleteNote(notes.get(idx).getId());
                }
                return null;
            },
            ignored -> loadNotesAsync(),
            err -> showAlert("Failed to delete note: " +
                    (err != null ? err.getMessage() : "unknown"), Alert.AlertType.ERROR)
        );
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