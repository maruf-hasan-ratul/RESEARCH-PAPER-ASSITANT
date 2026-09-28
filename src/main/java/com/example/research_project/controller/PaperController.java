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
import java.util.Optional;

public class PaperController {

    @FXML private TextField  searchField;
    @FXML private Label      lblStatus;

    @FXML private TableView<Paper>           papersTable;
    @FXML private TableColumn<Paper,Integer> colId;
    @FXML private TableColumn<Paper,String>  colFavorite;
    @FXML private TableColumn<Paper,String>  colTitle;
    @FXML private TableColumn<Paper,String>  colAuthors;
    @FXML private TableColumn<Paper,Integer> colYear;
    @FXML private TableColumn<Paper,String>  colCategory;
    @FXML private TableColumn<Paper,String>  colStatus;
    @FXML private TableColumn<Paper,String>  colCreatedAt;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupColumns();
        loadAllPapersAsync();

        papersTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && getSelected() != null) {
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
        colCreatedAt.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCreatedAt()));
    }

    private void loadAllPapersAsync() {
        lblStatus.setText("Loading…");
        TaskUtil.run(
            () -> paperService.getAllPapers(),
            papers -> {
                papersTable.setItems(FXCollections.observableArrayList(papers));
                lblStatus.setText(papers.size() + " paper(s) found.");
            },
            err -> lblStatus.setText("Failed to load papers.")
        );
    }

    @FXML
    private void onQuickSearch() {
        String term = searchField.getText().trim();
        lblStatus.setText("Searching…");
        TaskUtil.run(
            () -> paperService.searchPapers(term),
            results -> {
                papersTable.setItems(FXCollections.observableArrayList(results));
                lblStatus.setText(results.size() + " result(s).");
            },
            err -> lblStatus.setText("Search failed.")
        );
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadAllPapersAsync();
    }

    @FXML
    private void onAdd() {
        papersTable.getScene().setUserData(null);
        navigateTo("add-paper.fxml");
    }

    @FXML
    private void onView() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper first.", Alert.AlertType.WARNING); return; }
        papersTable.getScene().setUserData(p);
        navigateTo("paper-details.fxml");
    }

    @FXML
    private void onEdit() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper to edit.", Alert.AlertType.WARNING); return; }
        papersTable.getScene().setUserData(p);
        navigateTo("add-paper.fxml");
    }

    @FXML
    private void onAnalyse() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper to analyse.", Alert.AlertType.WARNING); return; }
        papersTable.getScene().setUserData(p);
        navigateTo("analysis.fxml");
    }

    @FXML
    private void onDelete() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper to delete.", Alert.AlertType.WARNING); return; }

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete paper:\n\"" + p.getTitle() + "\"?\n\nThis also deletes keywords and notes.",
            ButtonType.YES, ButtonType.NO).showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            lblStatus.setText("Deleting…");
            TaskUtil.run(
                () -> paperService.deletePaper(p.getId()),
                ok -> {
                    if (ok) {
                        loadAllPapersAsync();
                        showAlert("Paper deleted.", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Delete failed.", Alert.AlertType.ERROR);
                    }
                },
                err -> showAlert("Delete failed: " + (err != null ? err.getMessage() : ""), Alert.AlertType.ERROR)
            );
        }
    }

    @FXML
    private void onToggleFavorite() {
        Paper p = getSelected();
        if (p == null) {
            showAlert("Select a paper to toggle favorite.", Alert.AlertType.WARNING);
            return;
        }
        boolean newFav = !p.isFavorite();
        TaskUtil.run(
            () -> paperService.setFavorite(p.getId(), newFav),
            ok -> {
                if (ok) {
                    p.setFavorite(newFav);
                    papersTable.refresh();
                } else {
                    showAlert("Failed to update favorite status.", Alert.AlertType.ERROR);
                }
            },
            err -> showAlert("Error updating favorite.", Alert.AlertType.ERROR)
        );
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
            System.err.println("PaperController navigate error: " + e.getMessage());
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}