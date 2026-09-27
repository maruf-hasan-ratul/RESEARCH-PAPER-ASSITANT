package com.example.research_project.util;

import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * JsonUtil.java - JSON export and import using org.json.
 *
 * Provides serialization and deserialization for Paper and AnalysisResult models
 * without relying on external reflection-based libraries like Gson.
 */
public class JsonUtil {

    private JsonUtil() {}

    /**
     * Converts a Paper object to an org.json JSONObject.
     */
    public static JSONObject paperToJsonObject(Paper paper) {
        if (paper == null) return new JSONObject();
        JSONObject obj = new JSONObject();
        obj.put("id", paper.getId());
        obj.put("title", paper.getTitle() != null ? paper.getTitle() : "");
        obj.put("authors", paper.getAuthors() != null ? paper.getAuthors() : "");
        obj.put("year", paper.getYear());
        obj.put("abstractText", paper.getAbstractText() != null ? paper.getAbstractText() : "");
        obj.put("methodology", paper.getMethodology() != null ? paper.getMethodology() : "");
        obj.put("findings", paper.getFindings() != null ? paper.getFindings() : "");
        obj.put("category", paper.getCategory() != null ? paper.getCategory() : "");
        obj.put("source", paper.getSource() != null ? paper.getSource() : "");
        obj.put("filePath", paper.getFilePath() != null ? paper.getFilePath() : "");
        obj.put("createdAt", paper.getCreatedAt() != null ? paper.getCreatedAt() : "");
        return obj;
    }

    /**
     * Converts a Paper object to a pretty-printed JSON string.
     */
    public static String exportPaperToJson(Paper paper) {
        return paperToJsonObject(paper).toString(2);
    }

    /**
     * Parses a JSON string back into a Paper object.
     */
    public static Paper importPaperFromJson(String json) {
        if (json == null || json.isBlank()) return null;
        JSONObject obj = new JSONObject(json);

        // If wrapped in a "paper" key
        if (obj.has("paper") && obj.get("paper") instanceof JSONObject) {
            obj = obj.getJSONObject("paper");
        }

        Paper paper = new Paper();
        if (obj.has("id")) paper.setId(obj.optInt("id", 0));
        if (obj.has("title")) paper.setTitle(obj.optString("title", ""));
        if (obj.has("authors")) paper.setAuthors(obj.optString("authors", ""));
        if (obj.has("year")) paper.setYear(obj.optInt("year", 0));
        if (obj.has("abstractText")) paper.setAbstractText(obj.optString("abstractText", ""));
        else if (obj.has("abstract")) paper.setAbstractText(obj.optString("abstract", ""));
        if (obj.has("methodology")) paper.setMethodology(obj.optString("methodology", ""));
        if (obj.has("findings")) paper.setFindings(obj.optString("findings", ""));
        if (obj.has("category")) paper.setCategory(obj.optString("category", ""));
        if (obj.has("source")) paper.setSource(obj.optString("source", ""));
        if (obj.has("filePath")) paper.setFilePath(obj.optString("filePath", ""));
        if (obj.has("createdAt")) paper.setCreatedAt(obj.optString("createdAt", ""));
        return paper;
    }

    /**
     * A wrapper class to bundle Paper + keywords + analysis for export.
     */
    public static class PaperExport {
        public Paper paper;
        public List<String> keywords;
        public String summary;
        public String category;
    }

    /**
     * Exports a paper with its keywords and analysis summary to a formatted JSON string.
     */
    public static String exportFull(Paper paper, List<String> keywords,
                                    String summary, String category) {
        JSONObject root = new JSONObject();
        root.put("paper", paperToJsonObject(paper));

        JSONArray kwArr = new JSONArray();
        if (keywords != null) {
            for (String kw : keywords) {
                kwArr.put(kw);
            }
        }
        root.put("keywords", kwArr);
        root.put("summary", summary != null ? summary : "");
        root.put("category", category != null ? category : "");
        return root.toString(2);
    }

    /**
     * Converts an AnalysisResult to a formatted JSON string.
     */
    public static String exportAnalysisToJson(AnalysisResult result) {
        if (result == null) return "{}";
        JSONObject obj = new JSONObject();
        obj.put("paperId", result.getPaperId());
        obj.put("summary", result.getSummary() != null ? result.getSummary() : "");

        JSONArray kwArr = new JSONArray();
        if (result.getKeywords() != null) {
            for (String kw : result.getKeywords()) {
                kwArr.put(kw);
            }
        }
        obj.put("keywords", kwArr);
        obj.put("category", result.getCategory() != null ? result.getCategory() : "");
        obj.put("methodology", result.getMethodology() != null ? result.getMethodology() : "");
        obj.put("findings", result.getFindings() != null ? result.getFindings() : "");
        obj.put("confidence", result.getConfidence());
        obj.put("processingTimeMs", result.getProcessingTimeMs());
        return obj.toString(2);
    }

    /**
     * Generic serialization to pretty-printed JSON string using org.json.
     */
    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Paper p) {
            return exportPaperToJson(p);
        }
        if (obj instanceof AnalysisResult ar) {
            return exportAnalysisToJson(ar);
        }
        if (obj instanceof PaperExport pe) {
            return exportFull(pe.paper, pe.keywords, pe.summary, pe.category);
        }
        return new JSONObject(obj).toString(2);
    }
}