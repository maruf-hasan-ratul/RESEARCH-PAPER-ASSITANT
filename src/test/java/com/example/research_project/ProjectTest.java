package com.example.research_project;

import com.example.research_project.database.Database;
import com.example.research_project.model.AnalysisResult;
import com.example.research_project.model.Paper;
import com.example.research_project.service.PaperService;
import com.example.research_project.util.JsonUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectTest {

    @BeforeAll
    public static void setup() {
        Database.initialise();
    }

    @Test
    public void testJsonUtilExportAndImport() {
        Paper paper = new Paper();
        paper.setId(101);
        paper.setTitle("Attention Is All You Need");
        paper.setAuthors("Vaswani et al.");
        paper.setYear(2017);
        paper.setCategory("Natural Language Processing");
        paper.setAbstractText("The dominant sequence transduction models are based on complex recurrent or convolutional neural networks...");

        // 1. Export Paper to JSON
        String json = JsonUtil.exportPaperToJson(paper);
        assertNotNull(json);
        assertTrue(json.contains("Attention Is All You Need"));
        assertTrue(json.contains("2017"));

        // 2. Import Paper from JSON
        Paper restored = JsonUtil.importPaperFromJson(json);
        assertNotNull(restored);
        assertEquals(101, restored.getId());
        assertEquals("Attention Is All You Need", restored.getTitle());
        assertEquals("Vaswani et al.", restored.getAuthors());
        assertEquals(2017, restored.getYear());
        assertEquals("Natural Language Processing", restored.getCategory());

        // 3. Export full bundle
        String fullJson = JsonUtil.exportFull(paper, List.of("transformer", "attention", "nlp"), "A seminal paper.", "NLP");
        assertNotNull(fullJson);
        assertTrue(fullJson.contains("transformer"));
        assertTrue(fullJson.contains("A seminal paper."));

        // 4. Export AnalysisResult to JSON
        AnalysisResult ar = new AnalysisResult(101, "Summary text", List.of("kw1", "kw2"), "AI", "Method", "Findings");
        String arJson = JsonUtil.exportAnalysisToJson(ar);
        assertNotNull(arJson);
        assertTrue(arJson.contains("Summary text"));
        assertTrue(arJson.contains("kw1"));
    }

    @Test
    public void testPaperServiceAndKeywordDAO() {
        PaperService service = new PaperService();

        Paper paper = new Paper();
        paper.setTitle("Unit Test Paper " + System.currentTimeMillis());
        paper.setAuthors("Tester");
        paper.setYear(2025);
        paper.setCategory("Computer Vision");
        paper.setAbstractText("This is a test abstract.");

        int id = service.addPaperWithKeywords(paper, List.of("vision", "deep-learning"));
        assertTrue(id > 0, "Paper ID should be greater than 0");

        Paper fetched = service.getPaperById(id);
        assertNotNull(fetched);
        assertEquals(paper.getTitle(), fetched.getTitle());

        List<String> keywords = service.getKeywordsForPaper(id);
        assertTrue(keywords.contains("vision"));
        assertTrue(keywords.contains("deep-learning"));

        // Cleanup
        boolean deleted = service.deletePaper(id);
        assertTrue(deleted);
    }

    @Test
    public void testFxmlFilesExistInClasspath() {
        // Core screens
        String[] fxmls = {
            "main.fxml", "dashboard.fxml", "papers.fxml",
            "add-paper.fxml", "analysis.fxml", "paper-details.fxml", "search.fxml",
            // Extended screens added in Step 2
            "favorites.fxml", "comparison.fxml", "reports.fxml"
        };

        for (String fxml : fxmls) {
            URL url = getClass().getResource("/com/example/research_project/fxml/" + fxml);
            assertNotNull(url, "Resource should exist: /com/example/research_project/fxml/" + fxml);
        }
    }

    @Test
    public void testPaperFavoriteAndReadingStatus() {
        // Default values
        Paper paper = new Paper();
        assertFalse(paper.isFavorite(), "Default favorite should be false");
        assertEquals("UNREAD", paper.getReadingStatus(), "Default reading status should be UNREAD");

        // Setting favorite
        paper.setFavorite(true);
        assertTrue(paper.isFavorite());

        // Setting reading status
        paper.setReadingStatus("READING");
        assertEquals("READING", paper.getReadingStatus());

        paper.setReadingStatus("COMPLETED");
        assertEquals("COMPLETED", paper.getReadingStatus());

        // Null/blank reading status should default to UNREAD
        paper.setReadingStatus(null);
        assertEquals("UNREAD", paper.getReadingStatus());

        paper.setReadingStatus("  ");
        assertEquals("UNREAD", paper.getReadingStatus());
    }

    @Test
    public void testPaperServiceFavoriteAndStatus() {
        PaperService service = new PaperService();

        Paper paper = new Paper();
        paper.setTitle("Favorite Test Paper " + System.currentTimeMillis());
        paper.setAuthors("Test Author");
        paper.setYear(2025);

        int id = service.addPaper(paper);
        assertTrue(id > 0, "Paper ID should be > 0");

        try {
            // Test toggling favorite
            boolean ok = service.setFavorite(id, true);
            assertTrue(ok, "setFavorite should succeed");

            Paper fetched = service.getPaperById(id);
            assertNotNull(fetched);
            assertTrue(fetched.isFavorite(), "Paper should be marked as favorite");

            // Test reading status update
            boolean statusOk = service.updateReadingStatus(id, "READING");
            assertTrue(statusOk, "updateReadingStatus should succeed");

            Paper fetched2 = service.getPaperById(id);
            assertNotNull(fetched2);
            assertEquals("READING", fetched2.getReadingStatus());

            // Favorites list should include this paper
            java.util.List<Paper> favorites = service.getFavoritePapers();
            assertTrue(favorites.stream().anyMatch(p -> p.getId() == id),
                       "Favorite papers list should contain paper id=" + id);
        } finally {
            // Always clean up
            service.deletePaper(id);
        }
    }
}
