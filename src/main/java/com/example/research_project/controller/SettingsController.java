package com.example.research_project.controller;

import com.example.research_project.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

/**
 * Controller for the Settings view.
 * Provides controls for Light / Dark theme selection, base font size adjustments,
 * and system architecture inspection.
 */
public class SettingsController {

    @FXML private ToggleGroup themeToggleGroup;
    @FXML private RadioButton rbDark;
    @FXML private RadioButton rbLight;
    @FXML private Label lblCurrentThemeBadge;
    @FXML private Button btnQuickToggle;

    @FXML private ToggleGroup fontSizeToggleGroup;
    @FXML private RadioButton rbSmall;
    @FXML private RadioButton rbMedium;
    @FXML private RadioButton rbLarge;

    private final ThemeManager.ThemeListener themeListener = (theme, fontSize) -> updateUIState();

    @FXML
    public void initialize() {
        updateUIState();
        ThemeManager.addListener(themeListener);
    }

    private void updateUIState() {
        ThemeManager.Theme theme = ThemeManager.getTheme();
        if (theme == ThemeManager.Theme.LIGHT) {
            rbLight.setSelected(true);
            lblCurrentThemeBadge.setText("☀️ Light Mode Active");
            btnQuickToggle.setText("🌙 Switch to Dark Mode");
        } else {
            rbDark.setSelected(true);
            lblCurrentThemeBadge.setText("🌙 Dark Mode Active");
            btnQuickToggle.setText("☀️ Switch to Light Mode");
        }

        ThemeManager.FontSize fontSize = ThemeManager.getFontSize();
        switch (fontSize) {
            case SMALL:
                rbSmall.setSelected(true);
                break;
            case LARGE:
                rbLarge.setSelected(true);
                break;
            case MEDIUM:
            default:
                rbMedium.setSelected(true);
                break;
        }
    }

    @FXML
    public void onThemeSelected() {
        if (rbLight.isSelected()) {
            ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        } else if (rbDark.isSelected()) {
            ThemeManager.setTheme(ThemeManager.Theme.DARK);
        }
    }

    @FXML
    public void onQuickToggleTheme() {
        ThemeManager.toggleTheme();
    }

    @FXML
    public void onFontSizeSelected() {
        if (rbSmall.isSelected()) {
            ThemeManager.setFontSize(ThemeManager.FontSize.SMALL);
        } else if (rbLarge.isSelected()) {
            ThemeManager.setFontSize(ThemeManager.FontSize.LARGE);
        } else {
            ThemeManager.setFontSize(ThemeManager.FontSize.MEDIUM);
        }
    }
}
