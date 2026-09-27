package com.example.research_project.controller;

import com.example.research_project.model.Paper;
import com.example.research_project.model.SimilarityResult;
import com.example.research_project.service.PaperService;
import com.example.research_project.service.SimilarityService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PaperComparisonController.java
 *
 * Provides side-by-side comparison of two research papers.
 * Evaluates textual cosine similarity using TF-IDF via SimilarityService,
 * extracts common and distinct keywords, and compares metadata, methodology, and findings.
 */
public class PaperComparisonController {

    @FXML private ComboBox<Paper> comboPaperA;
    @FXML private ComboBox<Paper> comboPaperB;

    // Results container & labels
    @FXML private VBox resultsBox;
    @FXML private Label lblSimilarityScore;
    @FXML private ProgressBar progressSimilarity;

    // Keywords
    @FXML private Label lblCommonKeywords;
    @FXML private Label lblKeywordsA;
    @FXML private Label lblKeywordsB;

    // Paper A details
    @FXML private Label lblTitleA;
    @FXML private Label lblAuthorsA;
    @FXML private Label lblYearA;
    @FXML private Label lblCategoryA;
    @FXML private Label lblSourceA;
    @FXML private Label lblSummaryA;

    // Paper B details
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
        loadPapers();
    }

    private void loadPapers() {
        List<Paper> all = paperService.getAllPapers();
        comboPaperA.setItems(FXCollections.observableArrayList(all));
        comboPaperB.setItems(FXCollections.observableArrayList(all));

        StringConverter<Paper> converter = new StringConverter<>() {
            @Override
            public String toString(Paper p) {
                return p == null ? "" : "[" + p.getId() + "] " + p.getTitle();
            }

            @Override
            public Paper fromString(String s) {
                return null;
            }
        };

        comboPaperA.setConverter(converter);
        comboPaperB.setConverter(converter);

        if (all.size() >= 2) {
            comboPaperA.setValue(all.get(0));
            comboPaperB.setValue(all.get(1));
        } else if (all.size() == 1) {
            comboPaperA.setValue(all.get(0));
        }
    }

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

        // Calculate similarity using existing SimilarityService
        SimilarityResult result = similarityService.compare(a, b);

        // Keywords analysis
        List<String> kwsA = paperService.getKeywordsForPaper(a.getId());
        List<String> kwsB = paperService.getKeywordsForPaper(b.getId());

        Set<String> setA = new HashSet<>();
        for (String k : kwsA) setA.add(k.trim().toLowerCase());

        List<String> common = new ArrayList<>();
        List<String> uniqueB = new ArrayList<>();

        for (String k : kwsB) {
            if (setA.contains(k.trim().toLowerCase())) {
                common.add(k.trim());
            } else {
                uniqueB.add(k.trim());
            }
        }

        Set<String> setB = new HashSet<>();
        for (String k : kwsB) setB.add(k.trim().toLowerCase());

        List<String> uniqueA = new ArrayList<>();
        for (String k : kwsA) {
            if (!setB.contains(k.trim().toLowerCase())) {
                uniqueA.add(k.trim());
            }
        }

        // Populate UI
        lblSimilarityScore.setText(result.getScoreAsPercent());
        progressSimilarity.setProgress(result.getScore());

        lblCommonKeywords.setText(common.isEmpty() ? "(No shared keywords)" : String.join(", ", common));
        lblKeywordsA.setText(kwsA.isEmpty() ? "(none)" : String.join(", ", kwsA));
        lblKeywordsB.setText(kwsB.isEmpty() ? "(none)" : String.join(", ", kwsB));

        // Paper A details
        lblTitleA.setText(nvl(a.getTitle()));
        lblAuthorsA.setText(nvl(a.getAuthors()));
        lblYearA.setText(a.getYear() > 0 ? String.valueOf(a.getYear()) : "-");
        lblCategoryA.setText(nvl(a.getCategory()));
        lblSourceA.setText(nvl(a.getSource()));
        lblSummaryA.setText(nvl(a.getAbstractText()));

        // Paper B details
        lblTitleB.setText(nvl(b.getTitle()));
        lblAuthorsB.setText(nvl(b.getAuthors()));
        lblYearB.setText(b.getYear() > 0 ? String.valueOf(b.getYear()) : "-");
        lblCategoryB.setText(nvl(b.getCategory()));
        lblSourceB.setText(nvl(b.getSource()));
        lblSummaryB.setText(nvl(b.getAbstractText()));

        resultsBox.setVisible(true);
        resultsBox.setManaged(true);
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "-" : s.trim();
    }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
