package com.example.research_project.util;

import javafx.scene.Node;
import javafx.scene.Scene;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

/**
 * Manages UI themes (Light / Dark mode) and application font sizes.
 * Provides real-time switching, scene cascading, and persistent storage
 * using standard Java Preferences.
 */
public class ThemeManager {

    public enum Theme {
        LIGHT("Light Mode", "☀ Light"),
        DARK("Dark Mode", "🌙 Dark");

        private final String displayName;
        private final String shortLabel;

        Theme(String displayName, String shortLabel) {
            this.displayName = displayName;
            this.shortLabel = shortLabel;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getShortLabel() {
            return shortLabel;
        }
    }

    public enum FontSize {
        SMALL("font-small", "Small (11px)", 11),
        MEDIUM("font-medium", "Medium (13px)", 13),
        LARGE("font-large", "Large (15px)", 15);

        private final String styleClass;
        private final String displayName;
        private final int sizePx;

        FontSize(String styleClass, String displayName, int sizePx) {
            this.styleClass = styleClass;
            this.displayName = displayName;
            this.sizePx = sizePx;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getSizePx() {
            return sizePx;
        }
    }

    public interface ThemeListener {
        void onThemeChanged(Theme newTheme, FontSize newFontSize);
    }

    private static final String PREF_THEME = "app_theme";
    private static final String PREF_FONT_SIZE = "app_font_size";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private static final String DARK_CSS = "/com/example/research_project/css/dark.css";
    private static final String LIGHT_CSS = "/com/example/research_project/css/light.css";

    private static final Set<Scene> registeredScenes = Collections.newSetFromMap(new WeakHashMap<>());
    private static final List<ThemeListener> listeners = new ArrayList<>();

    private static Theme currentTheme = Theme.DARK;
    private static FontSize currentFontSize = FontSize.MEDIUM;

    static {
        // Load persisted settings (default to DARK and MEDIUM)
        try {
            String savedTheme = prefs.get(PREF_THEME, Theme.DARK.name());
            currentTheme = Theme.valueOf(savedTheme);
        } catch (Exception e) {
            currentTheme = Theme.DARK;
        }

        try {
            String savedFontSize = prefs.get(PREF_FONT_SIZE, FontSize.MEDIUM.name());
            currentFontSize = FontSize.valueOf(savedFontSize);
        } catch (Exception e) {
            currentFontSize = FontSize.MEDIUM;
        }
    }

    public static Theme getTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public static FontSize getFontSize() {
        return currentFontSize;
    }

    public static void registerScene(Scene scene) {
        if (scene != null) {
            registeredScenes.add(scene);
            applyCurrentToScene(scene);
        }
    }

    public static void addListener(ThemeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(ThemeListener listener) {
        listeners.remove(listener);
    }

    public static void setTheme(Theme theme) {
        if (theme == null) return;
        currentTheme = theme;

        try {
            prefs.put(PREF_THEME, theme.name());
        } catch (Exception ignored) {}

        applyToAll();
        notifyListeners();
    }

    public static void toggleTheme() {
        if (currentTheme == Theme.DARK) {
            setTheme(Theme.LIGHT);
        } else {
            setTheme(Theme.DARK);
        }
    }

    public static void setFontSize(FontSize size) {
        if (size == null) return;
        currentFontSize = size;

        try {
            prefs.put(PREF_FONT_SIZE, size.name());
        } catch (Exception ignored) {}

        applyToAll();
        notifyListeners();
    }

    public static void applyTheme(Scene scene) {
        applyCurrentToScene(scene);
    }

    public static void applyThemeToNode(Node node) {
        if (node == null) return;
        if (node instanceof javafx.scene.Parent parent) {
            parent.getStylesheets().clear();
            String cssPath = (currentTheme == Theme.DARK) ? DARK_CSS : LIGHT_CSS;
            URL url = ThemeManager.class.getResource(cssPath);
            if (url == null) {
                url = ThemeManager.class.getResource(currentTheme == Theme.DARK ? "/css/dark.css" : "/css/light.css");
            }
            if (url != null) {
                parent.getStylesheets().add(url.toExternalForm());
            }
        }

        node.getStyleClass().removeAll("font-small", "font-medium", "font-large");
        node.getStyleClass().add(currentFontSize.getStyleClass());
    }

    private static void applyToAll() {
        for (Scene scene : registeredScenes) {
            applyCurrentToScene(scene);
        }
    }

    private static void applyCurrentToScene(Scene scene) {
        if (scene == null) return;

        String cssPath = (currentTheme == Theme.DARK) ? DARK_CSS : LIGHT_CSS;
        URL url = ThemeManager.class.getResource(cssPath);
        if (url == null) {
            url = ThemeManager.class.getResource(currentTheme == Theme.DARK ? "/css/dark.css" : "/css/light.css");
        }

        scene.getStylesheets().clear();
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }

        Node root = scene.getRoot();
        if (root != null) {
            root.getStyleClass().removeAll("font-small", "font-medium", "font-large");
            root.getStyleClass().add(currentFontSize.getStyleClass());
        }
    }

    private static void notifyListeners() {
        for (ThemeListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onThemeChanged(currentTheme, currentFontSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
