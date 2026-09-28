package com.example.research_project.controller;

import com.example.research_project.model.Paper;
import com.example.research_project.service.PaperService;
import com.example.research_project.service.ResearchReportService;
import com.example.research_project.util.TaskUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportController {

    public static class SelectablePaper {
        private final Paper paper;
        private final BooleanProperty selected;

        public SelectablePaper(Paper paper, boolean isSelected) {
            this.paper    = paper;
            this.selected = new SimpleBooleanProperty(isSelected);
        }

        public Paper getPaper()                    { return paper; }
        public BooleanProperty selectedProperty()  { return selected; }
        public boolean isSelected()                { return selected.get(); }
        public void setSelected(boolean val)       { selected.set(val); }
    }

    @FXML private TableView<SelectablePaper>          papersSelectionTable;
    @FXML private TableColumn<SelectablePaper, Boolean>  colSelect;
    @FXML private TableColumn<SelectablePaper, Integer>  colId;
    @FXML private TableColumn<SelectablePaper, String>   colTitle;
    @FXML private TableColumn<SelectablePaper, String>   colAuthors;
    @FXML private TableColumn<SelectablePaper, Integer>  colYear;
    @FXML private TableColumn<SelectablePaper, String>   colCategory;
    @FXML private TableColumn<SelectablePaper, String>   colStatus;

    @FXML private TextArea txtReportPreview;
    @FXML private Label    lblSelectionSummary;
    @FXML private Button   btnExportPdf;
    @FXML private Label    lblReportStatus;

    private final PaperService          paperService  = new PaperService();
    private final ResearchReportService reportService = new ResearchReportService();

    private final ObservableList<SelectablePaper> selectablePapers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadPapersAsync();
    }

    private void setupTable() {
        papersSelectionTable.setEditable(true);

        colSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        colId.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getPaper().getId()).asObject());
        colTitle.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPaper().getTitle()));
        colAuthors.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPaper().getAuthors()));
        colYear.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getPaper().getYear()).asObject());
        colCategory.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPaper().getCategory()));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPaper().getReadingStatus()));

        papersSelectionTable.setItems(selectablePapers);
    }

    private void loadPapersAsync() {
        if (lblSelectionSummary != null) lblSelectionSummary.setText("Loading papers…");
        TaskUtil.run(
            () -> paperService.getAllPapers(),
            all -> {
                selectablePapers.clear();
                for (Paper p : all) {
                    selectablePapers.add(new SelectablePaper(p, true));
                }
                updateSelectionCount();
            },
            err -> {
                if (lblSelectionSummary != null)
                    lblSelectionSummary.setText("Failed to load papers.");
            }
        );
    }

    private void updateSelectionCount() {
        long count = selectablePapers.stream().filter(SelectablePaper::isSelected).count();
        if (lblSelectionSummary != null)
            lblSelectionSummary.setText(count + " of " + selectablePapers.size() + " papers selected");
    }

    @FXML
    private void onSelectAll() {
        for (SelectablePaper sp : selectablePapers) sp.setSelected(true);
        papersSelectionTable.refresh();
        updateSelectionCount();
    }

    @FXML
    private void onDeselectAll() {
        for (SelectablePaper sp : selectablePapers) sp.setSelected(false);
        papersSelectionTable.refresh();
        updateSelectionCount();
    }

    private List<Paper> getSelectedPapers() {
        List<Paper> list = new ArrayList<>();
        for (SelectablePaper sp : selectablePapers) {
            if (sp.isSelected()) list.add(sp.getPaper());
        }
        return list;
    }

    @FXML
    private void onGenerateReport() {
        List<Paper> selected = getSelectedPapers();
        if (selected.isEmpty()) {
            showAlert("Please select at least one paper.", Alert.AlertType.WARNING);
            return;
        }

        updateSelectionCount();
        if (lblReportStatus != null) lblReportStatus.setText("Generating report…");
        txtReportPreview.setText("");
        btnExportPdf.setDisable(true);

        TaskUtil.run(
            () -> reportService.generateTextReport(selected),
            report -> {
                txtReportPreview.setText(report);
                btnExportPdf.setDisable(false);
                if (lblReportStatus != null) lblReportStatus.setText("Report ready.");
            },
            err -> {
                if (lblReportStatus != null) lblReportStatus.setText("Report generation failed.");
                showAlert("Failed to generate report: " + (err != null ? err.getMessage() : "unknown"),
                        Alert.AlertType.ERROR);
            }
        );
    }

    @FXML
    private void onExportPdf() {
        List<Paper> selected = getSelectedPapers();
        if (selected.isEmpty()) {
            showAlert("Please select at least one paper to export.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Research Report as PDF");
        fc.setInitialFileName("Research_Report_" + System.currentTimeMillis() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

        File file = fc.showSaveDialog(txtReportPreview.getScene().getWindow());
        if (file == null) return;

        if (lblReportStatus != null) lblReportStatus.setText("Exporting PDF…");
        btnExportPdf.setDisable(true);

        TaskUtil.run(
            (java.util.concurrent.Callable<Void>) () -> {
                reportService.exportPdfReport(selected, file);
                return null;
            },
            ignored -> {
                btnExportPdf.setDisable(false);
                if (lblReportStatus != null) lblReportStatus.setText("PDF exported.");
                showAlert("Research report exported successfully to:\n" + file.getAbsolutePath(),
                        Alert.AlertType.INFORMATION);
            },
            err -> {
                btnExportPdf.setDisable(false);
                if (lblReportStatus != null) lblReportStatus.setText("Export failed.");
                showAlert("Failed to export PDF: " + (err != null ? err.getMessage() : "unknown"),
                        Alert.AlertType.ERROR);
            }
        );
    }

    private void showAlert(String msg, Alert.AlertType type) {
        new Alert(type, msg, ButtonType.OK).showAndWait();
    }
}
