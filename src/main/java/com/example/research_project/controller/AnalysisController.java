package com.example.research_project.controller;

import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;
import com.example.research_project.model.SimilarityResult;
import com.example.research_project.service.AIService;
import com.example.research_project.service.PaperService;
import com.example.research_project.service.SimilarityService;
import com.example.research_project.task.AnalysisTask;
import com.example.research_project.util.FileUtil;
import com.example.research_project.util.JsonUtil;
import com.example.research_project.util.TaskUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * AnalysisController.java
 *
 * Controls the Analysis screen.
 * Supports Local Ollama (Qwen 2.5 3B) LLM synthesis with algorithmic NLP fallback.
 *
 * KEY FEATURE: multithreading
 * When the user clicks "Analyse", we create an AnalysisTask (extends Task)
 * and run it in a background thread. This keeps the UI responsive.
 * Progress and status are bound to ProgressBar and Label automatically.
 */
public class AnalysisController {

    @FXML private ComboBox<Paper>  paperCombo;
    @FXML private ProgressBar      progressBar;
    @FXML private Label            lblStatus;
    @FXML private TextArea         summaryArea;
    @FXML private TextArea         keywordsArea;
    @FXML private Label            lblCategory;
    @FXML private TextArea         methodologyArea;
    @FXML private TextArea         findingsArea;
    @FXML private ComboBox<Paper>  compareCombo;
    @FXML private Label            lblSimilarity;

    // Ollama AI controls
    @FXML private CheckBox         chkUseOllama;
    @FXML private Label            lblAiStatus;
    @FXML private Label            lblSummaryBadge;
    @FXML private Button           btnConfigureOllama;

    private final PaperService      paperService      = new PaperService();
    private final SimilarityService similarityService = new SimilarityService();
    private final AIService         aiService         = new AIService();

    private AnalysisResult lastResult = null;

    @FXML
    public void initialize() {
        refreshAiStatus();
        loadPapersAsync();
    }

    /**
     * Loads papers from the DB in a background thread, then populates both
     * combo boxes on the FX thread.  After population, applies any pre-selected
     * paper that was passed via scene userData (e.g. from the Papers screen).
     */
    private void loadPapersAsync() {
        TaskUtil.run(
            () -> paperService.getAllPapers(),
            papers -> {
                javafx.util.StringConverter<Paper> converter = new javafx.util.StringConverter<>() {
                    @Override public String toString(Paper p) {
                        return p == null ? "" : "[" + p.getId() + "] " + p.getTitle();
                    }
                    @Override public Paper fromString(String s) { return null; }
                };

                paperCombo.setItems(FXCollections.observableArrayList(papers));
                compareCombo.setItems(FXCollections.observableArrayList(papers));
                paperCombo.setConverter(converter);
                compareCombo.setConverter(converter);

                // Apply pre-selected paper from scene userData (if any)
                if (paperCombo.getScene() != null) {
                    Object userData = paperCombo.getScene().getUserData();
                    if (userData instanceof Paper p) {
                        for (Paper item : papers) {
                            if (item.getId() == p.getId()) {
                                paperCombo.setValue(item);
                                break;
                            }
                        }
                    }
                }
            },
            err -> System.err.println("AnalysisController loadPapers error: " +
                    (err != null ? err.getMessage() : "?"))
        );
    }

    private void refreshAiStatus() {
        if (aiService.isAvailable()) {
            lblAiStatus.setText("● Ollama (" + aiService.getModel() + "): Online");
            lblAiStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            chkUseOllama.setSelected(true);
            chkUseOllama.setDisable(false);
        } else {
            lblAiStatus.setText("● Ollama: Offline (Click ⚙️ to check/configure)");
            lblAiStatus.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px; -fx-font-weight: bold;");
            chkUseOllama.setSelected(false);
        }
    }

    @FXML
    private void onConfigureOllama() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Local Ollama Configuration");
        dialog.setHeaderText("Configure Local Ollama Host & Model");

