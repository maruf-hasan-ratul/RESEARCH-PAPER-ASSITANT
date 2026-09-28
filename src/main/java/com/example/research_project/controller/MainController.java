package com.example.research_project.controller;

import com.example.research_project.util.ThemeManager;
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
    @FXML private Button btnSettings;
    @FXML private Button btnThemeToggle;

    @FXML
    public void initialize() {
        updateThemeToggleUI();
        ThemeManager.addListener((theme, fontSize) -> updateThemeToggleUI());
        showDashboard();
    }

    private void updateThemeToggleUI() {
        if (btnThemeToggle != null) {
            if (ThemeManager.isDarkMode()) {
                btnThemeToggle.setText("☀️  Light Mode");
            } else {
                btnThemeToggle.setText("🌙  Dark Mode");
            }
        }
    }

    @FXML
    public void onToggleTheme() {
        ThemeManager.toggleTheme();
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

    @FXML
    public void showSettings() {
        loadView("settings.fxml");
        setActive(btnSettings);
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
            if (view instanceof javafx.scene.Parent parent) {
                parent.getStylesheets().clear();
            }
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActive(Button selected) {
        Button[] allButtons = {btnDashboard, btnPapers, btnAddPaper, btnAnalysis, btnSearch,
                               btnFavorites, btnCompare, btnReports, btnSettings};
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.setStyle(null); // Clear hardcoded inline styles to let CSS classes manage appearance
                btn.getStyleClass().remove("nav-button-active");
                if (btn == selected) {
                    btn.getStyleClass().add("nav-button-active");
                }
            }
        }
    }
}