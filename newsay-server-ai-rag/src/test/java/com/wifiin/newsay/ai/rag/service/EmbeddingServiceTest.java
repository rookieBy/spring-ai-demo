package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.EmbeddingResult;
import com.wifiin.newsay.ai.rag.service.impl.EmbeddingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingServiceImpl();
    }

    @Test
    @DisplayName("embed should return valid embedding result")
    void embed_shouldReturnValidEmbeddingResult() {
        EmbeddingResult result = embeddingService.embed("Hello world");

        assertNotNull(result);
        assertNotNull(result.getVector());
        assertTrue(result.getDimension() > 0);
        assertEquals("Hello world", result.getText());
    }

    @Test
    @DisplayName("embed should handle empty string")
    void embed_shouldHandleEmptyString() {
        EmbeddingResult result = embeddingService.embed("");

        assertNotNull(result);
        assertNotNull(result.getVector());
    }

    @Test
    @DisplayName("embedBatch should return results for multiple texts")
    void embedBatch_shouldReturnResultsForMultipleTexts() {
        List<String> texts = List.of("Hello", "World", "Test");

        List<EmbeddingResult> results = embeddingService.embedBatch(texts);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(r -> r.getVector() != null));
    }

    @Test
    @DisplayName("embedBatch should handle empty list")
    void embedBatch_shouldHandleEmptyList() {
        List<EmbeddingResult> results = embeddingService.embedBatch(List.of());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("cosineSimilarity should return 1 for identical vectors")
    void cosineSimilarity_shouldReturn1ForIdenticalVectors() {
        float[] vec = new float[]{1.0f, 0.0f, 0.0f};

        double similarity = embeddingService.cosineSimilarity(vec, vec);

        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    @DisplayName("cosineSimilarity should return 0 for orthogonal vectors")
    void cosineSimilarity_shouldReturn0ForOrthogonalVectors() {
        float[] vec1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] vec2 = new float[]{0.0f, 1.0f, 0.0f};

        double similarity = embeddingService.cosineSimilarity(vec1, vec2);

        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    @DisplayName("cosineSimilarity should return -1 for opposite vectors")
    void cosineSimilarity_shouldReturnNegative1ForOppositeVectors() {
        float[] vec1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] vec2 = new float[]{-1.0f, 0.0f, 0.0f};

        double similarity = embeddingService.cosineSimilarity(vec1, vec2);

        assertEquals(-1.0, similarity, 0.0001);
    }

    @Test
    @DisplayName("getEmbeddingDimension should return positive value")
    void getEmbeddingDimension_shouldReturnPositiveValue() {
        int dimension = embeddingService.getEmbeddingDimension();

        assertTrue(dimension > 0);
    }

    @Test
    @DisplayName("similarity should return value between -1 and 1")
    void similarity_shouldReturnValueBetweenMinus1And1() {
        double similarity = embeddingService.similarity("Hello", "World");

        assertTrue(similarity >= -1.0 && similarity <= 1.0);
    }
}
