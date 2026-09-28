package com.example.research_project.controller;

import com.example.research_project.model.Paper;
import com.example.research_project.service.PaperService;
import com.example.research_project.util.TaskUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * DashboardController.java
 *
 * Controls the research dashboard screen:
 *   - Overview statistics (total papers, notes, favorites, reading status breakdown)
 *   - Topic category distribution (JavaFX PieChart)
 *   - Most common keywords ranking (ListView)
 *   - Recent papers library table
 *   - Quick navigation shortcuts
 *
 * MULTITHREADING:
 *   All DB-heavy operations (stats, charts, keywords, recent papers) are
 *   dispatched to background daemon threads via TaskUtil so the UI never
 *   freezes during initialize().
 */
public class DashboardController {

    // Metrics cards
    @FXML private Label lblTotalPapers;
    @FXML private Label lblTotalNotes;
    @FXML private Label lblFavorites;
    @FXML private Label lblUnread;
    @FXML private Label lblReading;
    @FXML private Label lblCompleted;
    @FXML private Label lblCategories;
    @FXML private Label lblAnalysed;

    // Charts & rankings
    @FXML private PieChart categoryChart;
    @FXML private ListView<String> topKeywordsList;

    // Recent papers table
    @FXML private TableView<Paper>           recentPapersTable;
    @FXML private TableColumn<Paper,String>  colFavorite;
    @FXML private TableColumn<Paper,String>  colTitle;
    @FXML private TableColumn<Paper,String>  colAuthors;
    @FXML private TableColumn<Paper,Integer> colYear;
    @FXML private TableColumn<Paper,String>  colCategory;
    @FXML private TableColumn<Paper,String>  colStatus;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupTableColumns();

        // Kick off all data loads concurrently in background threads
        loadStatsAsync();
        loadRecentPapersAsync();
        loadChartsAsync();
        loadTopKeywordsAsync();

        recentPapersTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && recentPapersTable.getSelectionModel().getSelectedItem() != null) {
                Paper selected = recentPapersTable.getSelectionModel().getSelectedItem();
                recentPapersTable.getScene().setUserData(selected);
                navigateTo("paper-details.fxml");
            }
        });
    }

    private void setupTableColumns() {
        if (colFavorite != null) {
            colFavorite.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isFavorite() ? "★" : "☆"));
        }
        colTitle.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTitle()));
        colAuthors.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getAuthors()));
        colYear.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getYear()).asObject());
        colCategory.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCategory()));
        if (colStatus != null) {
            colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getReadingStatus()));
        }
    }

    // ----------------------------------------------------------------
    // Async data loaders — each runs its DB call off the FX thread
    // ----------------------------------------------------------------

    /** Loads all 8 stat counts concurrently and updates labels on the FX thread. */
    private void loadStatsAsync() {
        TaskUtil.run(
            () -> new int[]{
                paperService.getTotalPaperCount(),
                paperService.getTotalNoteCount(),
                paperService.getFavoritePaperCount(),
                paperService.getUnreadPaperCount(),
                paperService.getReadingPaperCount(),
                paperService.getCompletedPaperCount(),
                paperService.getTotalCategoryCount(),
                paperService.getAnalysedPaperCount()
            },
            counts -> {
                if (lblTotalPapers != null) lblTotalPapers.setText(String.valueOf(counts[0]));
                if (lblTotalNotes  != null) lblTotalNotes .setText(String.valueOf(counts[1]));
                if (lblFavorites   != null) lblFavorites  .setText(String.valueOf(counts[2]));
                if (lblUnread      != null) lblUnread     .setText(String.valueOf(counts[3]));
                if (lblReading     != null) lblReading    .setText(String.valueOf(counts[4]));
                if (lblCompleted   != null) lblCompleted  .setText(String.valueOf(counts[5]));
                if (lblCategories  != null) lblCategories .setText(String.valueOf(counts[6]));
                if (lblAnalysed    != null) lblAnalysed   .setText(String.valueOf(counts[7]));
            },
            err -> System.err.println("Dashboard stats error: " + (err != null ? err.getMessage() : "?"))
        );
    }

    /** Loads the 8 most recent papers into the table. */
    private void loadRecentPapersAsync() {
        TaskUtil.run(
            () -> paperService.getRecentPapers(8),
            papers -> recentPapersTable.setItems(FXCollections.observableArrayList(papers)),
            err -> System.err.println("Dashboard recent papers error: " + (err != null ? err.getMessage() : "?"))
        );
    }

    /** Loads category statistics and populates the PieChart. */
    private void loadChartsAsync() {
        if (categoryChart == null) return;
        TaskUtil.run(
            () -> paperService.getCategoryStatistics(),
            catStats -> {
                ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
                for (Map.Entry<String, Integer> entry : catStats.entrySet()) {
                    chartData.add(new PieChart.Data(
                        entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
                }
                categoryChart.setData(chartData);
                categoryChart.setLegendVisible(true);
            },
            err -> System.err.println("Dashboard chart error: " + (err != null ? err.getMessage() : "?"))
        );
    }

    /** Loads top 10 keywords and populates the ListView. */
    private void loadTopKeywordsAsync() {
        if (topKeywordsList == null) return;
        TaskUtil.run(
            () -> paperService.getTopKeywords(10),
            topKw -> {
                ObservableList<String> items = FXCollections.observableArrayList();
                int rank = 1;
                for (Map.Entry<String, Integer> entry : topKw.entrySet()) {
                    items.add(String.format("#%d  %s  (%d papers)", rank++, entry.getKey(), entry.getValue()));
                }
                if (items.isEmpty()) items.add("No keywords extracted yet.");
                topKeywordsList.setItems(items);
            },
            err -> System.err.println("Dashboard keywords error: " + (err != null ? err.getMessage() : "?"))
        );
    }

    // ---- Quick action buttons ----
    @FXML private void onViewAllPapers() { navigateTo("papers.fxml"); }
    @FXML private void onViewFavorites() { navigateTo("favorites.fxml"); }
    @FXML private void onCompare()       { navigateTo("comparison.fxml"); }
    @FXML private void onReports()       { navigateTo("reports.fxml"); }
    @FXML private void onAddPaper()      { navigateTo("add-paper.fxml"); }
    @FXML private void onAnalyse()       { navigateTo("analysis.fxml"); }
    @FXML private void onSearch()        { navigateTo("search.fxml"); }

    private void navigateTo(String fxml) {
        try {
            java.net.URL loc = getClass().getResource("/com/example/research_project/fxml/" + fxml);
            if (loc == null) {
                loc = getClass().getResource("/com/example/researchassistant/fxml/" + fxml);
            }
            Node view = FXMLLoader.load(loc);
            StackPane root = (StackPane) recentPapersTable.getScene().lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Dashboard navigate error: " + e.getMessage());
        }
    }
}