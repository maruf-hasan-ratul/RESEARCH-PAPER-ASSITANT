package com.example.research_project.util;

import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/**
 * JsonUtil.java - JSON export and import using Gson.
 *
 * WHAT IS GSON?
 * Gson is a library from Google that converts Java objects to JSON text
 * and back. We added it to pom.xml as a dependency.
 *
 * EXAMPLE:
 *   Paper paper = new Paper();
 *   paper.setTitle("Deep Learning");
 *   String json = JsonUtil.exportPaperToJson(paper);
 *   // json -> { "title": "Deep Learning", "year": 0, ... }
 *
 *   Paper restored = JsonUtil.importPaperFromJson(json);
 *   // restored.getTitle() -> "Deep Learning"
 */
public class JsonUtil {

    private JsonUtil() {}

    // setPrettyPrinting() makes the JSON human-readable with indentation
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Converts a Paper object to a JSON string.
     */
    public static String exportPaperToJson(Paper paper) {
        return GSON.toJson(paper);
    }

    /**
     * Parses a JSON string back into a Paper object.
     */
    public static Paper importPaperFromJson(String json) {
        return GSON.fromJson(json, Paper.class);
    }

    /**
     * A "wrapper" class to bundle Paper + keywords + analysis for a richer JSON export.
     */
    public static class PaperExport {
        public Paper paper;
        public List<String> keywords;
        public String summary;
        public String category;
    }

    /**
     * Exports a paper with its keywords and analysis summary.
     */
    public static String exportFull(Paper paper, List<String> keywords,
                                    String summary, String category) {
        PaperExport export = new PaperExport();
        export.paper = paper;
        export.keywords = keywords;
        export.summary = summary;
        export.category = category;
        return GSON.toJson(export);
    }

    /**
     * Converts an AnalysisResult to a JSON string.
     */
    public static String exportAnalysisToJson(AnalysisResult result) {
        return GSON.toJson(result);
    }

    /**
     * Generic: converts any object to a pretty JSON string.
     * Useful for debugging.
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}