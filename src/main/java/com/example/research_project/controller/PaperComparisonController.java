package com.example.research_project.controller;

import com.example.research_project.model.Paper;
import com.example.research_project.model.SimilarityResult;
import com.example.research_project.service.PaperService;
import com.example.research_project.service.SimilarityService;
import com.example.research_project.util.TaskUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PaperComparisonController {

    @FXML private ComboBox<Paper> comboPaperA;
    @FXML private ComboBox<Paper> comboPaperB;

    @FXML private VBox resultsBox;
    @FXML private Label lblSimilarityScore;
    @FXML private ProgressBar progressSimilarity;
    @FXML private Label lblCompareStatus;

    @FXML private Label lblCommonKeywords;
    @FXML private Label lblKeywordsA;
    @FXML private Label lblKeywordsB;

    @FXML private Label lblTitleA;
    @FXML private Label lblAuthorsA;
    @FXML private Label lblYearA;
    @FXML private Label lblCategoryA;
    @FXML private Label lblSourceA;
    @FXML private Label lblSummaryA;

    @FXML private Label lblTitleB;
    @FXML private Label lblAuthorsB;
    @FXML private Label lblYearB;
    @FXML private Label lblCategoryB;
    @FXML private Label lblSourceB;
    @FXML private Label lblSummaryB;

    private final PaperService paperService = new PaperService();
    private final SimilarityService similarityService = new SimilarityService();

    @FXML
    public void initialize() {
        loadPapersAsync();
    }

    private void loadPapersAsync() {
        TaskUtil.run(
            () -> paperService.getAllPapers(),
            all -> {
                StringConverter<Paper> converter = new StringConverter<>() {
                    @Override public String toString(Paper p) {
                        return p == null ? "" : "[" + p.getId() + "] " + p.getTitle();
                    }
                    @Override public Paper fromString(String s) { return null; }
                };
                comboPaperA.setItems(FXCollections.observableArrayList(all));
                comboPaperB.setItems(FXCollections.observableArrayList(all));
                comboPaperA.setConverter(converter);
                comboPaperB.setConverter(converter);
                if (all.size() >= 2) {
                    comboPaperA.setValue(all.get(0));
                    comboPaperB.setValue(all.get(1));
                } else if (all.size() == 1) {
                    comboPaperA.setValue(all.get(0));
                }
            },
            err -> System.err.println("PaperComparisonController loadPapers error: " +
                    (err != null ? err.getMessage() : "?"))
        );
    }

    private record CompareResult(
            SimilarityResult similarity,
            List<String> kwsA,
            List<String> kwsB,
            List<String> common,
            List<String> uniqueA,
            List<String> uniqueB
    ) {}

    @FXML
    private void onCompare() {
        Paper a = comboPaperA.getValue();
        Paper b = comboPaperB.getValue();

        if (a == null || b == null) {
            showAlert("Please select both papers to compare.", Alert.AlertType.WARNING);
            return;
        }
        if (a.getId() == b.getId()) {
            showAlert("Please select two different papers.", Alert.AlertType.WARNING);
            return;
        }

        if (lblCompareStatus != null) lblCompareStatus.setText("Computing similarity…");
        resultsBox.setVisible(false);
        resultsBox.setManaged(false);

        TaskUtil.run(
            () -> {
                SimilarityResult result = similarityService.compare(a, b);

                List<String> kwsA = paperService.getKeywordsForPaper(a.getId());
                List<String> kwsB = paperService.getKeywordsForPaper(b.getId());

                Set<String> setA = new HashSet<>();
                for (String k : kwsA) setA.add(k.trim().toLowerCase());

                List<String> common  = new ArrayList<>();
                List<String> uniqueB = new ArrayList<>();
                for (String k : kwsB) {
                    if (setA.contains(k.trim().toLowerCase())) common.add(k.trim());
                    else uniqueB.add(k.trim());
                }

                Set<String> setB = new HashSet<>();
                for (String k : kwsB) setB.add(k.trim().toLowerCase());

                List<String> uniqueA = new ArrayList<>();
                for (String k : kwsA) {
                    if (!setB.contains(k.trim().toLowerCase())) uniqueA.add(k.trim());
                }

                return new CompareResult(result, kwsA, kwsB, common, uniqueA, uniqueB);
            },
            cr -> {
                lblSimilarityScore.setText(cr.similarity().getScoreAsPercent());
                progressSimilarity.setProgress(cr.similarity().getScore());

                lblCommonKeywords.setText(cr.common().isEmpty()  ? "(No shared keywords)"  : String.join(", ", cr.common()));
                lblKeywordsA.setText(cr.kwsA().isEmpty()         ? "(none)"                : String.join(", ", cr.kwsA()));
                lblKeywordsB.setText(cr.kwsB().isEmpty()         ? "(none)"                : String.join(", ", cr.kwsB()));

                lblTitleA.setText(nvl(a.getTitle()));
                lblAuthorsA.setText(nvl(a.getAuthors()));
                lblYearA.setText(a.getYear() > 0 ? String.valueOf(a.getYear()) : "-");
                lblCategoryA.setText(nvl(a.getCategory()));
                lblSourceA.setText(nvl(a.getSource()));
                lblSummaryA.setText(nvl(a.getAbstractText()));

                lblTitleB.setText(nvl(b.getTitle()));
                lblAuthorsB.setText(nvl(b.getAuthors()));
                lblYearB.setText(b.getYear() > 0 ? String.valueOf(b.getYear()) : "-");
                lblCategoryB.setText(nvl(b.getCategory()));
                lblSourceB.setText(nvl(b.getSource()));
                lblSummaryB.setText(nvl(b.getAbstractText()));

                if (lblCompareStatus != null) lblCompareStatus.setText("Comparison complete.");
                resultsBox.setVisible(true);
                resultsBox.setManaged(true);
            },
            err -> {
                if (lblCompareStatus != null) lblCompareStatus.setText("Comparison failed.");
                showAlert("Comparison failed: " + (err != null ? err.getMessage() : "unknown"), Alert.AlertType.ERROR);
            }
        );
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "-" : s.trim();
    }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
