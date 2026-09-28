package com.example.research_project.controller;

import com.example.research_project.model.Paper;
import com.example.research_project.service.PaperService;
import com.example.research_project.util.TaskUtil;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchController.java
 *
 * Controls the Advanced Paper Search & Filtering screen.
 * Allows searching papers by:
 *   - Query string (matches title, author, abstract, or keyword)
 *   - Category (dropdown)
 *   - Year (dropdown)
 *   - Reading status (UNREAD, READING, COMPLETED)
 *   - Favorite status (All, Favorites Only)
 *
 * Results are displayed in an interactive TableView.
 *
 * MULTITHREADING:
 *   - Filter-option population runs off the FX thread (loadFilterOptionsAsync).
 *   - Every search query runs off the FX thread (onSearch).
 *   - The result-count label shows "Searching…" feedback during the query.
 */
public class SearchController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> yearCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> favoriteCombo;
    @FXML private Label            lblResultCount;

    @FXML private TableView<Paper>           resultsTable;
    @FXML private TableColumn<Paper,Integer> colId;
    @FXML private TableColumn<Paper,String>  colFavorite;
    @FXML private TableColumn<Paper,String>  colTitle;
    @FXML private TableColumn<Paper,String>  colAuthors;
    @FXML private TableColumn<Paper,Integer> colYear;
    @FXML private TableColumn<Paper,String>  colCategory;
    @FXML private TableColumn<Paper,String>  colStatus;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupColumns();

        // Populate filter dropdowns in background, then trigger the initial search
        loadFilterOptionsAsync(() -> onSearch());

        resultsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && resultsTable.getSelectionModel().getSelectedItem() != null) {
                onView();
            }
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getId()).asObject());
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

    /**
     * Populate filter dropdowns from actual data, running the DB call off the FX thread.
     * Once done, calls {@code afterLoad} on the FX thread (e.g. to trigger the first search).
     */
    private void loadFilterOptionsAsync(Runnable afterLoad) {
        TaskUtil.run(
            () -> paperService.getAllPapers(),
            allPapers -> {
                // Categories
                List<String> categories = new ArrayList<>();
                categories.add("All");
                allPapers.stream()
                    .map(Paper::getCategory)
                    .filter(c -> c != null && !c.isBlank())
                    .distinct().sorted()
                    .forEach(categories::add);
                categoryCombo.setItems(FXCollections.observableArrayList(categories));
                categoryCombo.setValue("All");

                // Years
                List<String> years = new ArrayList<>();
                years.add("All");
                allPapers.stream()
                    .map(Paper::getYear)
                    .filter(y -> y > 0)
                    .distinct()
                    .sorted((a, b) -> b - a)
                    .map(String::valueOf)
                    .forEach(years::add);
                yearCombo.setItems(FXCollections.observableArrayList(years));
                yearCombo.setValue("All");

                // Reading Status
                if (statusCombo != null) {
                    statusCombo.setItems(FXCollections.observableArrayList(
                        "All", "UNREAD", "READING", "COMPLETED"));
                    statusCombo.setValue("All");
                }

                // Favorite Status
                if (favoriteCombo != null) {
                    favoriteCombo.setItems(FXCollections.observableArrayList(
                        "All Papers", "Favorites Only"));
                    favoriteCombo.setValue("All Papers");
                }

                // Run caller-supplied post-load action (e.g. initial search)
                if (afterLoad != null) afterLoad.run();
            },
            err -> lblResultCount.setText("Failed to load filters.")
        );
    }

    @FXML
    private void onSearch() {
        String query    = (searchField    != null) ? searchField.getText().trim() : "";
        String category = (categoryCombo  != null) ? categoryCombo.getValue()     : "All";
        String yearStr  = (yearCombo      != null) ? yearCombo.getValue()          : "All";
        String status   = (statusCombo    != null) ? statusCombo.getValue()        : "All";
        String favStr   = (favoriteCombo  != null) ? favoriteCombo.getValue()      : "All Papers";

        int year = 0;
        if (yearStr != null && !yearStr.equals("All")) {
            try { year = Integer.parseInt(yearStr); }
            catch (NumberFormatException ignored) {}
        }
        boolean favoriteOnly = "Favorites Only".equalsIgnoreCase(favStr);

        final String  fQuery    = query.isEmpty() ? null : query;
        final String  fCategory = category;
        final int     fYear     = year;
        final String  fStatus   = status;
        final Boolean fFav      = favoriteOnly ? Boolean.TRUE : null;

        lblResultCount.setText("Searching…");

        TaskUtil.run(
            () -> paperService.searchAdvanced(fQuery, fCategory, fYear, fStatus, fFav),
            results -> {
                resultsTable.setItems(FXCollections.observableArrayList(results));
                lblResultCount.setText(results.size() + " matching paper(s) found.");
            },
            err -> lblResultCount.setText("Search failed.")
        );
    }

    @FXML
    private void onReset() {
        if (searchField   != null) searchField.clear();
        if (categoryCombo != null) categoryCombo.setValue("All");
        if (yearCombo     != null) yearCombo.setValue("All");
        if (statusCombo   != null) statusCombo.setValue("All");
        if (favoriteCombo != null) favoriteCombo.setValue("All Papers");
        onSearch();
    }

    @FXML
    private void onView() {
        Paper p = resultsTable.getSelectionModel().getSelectedItem();
        if (p == null) {
            new Alert(Alert.AlertType.WARNING, "Select a paper first.", ButtonType.OK).showAndWait();
            return;
        }
        resultsTable.getScene().setUserData(p);
        navigateTo("paper-details.fxml");
    }

    @FXML
    private void onAnalyse() {
        Paper p = resultsTable.getSelectionModel().getSelectedItem();
        if (p == null) {
            new Alert(Alert.AlertType.WARNING, "Select a paper first.", ButtonType.OK).showAndWait();
            return;
        }
        resultsTable.getScene().setUserData(p);
        navigateTo("analysis.fxml");
    }

    private void navigateTo(String fxml) {
        try {
            URL loc = getClass().getResource("/com/example/research_project/fxml/" + fxml);
            if (loc == null) {
                loc = getClass().getResource("/com/example/researchassistant/fxml/" + fxml);
            }
            Node view = FXMLLoader.load(loc);
            StackPane root = (StackPane) resultsTable.getScene().lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("SearchController navigate error: " + e.getMessage());
        }
    }
}