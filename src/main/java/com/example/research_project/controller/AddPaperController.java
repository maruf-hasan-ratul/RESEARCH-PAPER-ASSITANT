package com.example.research_project.controller;

import com.example.research_project.model.Paper;
import com.example.research_project.service.PaperService;
import com.example.research_project.util.FileUtil;
import com.example.research_project.util.TaskUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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
    @FXML private Label     lblSaveStatus;

    private final PaperService paperService = new PaperService();
    private Paper editingPaper = null;

    private static final List<String> CATEGORIES = Arrays.asList(
        "Artificial Intelligence", "Machine Learning", "Computer Vision",
        "Natural Language Processing", "Cyber Security", "Data Science",
        "Software Engineering", "IoT", "Robotics", "Other"
    );

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList(CATEGORIES));
        Platform.runLater(() -> {
            if (titleField.getScene() != null && titleField.getScene().getUserData() instanceof Paper p) {
                setPaper(p);
                titleField.getScene().setUserData(null);
            }
        });
    }

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
        String title        = titleField.getText().trim();
        String authorsText  = authorsField.getText().trim();
        String yearText     = yearField.getText().trim();
        String category     = categoryCombo.getValue();
        String source       = sourceField.getText().trim();
        String abstractText = abstractArea.getText().trim();
        String methodology  = methodologyArea.getText().trim();
        String findings     = findingsArea.getText().trim();

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

        Paper paper = (editingPaper != null) ? editingPaper : new Paper();
        paper.setTitle(title);
        paper.setAuthors(authorsText);
        paper.setYear(year);
        paper.setCategory(category);
        paper.setSource(source);
        paper.setAbstractText(abstractText);
        paper.setMethodology(methodology);
        paper.setFindings(findings);

        setStatus("Saving…");

        final boolean isNew = (editingPaper == null);

        TaskUtil.run(
            () -> {
                if (isNew) {
                    return paperService.addPaper(paper);
                } else {
                    return paperService.updatePaper(paper) ? paper.getId() : -1;
                }
            },
            resultId -> {
                if (resultId > 0) {
                    setStatus(isNew ? "Saved (ID: " + resultId + ")." : "Updated successfully.");
                    showAlert(isNew ? "Paper saved! (ID: " + resultId + ")" : "Paper updated successfully.",
                            Alert.AlertType.INFORMATION);
                    if (isNew) onClear();
                    navigateTo("papers.fxml");
                } else {
                    setStatus("Save failed.");
                    showAlert(isNew ? "Failed to save paper." : "Update failed.", Alert.AlertType.ERROR);
                }
            },
            err -> {
                setStatus("Save failed.");
                showAlert(err != null ? err.getMessage() : "Save failed.", Alert.AlertType.ERROR);
            }
        );
    }

    @FXML
    private void onImportFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Paper from Text or PDF File");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Supported Files (*.txt, *.pdf)", "*.txt", "*.pdf"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fc.showOpenDialog(titleField.getScene().getWindow());
        if (file == null) return;

        String fileName = file.getName().toLowerCase();
        setStatus("Importing…");

        if (fileName.endsWith(".pdf")) {
            TaskUtil.run(
                () -> paperService.importPaperFromPdf(file),
                imported -> {
                    if (imported.getTitle()        != null) titleField.setText(imported.getTitle());
                    if (imported.getAuthors()      != null) authorsField.setText(imported.getAuthors());
                    if (imported.getYear()         > 0)     yearField.setText(String.valueOf(imported.getYear()));
                    if (imported.getCategory()     != null) categoryCombo.setValue(imported.getCategory());
                    if (imported.getSource()       != null) sourceField.setText(imported.getSource());
                    if (imported.getAbstractText() != null) abstractArea.setText(imported.getAbstractText());
                    if (imported.getMethodology()  != null) methodologyArea.setText(imported.getMethodology());
                    if (imported.getFindings()     != null) findingsArea.setText(imported.getFindings());

                    editingPaper = imported;
                    pageTitle.setText("Edit Paper (Imported from PDF)");
                    setStatus("PDF imported (ID: " + imported.getId() + ").");
                    showAlert("PDF imported and saved! (ID: " + imported.getId() + ")\n" +
                              "Review the fields and click Save to confirm.", Alert.AlertType.INFORMATION);
                },
                err -> {
                    setStatus("Import failed.");
                    showAlert("Could not read PDF file: " + (err != null ? err.getMessage() : "unknown"),
                            Alert.AlertType.ERROR);
                }
            );
        } else {
            TaskUtil.run(
                () -> FileUtil.parseFromFile(file.getAbsolutePath()),
                parsed -> {
                    if (parsed == null) {
                        setStatus("Import failed.");
                        showAlert("Could not parse file. Make sure it follows the expected format.",
                                Alert.AlertType.WARNING);
                        return;
                    }
                    if (parsed.getTitle()        != null) titleField.setText(parsed.getTitle());
                    if (parsed.getAuthors()      != null) authorsField.setText(parsed.getAuthors());
                    if (parsed.getYear()         > 0)     yearField.setText(String.valueOf(parsed.getYear()));
                    if (parsed.getAbstractText() != null) abstractArea.setText(parsed.getAbstractText());
                    if (parsed.getMethodology()  != null) methodologyArea.setText(parsed.getMethodology());
                    if (parsed.getFindings()     != null) findingsArea.setText(parsed.getFindings());
                    setStatus("File imported.");
                },
                err -> {
                    setStatus("Import failed.");
                    showAlert("Import error: " + (err != null ? err.getMessage() : "unknown"),
                            Alert.AlertType.ERROR);
                }
            );
        }
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
        setStatus("");
    }

    @FXML
    private void onCancel() {
        navigateTo("papers.fxml");
    }

    private void setStatus(String msg) {
        if (lblSaveStatus != null) lblSaveStatus.setText(msg);
    }

    private void navigateTo(String fxml) {
        try {
            URL loc = getClass().getResource("/com/example/research_project/fxml/" + fxml);
            if (loc == null) {
                loc = getClass().getResource("/com/example/researchassistant/fxml/" + fxml);
            }
            Node view = FXMLLoader.load(loc);
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