package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.ChunkingStrategy;
import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.service.impl.ChunkingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingServiceImpl();
    }

    @Test
    @DisplayName("chunkWithOverlap should split content correctly")
    void chunkWithOverlap_shouldSplitContentCorrectly() {
        String content = "This is a test document. It has multiple sentences. We need to chunk it properly.";
        Map<String, String> metadata = Map.of("documentId", "doc-1");

        List<DocumentChunk> chunks = chunkingService.chunkWithOverlap(content, 20, 5, metadata);

        assertFalse(chunks.isEmpty());
        assertEquals("doc-1", chunks.get(0).getDocumentId());
        assertNotNull(chunks.get(0).getContent());
    }

    @Test
    @DisplayName("chunkWithOverlap should handle empty content")
    void chunkWithOverlap_shouldHandleEmptyContent() {
        List<DocumentChunk> chunks = chunkingService.chunkWithOverlap("", 20, 5, Map.of());
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("chunkWithOverlap should handle null content")
    void chunkWithOverlap_shouldHandleNullContent() {
        List<DocumentChunk> chunks = chunkingService.chunkWithOverlap(null, 20, 5, Map.of());
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("chunk with FIXED_SIZE strategy should work")
    void chunk_withFixedSizeStrategy_shouldWork() {
        String content = "A".repeat(1000);
        ChunkingStrategy strategy = new ChunkingStrategy(ChunkingStrategy.StrategyType.FIXED_SIZE, 100, 10);

        List<DocumentChunk> chunks = chunkingService.chunk(content, strategy, Map.of("documentId", "doc-1"));

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
    }

    @Test
    @DisplayName("smartChunk should respect semantic boundaries")
    void smartChunk_shouldRespectSemanticBoundaries() {
        String content = "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.";
        Map<String, String> metadata = Map.of("documentId", "doc-1");

        List<DocumentChunk> chunks = chunkingService.smartChunk(content, metadata);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).getContent().contains("First paragraph"));
    }

    @Test
    @DisplayName("chunks should have correct metadata")
    void chunks_shouldHaveCorrectMetadata() {
        String content = "Test content for metadata";
        Map<String, String> metadata = Map.of("documentId", "doc-123", "source", "test");

        List<DocumentChunk> chunks = chunkingService.chunkWithOverlap(content, 100, 10, metadata);

        assertFalse(chunks.isEmpty());
        assertEquals("doc-123", chunks.get(0).getMetadata().get("documentId"));
        assertEquals("test", chunks.get(0).getMetadata().get("source"));
    }

    @Test
    @DisplayName("chunks should have sequential indices")
    void chunks_shouldHaveSequentialIndices() {
        String content = "Chunk 1.\n\nChunk 2.\n\nChunk 3.";
        Map<String, String> metadata = Map.of("documentId", "doc-1");

        List<DocumentChunk> chunks = chunkingService.smartChunk(content, metadata);

        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex());
        }
    }

    @Test
    @DisplayName("chunk should calculate correct positions")
    void chunk_shouldCalculateCorrectPositions() {
        String content = "This is a very long document that needs to be split into multiple chunks with accurate position tracking.";
        Map<String, String> metadata = Map.of("documentId", "doc-1");

        List<DocumentChunk> chunks = chunkingService.chunkWithOverlap(content, 30, 5, metadata);

        assertFalse(chunks.isEmpty());
        DocumentChunk first = chunks.get(0);
        assertTrue(first.getEndPosition() > first.getStartPosition());
        assertEquals(first.getStartPosition() + first.getContent().length(), first.getEndPosition());
    }
}
