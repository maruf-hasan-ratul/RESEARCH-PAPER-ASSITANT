package com.example.research_project.service;

import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * AIService.java - Local LLM integration via Ollama (Qwen 2.5 3B) using org.json.
 *
 * FEATURES:
 * - Direct HTTP integration with local Ollama API (http://localhost:11434).
 * - Utilizes local 'qwen2.5:3b' model for fast, offline, and private paper synthesis.
 * - Enforces strict JSON output format via Ollama's "format": "json" parameter.
 * - Configuration sources:
 *     1. System Properties: -Dollama.host=... -Dollama.model=...
 *     2. Environment Variables: OLLAMA_HOST, OLLAMA_MODEL
 *     3. Configuration file: config.properties
 * - Graceful fallback: If Ollama is not running, safely returns null to allow fallback to local NLP.
 */
public class AIService {

    public static final String DEFAULT_HOST = "http://localhost:11434";
    public static final String DEFAULT_MODEL = "qwen2.5:3b";
    private static final String CONFIG_FILE = "config.properties";

    private final HttpClient httpClient;
    private String host;
    private String model;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        loadConfiguration();
    }

    private void loadConfiguration() {
        // 1. Host resolution
        String resolvedHost = System.getProperty("ollama.host");
        if (resolvedHost == null || resolvedHost.isBlank()) {
            resolvedHost = System.getenv("OLLAMA_HOST");
        }

        // 2. Model resolution
        String resolvedModel = System.getProperty("ollama.model");
        if (resolvedModel == null || resolvedModel.isBlank()) {
            resolvedModel = System.getenv("OLLAMA_MODEL");
        }

        // 3. config.properties file fallback
        File propFile = new File(CONFIG_FILE);
        if (propFile.exists()) {
            try (FileInputStream in = new FileInputStream(propFile)) {
                Properties props = new Properties();
                props.load(in);
                if (resolvedHost == null || resolvedHost.isBlank()) {
                    resolvedHost = props.getProperty("ollama.host");
                }
                if (resolvedModel == null || resolvedModel.isBlank()) {
                    resolvedModel = props.getProperty("ollama.model");
                }
            } catch (IOException ignored) {
            }
        }

        this.host = (resolvedHost != null && !resolvedHost.isBlank())
                ? trimTrailingSlash(resolvedHost.trim()) : DEFAULT_HOST;
        this.model = (resolvedModel != null && !resolvedModel.isBlank())
                ? resolvedModel.trim() : DEFAULT_MODEL;
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = (host != null && !host.isBlank()) ? trimTrailingSlash(host.trim()) : DEFAULT_HOST;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = (model != null && !model.isBlank()) ? model.trim() : DEFAULT_MODEL;
    }

    /**
     * Persists host and model settings to config.properties.
     */
    public void saveSettings(String newHost, String newModel) throws IOException {
        setHost(newHost);
        setModel(newModel);

        Properties props = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }
        }

        props.setProperty("ollama.host", this.host);
        props.setProperty("ollama.model", this.model);

        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "AI Research Assistant - Local Ollama Settings");
        }
    }

    /**
     * Checks if the local Ollama instance is running and reachable.
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tests connectivity to Ollama and verifies if the configured model is installed.
     * Returns "OK" or descriptive status.
     */
    public String testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/api/tags"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "Ollama returned HTTP " + response.statusCode();
            }

            // Check if model is listed in /api/tags
            JSONObject json = new JSONObject(response.body());
            if (json.has("models")) {
                JSONArray models = json.getJSONArray("models");
                boolean foundConfigured = false;
                List<String> available = new ArrayList<>();
                for (int i = 0; i < models.length(); i++) {
                    JSONObject el = models.optJSONObject(i);
                    if (el != null && el.has("name")) {
                        String name = el.getString("name");
                        available.add(name);
                        if (name.equalsIgnoreCase(model) || name.startsWith(model + ":") || model.startsWith(name)) {
                            foundConfigured = true;
                        }
                    }
                }
                if (foundConfigured) {
                    return "OK";
                } else {
                    return "Ollama is running, but model '" + model + "' is not installed. Installed: " + available;
                }
            }
            return "OK";
        } catch (Exception e) {
            return "Cannot connect to Ollama at " + host + ": " + e.getMessage();
        }
    }

    /**
     * Analyses a research paper using local Ollama Qwen 3B.
     * Returns an AnalysisResult, or null if Ollama is unreachable.
     *
     * @param paper The paper to analyse
     * @return AnalysisResult with summary, keywords, category, methodology, findings, or null
     */
    public AnalysisResult analysePaper(Paper paper) {
        if (paper == null) {
            return null;
        }

        long start = System.currentTimeMillis();
        String prompt = buildPaperAnalysisPrompt(paper);

        JSONObject body = new JSONObject();
        body.put("model", this.model);
        body.put("prompt", prompt);
        body.put("stream", false);
        body.put("format", "json");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                System.err.println("AIService: Ollama returned HTTP " + response.statusCode() + " - " + response.body());
                return null;
            }

            JSONObject responseJson = new JSONObject(response.body());
            if (!responseJson.has("response")) {
                System.err.println("AIService: No 'response' field in Ollama output.");
                return null;
            }

            String content = responseJson.getString("response");
            AnalysisResult result = parseJsonResult(content, paper.getId());
            long elapsed = System.currentTimeMillis() - start;
            result.setProcessingTimeMs(elapsed);
            result.setConfidence(0.95);

            return result;

        } catch (Exception e) {
            System.err.println("AIService: Error querying local Ollama (" + model + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Backward-compatible overload for string text input.
     */
    public AnalysisResult analyseWithAI(String text) {
        if (text == null || text.isBlank()) return null;
        Paper dummy = new Paper();
        dummy.setTitle("Text Document");
        dummy.setAbstractText(text);
        return analysePaper(dummy);
    }

    private String buildPaperAnalysisPrompt(Paper paper) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI academic research assistant.\n");
        sb.append("Analyze the following research paper and respond in strict JSON format.\n\n");
        sb.append("Paper Title: ").append(paper.getTitle() != null ? paper.getTitle() : "N/A").append("\n");
        sb.append("Authors: ").append(paper.getAuthors() != null ? paper.getAuthors() : "N/A").append("\n");
        sb.append("Domain Category: ").append(paper.getCategory() != null ? paper.getCategory() : "N/A").append("\n");
        sb.append("Abstract:\n").append(paper.getAbstractText() != null ? paper.getAbstractText() : "N/A").append("\n\n");

        if (paper.getMethodology() != null && !paper.getMethodology().isBlank()) {
            sb.append("Methodology Notes:\n").append(paper.getMethodology()).append("\n\n");
        }
        if (paper.getFindings() != null && !paper.getFindings().isBlank()) {
            sb.append("Findings Notes:\n").append(paper.getFindings()).append("\n\n");
        }

        sb.append("Respond ONLY with a JSON object matching this schema:\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"A concise 2 to 3 sentence summary of the paper's core contribution and conclusions.\",\n");
        sb.append("  \"keywords\": [\"keyword1\", \"keyword2\", \"keyword3\", \"keyword4\", \"keyword5\"],\n");
        sb.append("  \"category\": \"Primary academic domain/field\",\n");
        sb.append("  \"methodology\": \"Concise synthesis of algorithms, models, datasets, or architectures employed.\",\n");
        sb.append("  \"findings\": \"Key empirical results, quantitative metrics, benchmarks, or discoveries.\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    private AnalysisResult parseJsonResult(String rawText, int paperId) {
        String clean = rawText.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();

        JSONObject obj = new JSONObject(clean);

        String summary = obj.optString("summary", "");

        List<String> keywords = new ArrayList<>();
        if (obj.has("keywords")) {
            JSONArray arr = obj.optJSONArray("keywords");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String kw = arr.optString(i, "");
                    if (!kw.isBlank()) keywords.add(kw);
                }
            }
        }

        String category = obj.optString("category", "General");
        String methodology = obj.optString("methodology", "");
        String findings = obj.optString("findings", "");

        return new AnalysisResult(paperId, summary, keywords, category, methodology, findings);
    }
}