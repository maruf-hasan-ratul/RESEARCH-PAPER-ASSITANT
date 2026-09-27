package com.example.researchassistant.controller;

import com.example.researchassistant.model.Paper;
import com.example.researchassistant.service.PaperService;
import com.example.researchassistant.util.FileUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * AddPaperController.java
 *
 * Controls the Add/Edit paper form.
 * Handles:
 *   - New paper creation
 *   - Editing an existing paper (when scene userData is a Paper)
 *   - Importing a .txt file to pre-fill the form
 *   - Validation before saving
 */
public class AddPaperController {

    @FXML private Label     pageTitle;
    @FXML private TextField titleField;
    @FXML private TextField yearField;
    @FXML private TextField authorsField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField sourceField;
    @FXML private TextArea  abstractArea;
    @FXML private TextArea  methodologyArea;
    @FXML private TextArea  findingsArea;

    private final PaperService paperService = new PaperService();
    private Paper editingPaper = null; // null = new paper, non-null = editing

    private static final List<String> CATEGORIES = Arrays.asList(
        "Artificial Intelligence", "Machine Learning", "Computer Vision",
        "Natural Language Processing", "Cyber Security", "Data Science",
        "Software Engineering", "IoT", "Robotics", "Other"
    );

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList(CATEGORIES));
    }

    /**
     * Called by PaperController when editing an existing paper.
     * Pre-fills all form fields with the paper's current values.
     */
    public void setPaper(Paper paper) {
        this.editingPaper = paper;
        pageTitle.setText("Edit Paper");
        titleField.setText(paper.getTitle());
        authorsField.setText(paper.getAuthors());
        yearField.setText(paper.getYear() > 0 ? String.valueOf(paper.getYear()) : "");
        categoryCombo.setValue(paper.getCategory());
        sourceField.setText(paper.getSource());
        abstractArea.setText(paper.getAbstractText());
        methodologyArea.setText(paper.getMethodology());
        findingsArea.setText(paper.getFindings());
    }

    @FXML
    private void onSave() {
        // 1. Read and validate inputs
        String title = titleField.getText().trim();
        String authorsText = authorsField.getText().trim();
        String yearText = yearField.getText().trim();
        String category = categoryCombo.getValue();
        String source = sourceField.getText().trim();
        String abstractText = abstractArea.getText().trim();
        String methodology = methodologyArea.getText().trim();
        String findings = findingsArea.getText().trim();

        if (title.isEmpty()) {
            showAlert("Title cannot be empty.", Alert.AlertType.WARNING);
            titleField.requestFocus();
            return;
        }

        int year = 0;
        if (!yearText.isEmpty()) {
            try {
                year = Integer.parseInt(yearText);
            } catch (NumberFormatException e) {
                showAlert("Year must be a number (e.g. 2024).", Alert.AlertType.WARNING);
                yearField.requestFocus();
                return;
            }
        }

        // 2. Build the Paper object
        Paper paper = (editingPaper != null) ? editingPaper : new Paper();
        paper.setTitle(title);
        paper.setAuthors(authorsText);
        paper.setYear(year);
        paper.setCategory(category);
        paper.setSource(source);
        paper.setAbstractText(abstractText);
        paper.setMethodology(methodology);
        paper.setFindings(findings);

        try {
            // 3. Save or update
            if (editingPaper == null) {
                int newId = paperService.addPaper(paper);
                if (newId > 0) {
                    showAlert("Paper saved! (ID: " + newId + ")", Alert.AlertType.INFORMATION);
                    onClear();
                    navigateTo("papers.fxml");
                } else {
                    showAlert("Failed to save paper.", Alert.AlertType.ERROR);
                }
            } else {
                boolean ok = paperService.updatePaper(paper);
                if (ok) {
                    showAlert("Paper updated successfully.", Alert.AlertType.INFORMATION);
                    navigateTo("papers.fxml");
                } else {
                    showAlert("Update failed.", Alert.AlertType.ERROR);
                }
            }
        } catch (IllegalArgumentException e) {
            showAlert(e.getMessage(), Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void onImportFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Paper from Text File");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File file = fc.showOpenDialog(titleField.getScene().getWindow());
        if (file == null) return; // user cancelled

        // Parse sections from the file
        Paper parsed = FileUtil.parseFromFile(file.getAbsolutePath());
        if (parsed == null) {
            showAlert("Could not parse file. Make sure it follows the expected format.", Alert.AlertType.WARNING);
            return;
        }

        // Pre-fill the form
        if (parsed.getTitle()       != null) titleField.setText(parsed.getTitle());
        if (parsed.getAuthors()     != null) authorsField.setText(parsed.getAuthors());
        if (parsed.getYear()        > 0)     yearField.setText(String.valueOf(parsed.getYear()));
        if (parsed.getAbstractText()!= null) abstractArea.setText(parsed.getAbstractText());
        if (parsed.getMethodology() != null) methodologyArea.setText(parsed.getMethodology());
        if (parsed.getFindings()    != null) findingsArea.setText(parsed.getFindings());
    }

    @FXML
    private void onClear() {
        editingPaper = null;
        pageTitle.setText("Add New Paper");
        titleField.clear();
        authorsField.clear();
        yearField.clear();
        categoryCombo.setValue(null);
        sourceField.clear();
        abstractArea.clear();
        methodologyArea.clear();
        findingsArea.clear();
    }

    @FXML
    private void onCancel() {
        navigateTo("papers.fxml");
    }

    private void navigateTo(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                "/com/example/researchassistant/fxml/" + fxml));
            StackPane root = (StackPane) titleField.getScene().lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("AddPaperController navigate error: " + e.getMessage());
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}