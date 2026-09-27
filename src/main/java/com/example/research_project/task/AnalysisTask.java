package com.example.research_project.task;

import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;
import com.example.research_project.service.AIService;
import com.example.research_project.service.AnalysisService;
import javafx.concurrent.Task;

/**
 * AnalysisTask.java - Background thread for paper analysis.
 * Extends Task<AnalysisResult> - a JavaFX class for background work.
 * Supports Local Ollama (Qwen 2.5 3B) with seamless fallback to local rule-based NLP pipeline.
 * Reports progress via updateProgress() and updateMessage().
 */
public class AnalysisTask extends Task<AnalysisResult> {

    private final Paper paper;
    private final boolean useOllama;
    private final AIService aiService;
    private final AnalysisService analysisService;

    public AnalysisTask(Paper paper) {
        this(paper, false, null);
    }

    public AnalysisTask(Paper paper, boolean useOllama, AIService aiService) {
        this.paper = paper;
        this.useOllama = useOllama;
        this.aiService = (aiService != null) ? aiService : new AIService();
        this.analysisService = new AnalysisService();
    }

    @Override
    protected AnalysisResult call() throws Exception {
        // 1. Try local Ollama LLM if requested and accessible
        if (useOllama && aiService.isAvailable()) {
            updateMessage("Connecting to local Ollama (" + aiService.getModel() + ")...");
            updateProgress(1, 4);

            updateMessage("Generating local LLM synthesis via " + aiService.getModel() + "...");
            updateProgress(2, 4);

            AnalysisResult aiResult = aiService.analysePaper(paper);
            if (aiResult != null) {
                updateMessage("Ollama (" + aiService.getModel() + ") analysis complete! (" + aiResult.getProcessingTimeMs() + "ms)");
                updateProgress(4, 4);
                return aiResult;
            } else {
                updateMessage("Ollama returned null. Falling back to local algorithmic NLP...");
                Thread.sleep(300);
            }
        }

        // 2. Fallback to algorithmic NLP pipeline
        updateMessage("Starting algorithmic NLP analysis...");
        updateProgress(0, 5);

        updateMessage("Extracting keywords (frequency + stopword filtering)...");
        updateProgress(1, 5);
        Thread.sleep(120);

        updateMessage("Generating extractive summary (sentence scoring)...");
        updateProgress(2, 5);
        Thread.sleep(120);

        updateMessage("Classifying domain category (rule-based matching)...");
        updateProgress(3, 5);
        Thread.sleep(80);

        updateMessage("Extracting methodology and findings...");
        updateProgress(4, 5);
        Thread.sleep(80);

        AnalysisResult result = analysisService.analysePaper(paper);

        updateMessage("Algorithmic analysis complete! (" + result.getProcessingTimeMs() + "ms)");
        updateProgress(5, 5);

        return result;
    }
}
