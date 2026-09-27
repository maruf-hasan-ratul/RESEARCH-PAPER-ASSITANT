package com.example.research_project.service;

import java.util.*;

/**
 * ClassificationService.java - Rule-based topic classification.
 *
 * HOW IT WORKS:
 * We define keyword sets for each category.
 * The text is lowercased and checked for how many keywords from each
 * category it contains. The category with the highest match count wins.
 *
 * This is a rule-based approach - simple but effective for a university project.
 * It can be replaced by a machine learning classifier in the future.
 */
public class ClassificationService {

    // Map: category name -> list of trigger keywords
    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("Computer Vision", Arrays.asList(
            "image", "vision", "cnn", "convolutional", "detection",
            "segmentation", "recognition", "object", "pixel", "visual",
            "resnet", "yolo", "vgg", "generative", "gan", "diffusion"
        ));
        RULES.put("Natural Language Processing", Arrays.asList(
            "nlp", "language", "text", "word", "sentence", "corpus",
            "transformer", "bert", "gpt", "embedding", "token",
            "sentiment", "translation", "summarization", "parsing"
        ));
        RULES.put("Machine Learning", Arrays.asList(
            "machine learning", "neural network", "deep learning",
            "training", "model", "dataset", "accuracy", "gradient",
            "backpropagation", "overfitting", "regularization",
            "classification", "regression", "clustering", "svm"
        ));
        RULES.put("Artificial Intelligence", Arrays.asList(
            "artificial intelligence", "ai", "intelligent", "reasoning",
            "planning", "knowledge", "inference", "expert system",
            "fuzzy", "agent", "heuristic"
        ));
        RULES.put("Cyber Security", Arrays.asList(
            "security", "attack", "threat", "vulnerability", "encryption",
            "malware", "intrusion", "detection", "firewall", "authentication",
            "cryptography", "cyber", "phishing", "ransomware"
        ));
        RULES.put("Data Science", Arrays.asList(
            "data", "analytics", "statistics", "visualization",
            "big data", "feature", "preprocessing", "exploratory",
            "pandas", "spark", "hadoop", "sql", "database"
        ));
        RULES.put("IoT", Arrays.asList(
            "iot", "internet of things", "sensor", "embedded",
            "microcontroller", "raspberry", "arduino", "edge computing",
            "smart device", "wireless", "mqtt", "zigbee"
        ));
        RULES.put("Robotics", Arrays.asList(
            "robot", "robotics", "autonomous", "navigation", "actuator",
            "kinematics", "path planning", "manipulation", "drone", "uav"
        ));
        RULES.put("Software Engineering", Arrays.asList(
            "software", "agile", "testing", "bug", "refactoring",
            "design pattern", "architecture", "microservice", "devops",
            "continuous integration", "uml", "requirements"
        ));
    }

    /**
     * Classifies the paper text into a topic category.
     *
     * @param text  Combined paper text (title + abstract + methodology + findings)
     * @return      Category name (never null, defaults to "Other")
     */
    public String classify(String text) {
        if (text == null || text.isBlank()) return "Other";

        String lower = text.toLowerCase();
        String bestCategory = "Other";
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : RULES.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }
}