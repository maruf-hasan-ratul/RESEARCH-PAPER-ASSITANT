package com.example.researchassistant.controller;

import com.example.researchassistant.model.Paper;
import com.example.researchassistant.service.PaperService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchController.java
 *
 * Controls the Search screen. Allows searching papers by:
 *   - Keyword (title or author)
 *   - Category (dropdown)
 *   - Year (dropdown)
 *
 * Results are shown in a TableView.
 */
public class SearchController {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> yearCombo;
    @FXML private Label            lblResultCount;

    @FXML private TableView<Paper>           resultsTable;
    @FXML private TableColumn<Paper,Integer> colId;
    @FXML private TableColumn<Paper,String>  colTitle;
    @FXML private TableColumn<Paper,String>  colAuthors;
    @FXML private TableColumn<Paper,Integer> colYear;
    @FXML private TableColumn<Paper,String>  colCategory;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupColumns();
        loadFilterOptions();
        onSearch(); // show all by default
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colTitle.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getTitle()));
        colAuthors.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getAuthors()));
        colYear.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getYear()).asObject());
        colCategory.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCategory()));
    }

    /** Populate category and year dropdowns from actual data */
    private void loadFilterOptions() {
        List<Paper> allPapers = paperService.getAllPapers();

        // Unique categories
        List<String> categories = new ArrayList<>();
        categories.add("All");
        allPapers.stream()
            .map(Paper::getCategory)
            .filter(c -> c != null && !c.isBlank())
            .distinct()
            .sorted()
            .forEach(categories::add);
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        categoryCombo.setValue("All");

        // Unique years
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
    }

    @FXML
    private void onSearch() {
        String keyword  = searchField.getText().trim();
        String category = categoryCombo.getValue();
        String yearStr  = yearCombo.getValue();

        int year = 0;
        if (yearStr != null && !yearStr.equals("All")) {
            try { year = Integer.parseInt(yearStr); }
            catch (NumberFormatException ignored) {}
        }

        boolean hasCategory = category != null && !category.equals("All");
        boolean hasYear     = year > 0;
        boolean hasKeyword  = !keyword.isEmpty();

        List<Paper> results;

        if (!hasKeyword && !hasCategory && !hasYear) {
            results = paperService.getAllPapers();
        } else if (hasKeyword && !hasCategory && !hasYear) {
            results = paperService.searchPapers(keyword);
        } else if (!hasKeyword && (hasCategory || hasYear)) {
            results = paperService.filterPapers(hasCategory ? category : null, year);
        } else {
            // Keyword + filter: search then filter in memory
            results = paperService.searchPapers(keyword);
            if (hasCategory) {
                final String cat = category;
                results = results.stream().filter(p -> cat.equals(p.getCategory())).toList();
            }
            if (hasYear) {
                final int yr = year;
                results = results.stream().filter(p -> p.getYear() == yr).toList();
            }
        }

        resultsTable.setItems(FXCollections.observableArrayList(results));
        lblResultCount.setText(results.size() + " result(s) found.");
    }

    @FXML
    private void onReset() {
        searchField.clear();
        categoryCombo.setValue("All");
        yearCombo.setValue("All");
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
            Node view = FXMLLoader.load(getClass().getResource(
                "/com/example/researchassistant/fxml/" + fxml));
            StackPane root = (StackPane) resultsTable.getScene().lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("SearchController navigate error: " + e.getMessage());
        }
    }
}