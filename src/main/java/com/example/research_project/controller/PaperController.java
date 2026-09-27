package com.example.researchassistant.controller;

import com.example.researchassistant.model.Paper;
import com.example.researchassistant.service.PaperService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * PaperController.java
 * Controls the "Papers" list screen.
 * Shows a TableView of all papers, with Add/Edit/Delete/View/Analyse buttons.
 */
public class PaperController {

    @FXML private TextField  searchField;
    @FXML private Label      lblStatus;

    @FXML private TableView<Paper>           papersTable;
    @FXML private TableColumn<Paper,Integer> colId;
    @FXML private TableColumn<Paper,String>  colTitle;
    @FXML private TableColumn<Paper,String>  colAuthors;
    @FXML private TableColumn<Paper,Integer> colYear;
    @FXML private TableColumn<Paper,String>  colCategory;
    @FXML private TableColumn<Paper,String>  colCreatedAt;

    private final PaperService paperService = new PaperService();

    @FXML
    public void initialize() {
        setupColumns();
        loadAllPapers();

        // Double-click a row to view details
        papersTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && getSelected() != null) {
                onView();
            }
        });
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
        colCreatedAt.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCreatedAt()));
    }

    private void loadAllPapers() {
        List<Paper> papers = paperService.getAllPapers();
        papersTable.setItems(FXCollections.observableArrayList(papers));
        lblStatus.setText(papers.size() + " paper(s) found.");
    }

    @FXML
    private void onQuickSearch() {
        String term = searchField.getText().trim();
        List<Paper> results = paperService.searchPapers(term);
        papersTable.setItems(FXCollections.observableArrayList(results));
        lblStatus.setText(results.size() + " result(s).");
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        loadAllPapers();
    }

    @FXML
    private void onAdd() {
        navigateTo("add-paper.fxml");
    }

    @FXML
    private void onView() {
        Paper p = getSelected();
        if (p == null) { showAlert("Select a paper first.", Alert.AlertType.WARNING); return; }
        navigateTo("paper-details.fxml");
        // Pass selected paper to details controller via scene userData
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
            boolean ok = paperService.deletePaper(p.getId());
            if (ok) {
                loadAllPapers();
                showAlert("Paper deleted.", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Delete failed.", Alert.AlertType.ERROR);
            }
        }
    }

    private Paper getSelected() {
        return papersTable.getSelectionModel().getSelectedItem();
    }

    private void navigateTo(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(
                "/com/example/researchassistant/fxml/" + fxml));
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