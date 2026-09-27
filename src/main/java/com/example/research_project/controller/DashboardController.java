package com.example.researchassistant.controller;

import com.example.researchassistant.model.Paper;
import com.example.researchassistant.service.PaperService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

/**
 * DashboardController.java
 *
 * Controls the dashboard screen which shows:
 *   - Statistics cards (total papers, categories, analysed, recent)
 *   - A table of recent papers
 *   - Quick action buttons
 */
public class DashboardController {

    @FXML private Label lblTotalPapers;
    @FXML private Label lblCategories;
    @FXML private Label lblAnalysed;
    @FXML private Label lblRecent;

    @FXML private TableView<Paper>          recentPapersTable;
    @FXML private TableColumn<Paper,String> colTitle;
    @FXML private TableColumn<Paper,String> colAuthors;
    @FXML private TableColumn<Paper,Integer>colYear;
    @FXML private TableColumn<Paper,String> colCategory;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupTableColumns();
        refreshStats();
        loadRecentPapers();
    }

    // ---- Setup TableView columns ----
    private void setupTableColumns() {
        colTitle.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTitle()));
        colAuthors.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getAuthors()));
        colYear.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getYear()).asObject());
        colCategory.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCategory()));
    }

    // ---- Load stats from database ----
    private void refreshStats() {
        lblTotalPapers.setText(String.valueOf(paperService.getTotalPaperCount()));
        lblCategories .setText(String.valueOf(paperService.getTotalCategoryCount()));
        lblAnalysed   .setText(String.valueOf(paperService.getAnalysedPaperCount()));
        List<Paper> recent = paperService.getRecentPapers(50);
        lblRecent.setText(String.valueOf(recent.size()));
    }

    // ---- Load 10 most recent papers into the table ----
    private void loadRecentPapers() {
        List<Paper> papers = paperService.getRecentPapers(10);
        recentPapersTable.setItems(FXCollections.observableArrayList(papers));
    }

    // ---- Quick action buttons ----
    @FXML
    private void onViewAllPapers() { navigateTo("papers.fxml"); }

    @FXML
    private void onAddPaper()  { navigateTo("add-paper.fxml"); }

    @FXML
    private void onAnalyse()   { navigateTo("analysis.fxml"); }

    @FXML
    private void onSearch()    { navigateTo("search.fxml"); }

    // Navigate by finding the root StackPane (MainController's contentArea)
    private void navigateTo(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                "/com/example/researchassistant/fxml/" + fxml));
            StackPane root = (StackPane) recentPapersTable.getScene()
                .lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Dashboard navigate error: " + e.getMessage());
        }
    }
}