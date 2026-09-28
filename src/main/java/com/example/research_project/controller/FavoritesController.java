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
import java.util.List;

/**
 * FavoritesController.java
 *
 * Dedicated screen for managing bookmarked / favorite research papers.
 * Allows instant viewing, filtering/searching among favorites, and navigation.
 *
 * MULTITHREADING:
 *   - loadFavorites() runs DB queries in a background thread via TaskUtil.
 *   - onQuickSearch() filters off the FX thread to keep the table responsive.
 *   - onRemoveFavorite() performs the DB update in a background thread.
 */
public class FavoritesController {

    @FXML private TextField searchField;
    @FXML private Label lblStatus;

    @FXML private TableView<Paper> papersTable;
    @FXML private TableColumn<Paper, Integer> colId;
    @FXML private TableColumn<Paper, String> colFavorite;
    @FXML private TableColumn<Paper, String> colTitle;
    @FXML private TableColumn<Paper, String> colAuthors;
    @FXML private TableColumn<Paper, Integer> colYear;
    @FXML private TableColumn<Paper, String> colCategory;
    @FXML private TableColumn<Paper, String> colStatus;
    @FXML private TableColumn<Paper, String> colCreatedAt;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupColumns();
        loadFavoritesAsync();

        papersTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && getSelected() != null) {
                onView();
            }
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colFavorite.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isFavorite() ? "★" : "☆"));
        colTitle.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitle()));
        colAuthors.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAuthors()));
        colYear.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getYear()).asObject());
        colCategory.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategory()));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getReadingStatus()));
        colCreatedAt.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCreatedAt()));
    }

    /** Loads favorite papers off the FX thread; updates the table on completion. */
    private void loadFavoritesAsync() {
        lblStatus.setText("Loading…");
        TaskUtil.run(
            () -> paperService.getFavoritePapers(),
            favorites -> {
                papersTable.setItems(FXCollections.observableArrayList(favorites));
                lblStatus.setText(favorites.size() + " favorite paper(s) bookmarked.");
            },
            err -> lblStatus.setText("Failed to load favorites.")
        );
    }

    @FXML
    private void onQuickSearch() {
        String term = searchField.getText().trim().toLowerCase();
        if (term.isEmpty()) {
            loadFavoritesAsync();
            return;
        }

        lblStatus.setText("Searching…");
        TaskUtil.run(
            () -> paperService.getFavoritePapers().stream()
                    .filter(p -> (p.getTitle()    != null && p.getTitle().toLowerCase().contains(term)) ||
                                 (p.getAuthors()  != null && p.getAuthors().toLowerCase().contains(term)) ||
                                 (p.getCategory() != null && p.getCategory().toLowerCase().contains(term)))
                    .toList(),
            filtered -> {
                papersTable.setItems(FXCollections.observableArrayList(filtered));
                lblStatus.setText(filtered.size() + " match(es) in favorites.");
            },
            err -> lblStatus.setText("Search failed.")
        );
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadFavoritesAsync();
    }

    @FXML
    private void onView() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper first.", Alert.AlertType.WARNING); return; }
        papersTable.getScene().setUserData(p);
        navigateTo("paper-details.fxml");
    }

    @FXML
    private void onRemoveFavorite() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper to remove from favorites.", Alert.AlertType.WARNING); return; }

        lblStatus.setText("Updating…");
        TaskUtil.run(
            () -> paperService.setFavorite(p.getId(), false),
            ok -> {
                if (ok) {
                    loadFavoritesAsync();
                } else {
                    showAlert("Could not update favorite status.", Alert.AlertType.ERROR);
                }
            },
            err -> showAlert("Error: " + (err != null ? err.getMessage() : "unknown"), Alert.AlertType.ERROR)
        );
    }

    @FXML
    private void onAnalyse() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper to analyse.", Alert.AlertType.WARNING); return; }
        papersTable.getScene().setUserData(p);
        navigateTo("analysis.fxml");
    }

    private Paper getSelected() {
        return papersTable.getSelectionModel().getSelectedItem();
    }

    private void navigateTo(String fxml) {
        try {
            URL loc = getClass().getResource("/com/example/research_project/fxml/" + fxml);
            if (loc == null) {
                loc = getClass().getResource("/com/example/researchassistant/fxml/" + fxml);
            }
            Node view = FXMLLoader.load(loc);
            StackPane root = (StackPane) papersTable.getScene().lookup("#contentArea");
            if (root != null) root.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("FavoritesController navigate error: " + e.getMessage());
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