        ButtonType saveButtonType = new ButtonType("Save & Test", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField txtHost = new TextField(aiService.getHost());
        TextField txtModel = new TextField(aiService.getModel());

        grid.add(new Label("Ollama Host URL:"), 0, 0);
        grid.add(txtHost, 1, 0);
        grid.add(new Label("Model Name:"), 0, 1);
        grid.add(txtModel, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String newHost = txtHost.getText().trim();
            String newModel = txtModel.getText().trim();

            if (newHost.isBlank()) newHost = AIService.DEFAULT_HOST;
            if (newModel.isBlank()) newModel = AIService.DEFAULT_MODEL;

            try {
                aiService.saveSettings(newHost, newModel);
                String testResult = aiService.testConnection();
                refreshAiStatus();

                if ("OK".equals(testResult)) {
                    showAlert("Ollama connected successfully!\nModel '" + newModel + "' is ready.", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Ollama check result:\n" + testResult, Alert.AlertType.WARNING);
                }
            } catch (IOException e) {
                showAlert("Failed to save config: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void onAnalyse() {
        Paper selected = paperCombo.getValue();
        if (selected == null) {
            showAlert("Please select a paper to analyse.", Alert.AlertType.WARNING);
            return;
        }

        // Reset UI
        clearResults();
        lblStatus.setText("Starting analysis...");

        boolean useOllama = chkUseOllama != null && chkUseOllama.isSelected();

        // Create the background task
        AnalysisTask task = new AnalysisTask(selected, useOllama, aiService);

        // Bind progress bar and status label to the task
        progressBar.progressProperty().bind(task.progressProperty());
        lblStatus.textProperty().bind(task.messageProperty());

        // When done: update UI on the JavaFX thread
        task.setOnSucceeded(e -> {
            // Unbind so we can set values manually
            progressBar.progressProperty().unbind();
            lblStatus.textProperty().unbind();

            lastResult = task.getValue();
            displayResults(lastResult);

            // Save keywords and category back to the paper
            paperService.updateKeywords(selected.getId(), lastResult.getKeywords());
            selected.setCategory(lastResult.getCategory());
            paperService.updatePaper(selected);
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            lblStatus.textProperty().unbind();
            lblStatus.setText("Analysis failed: " + task.getException().getMessage());
        });

        // Run in a new daemon thread
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void displayResults(AnalysisResult result) {
        summaryArea.setText(result.getSummary() != null ? result.getSummary() : "-");

        if (result.getKeywords() != null && !result.getKeywords().isEmpty()) {
            keywordsArea.setText(String.join("\n", result.getKeywords()));
        } else {
            keywordsArea.setText("-");
        }

        lblCategory.setText(result.getCategory() != null ? result.getCategory() : "-");
        methodologyArea.setText(result.getMethodology() != null ? result.getMethodology() : "-");
        findingsArea.setText(result.getFindings() != null ? result.getFindings() : "-");

        if (lblSummaryBadge != null) {
            if (result.getConfidence() >= 0.9) {
                lblSummaryBadge.setText("✨ Ollama " + aiService.getModel());
                lblSummaryBadge.setStyle("-fx-background-color: #EEF2FF; -fx-text-fill: #4338CA; -fx-background-radius: 10px; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else {
                lblSummaryBadge.setText("NLP Extracted");
                lblSummaryBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 10px; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
        }
    }

    private void clearResults() {
        summaryArea.clear();
        keywordsArea.clear();
        lblCategory.setText("-");
        methodologyArea.clear();
        findingsArea.clear();
        lblSimilarity.setText("");
        lastResult = null;
        if (lblSummaryBadge != null) {
            lblSummaryBadge.setText("Pending");
            lblSummaryBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-background-radius: 10px; -fx-padding: 2 8; -fx-font-size: 10px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void onCompare() {
        Paper a = paperCombo.getValue();
        Paper b = compareCombo.getValue();
        if (a == null || b == null) {
            showAlert("Select both papers to compare.", Alert.AlertType.WARNING);
            return;
        }
        if (a.getId() == b.getId()) {
            showAlert("Select two different papers.", Alert.AlertType.WARNING);
            return;
        }

        SimilarityResult sim = similarityService.compare(a, b);
        lblSimilarity.setText(sim.getScoreAsPercent());
    }

    @FXML
    private void onSaveResults() {
        Paper p = paperCombo.getValue();
        if (p == null || lastResult == null) {
            showAlert("Run analysis first.", Alert.AlertType.WARNING);
            return;
        }
        p.setCategory(lastResult.getCategory());
        if (lastResult.getMethodology() != null && !lastResult.getMethodology().isBlank())
            p.setMethodology(lastResult.getMethodology());
        if (lastResult.getFindings() != null && !lastResult.getFindings().isBlank())
            p.setFindings(lastResult.getFindings());

        paperService.updatePaper(p);
        paperService.updateKeywords(p.getId(), lastResult.getKeywords());
        showAlert("Results saved to paper!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void onExportJson() {
        Paper p = paperCombo.getValue();
        if (p == null || lastResult == null) {
            showAlert("Run analysis first.", Alert.AlertType.WARNING);
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Analysis as JSON");
        fc.setInitialFileName("analysis_" + p.getId() + ".json");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fc.showSaveDialog(summaryArea.getScene().getWindow());
        if (file == null) return;

        String json = JsonUtil.exportFull(p, lastResult.getKeywords(),
            lastResult.getSummary(), lastResult.getCategory());
        if (FileUtil.writeTextFile(file.getAbsolutePath(), json)) {
            showAlert("Exported to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } else {
            showAlert("Export failed.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Platform.runLater(() -> new Alert(type, msg, ButtonType.OK).showAndWait());
    }
}