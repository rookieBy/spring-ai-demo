package com.wifiin.newsay.ai.rag.service.impl;

import com.wifiin.newsay.ai.rag.model.ChunkingStrategy;
import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.model.EmbeddingResult;
import com.wifiin.newsay.ai.rag.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    private static final String PHONES_DOC_PATH = "classpath:docs/phones.md";
    private static final String PHONES_COLLECTION = "phones";

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public DocumentServiceImpl(ChunkingService chunkingService,
                               EmbeddingService embeddingService,
                               VectorStoreService vectorStoreService) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public void loadAndChunkDocument(String collectionName, String filePath) {
        try {
            String content = Files.readString(Path.of(filePath));
            Map<String, String> metadata = Map.of("source", filePath, "type", "phone_spec");
            List<DocumentChunk> chunks = chunkingService.smartChunk(content, metadata);
            vectorStoreService.createCollectionIfNotExists(collectionName, embeddingService.getEmbeddingDimension());
            vectorStoreService.insert(collectionName, chunks);
            log.info("Loaded and chunked document: {} chunks created", chunks.size());
        } catch (IOException e) {
            log.error("Failed to load document: {}", filePath, e);
            throw new RuntimeException("Failed to load document: " + filePath, e);
        }
    }

    @Override
    public List<EmbeddingResult> embedChunks(List<String> chunks) {
        return embeddingService.embedBatch(chunks);
    }

    @Override
    public DocumentUploadResult uploadDocument(String collectionName, String filePath) {
        try {
            String content = Files.readString(Path.of(filePath));
            Map<String, String> metadata = Map.of("source", filePath, "type", "phone_spec");
            List<DocumentChunk> chunks = chunkingService.smartChunk(content, metadata);
            List<EmbeddingResult> embeddings = embeddingService.embedBatch(
                chunks.stream().map(DocumentChunk::getContent).toList()
            );
            vectorStoreService.createCollectionIfNotExists(collectionName, embeddingService.getEmbeddingDimension());
            vectorStoreService.insert(collectionName, chunks);
            log.info("Uploaded document to collection {}: {} chunks", collectionName, chunks.size());
            return new DocumentUploadResult(chunks.size(), "Document uploaded successfully");
        } catch (IOException e) {
            log.error("Failed to upload document: {}", filePath, e);
            return new DocumentUploadResult(0, "Failed to upload document: " + e.getMessage());
        }
    }

    @Override
    public void initializePhoneCollection() {
        try {
            var resourceStream = getClass().getResourceAsStream("/docs/phones.md");
            if (resourceStream == null) {
                log.warn("phones.md not found in classpath, skipping initialization");
                return;
            }
            String content = new String(resourceStream.readAllBytes());
            Map<String, String> metadata = Map.of("source", "phones.md", "type", "phone_spec");
            List<DocumentChunk> chunks = chunkingService.smartChunk(content, metadata);
            vectorStoreService.createCollectionIfNotExists(PHONES_COLLECTION, embeddingService.getEmbeddingDimension());
            vectorStoreService.insert(PHONES_COLLECTION, chunks);
            log.info("Initialized phone collection with {} chunks", chunks.size());
        } catch (Exception e) {
            log.error("Failed to initialize phone collection", e);
        }
    }
}