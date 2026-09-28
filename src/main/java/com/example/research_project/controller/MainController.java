package com.example.research_project.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class MainController {

    @FXML private StackPane contentArea;

    @FXML private Button btnDashboard;
    @FXML private Button btnPapers;
    @FXML private Button btnAddPaper;
    @FXML private Button btnAnalysis;
    @FXML private Button btnSearch;
    @FXML private Button btnFavorites;
    @FXML private Button btnCompare;
    @FXML private Button btnReports;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        loadView("dashboard.fxml");
        setActive(btnDashboard);
    }

    @FXML
    public void showPapers() {
        loadView("papers.fxml");
        setActive(btnPapers);
    }

    @FXML
    public void showAddPaper() {
        loadView("add-paper.fxml");
        setActive(btnAddPaper);
    }

    @FXML
    public void showAnalysis() {
        loadView("analysis.fxml");
        setActive(btnAnalysis);
    }

    @FXML
    public void showSearch() {
        loadView("search.fxml");
        setActive(btnSearch);
    }

    @FXML
    public void showFavorites() {
        loadView("favorites.fxml");
        setActive(btnFavorites);
    }

    @FXML
    public void showComparison() {
        loadView("comparison.fxml");
        setActive(btnCompare);
    }

    @FXML
    public void showReports() {
        loadView("reports.fxml");
        setActive(btnReports);
    }

    private void loadView(String fxmlFile) {
        try {
            URL location = getClass().getResource("/com/example/research_project/fxml/" + fxmlFile);
            if (location == null) {
                location = getClass().getResource("/com/example/researchassistant/fxml/" + fxmlFile);
            }
            if (location == null) {
                System.err.println("Cannot find FXML: " + fxmlFile);
                return;
            }
            Node view = FXMLLoader.load(location);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActive(Button selected) {
        Button[] allButtons = {btnDashboard, btnPapers, btnAddPaper, btnAnalysis, btnSearch,
                               btnFavorites, btnCompare, btnReports};
        for (Button btn : allButtons) {
            if (btn == selected) {
                btn.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: BASELINE_LEFT;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-weight: 500; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: BASELINE_LEFT;");
            }
        }
    }
}