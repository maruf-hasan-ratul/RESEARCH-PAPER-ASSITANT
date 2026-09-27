package com.example.research_project.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * MainController.java - Controls the main window navigation.
 *
 * WHAT IT DOES:
 * The main window has a sidebar with navigation buttons.
 * When a button is clicked, this controller loads the
 * corresponding FXML file into the center "contentArea".
 *
 * This is called "Single Page Application" style:
 * only one window stays open and the center content changes.
 *
 * HOW FXML LOADING WORKS:
 *   FXMLLoader.load(url) reads an FXML file and creates
 *   the JavaFX Node tree described in it.
 *   We then set that node as the content of our StackPane.
 */
public class MainController {

    @FXML private StackPane contentArea;  // The center area where screens are loaded

    // Sidebar navigation buttons (so we can update which one looks "active")
    @FXML private Button btnDashboard;
    @FXML private Button btnPapers;
    @FXML private Button btnAddPaper;
    @FXML private Button btnAnalysis;
    @FXML private Button btnSearch;

    /**
     * initialize() is called automatically by JavaFX after the FXML is loaded.
     * We load the dashboard as the default starting screen.
     */
    @FXML
    public void initialize() {
        showDashboard();
    }

    // ================================================================
    // NAVIGATION METHODS (called by FXML onAction attributes)
    // ================================================================

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

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    /**
     * Loads an FXML file into the contentArea StackPane.
     *
     * @param fxmlFile  Filename (e.g. "dashboard.fxml")
     */
    private void loadView(String fxmlFile) {
        try {
            URL location = getClass().getResource(
                "/com/example/researchassistant/fxml/" + fxmlFile
            );
            if (location == null) {
                System.err.println("Cannot find FXML: " + fxmlFile);
                return;
            }
            Node view = FXMLLoader.load(location);
            contentArea.getChildren().setAll(view); // replace current content
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates sidebar navigation buttons to highlight the active screen.
     * Applies vibrant active style to the selected button and neutral style to others.
     */
    private void setActive(Button selected) {
        Button[] allButtons = {btnDashboard, btnPapers, btnAddPaper, btnAnalysis, btnSearch};
        for (Button btn : allButtons) {
            if (btn == selected) {
                btn.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: BASELINE_LEFT;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-weight: 500; -fx-background-radius: 8px; -fx-padding: 10 14; -fx-alignment: BASELINE_LEFT;");
            }
        }
    }
}