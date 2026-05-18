package com.wifiin.newsay.ai.rag.controller;

import com.wifiin.newsay.ai.common.result.Result;
import com.wifiin.newsay.ai.rag.service.DocumentService;
import com.wifiin.newsay.ai.rag.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);
    private static final String PHONES_COLLECTION = "phones";

    private final VectorStoreService vectorStoreService;
    private final DocumentService documentService;

    public RagController(VectorStoreService vectorStoreService, DocumentService documentService) {
        this.vectorStoreService = vectorStoreService;
        this.documentService = documentService;
    }

    @GetMapping("/search")
    public Result<List<VectorStoreService.SearchResult>> search(
            @RequestParam(defaultValue = "照相声最好的3000元以内的手机") String q,
            @RequestParam(defaultValue = "5") int topK) {
        try {
            log.info("RAG search query: '{}', topK: {}", q, topK);
            var results = vectorStoreService.similaritySearch(PHONES_COLLECTION, q, topK);
            log.info("RAG search returned {} results", results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("RAG search failed", e);
            return Result.error("Search failed: " + e.getMessage());
        }
    }

    @GetMapping("/collections")
    public Result<Map<String, Object>> getCollections() {
        try {
            var stats = vectorStoreService.getCollectionStats(PHONES_COLLECTION);
            return Result.success(Map.of(
                "collection", PHONES_COLLECTION,
                "vectorCount", stats.vectorCount(),
                "dimension", stats.dimension()
            ));
        } catch (Exception e) {
            log.error("Failed to get collections", e);
            return Result.error("Failed to get collections: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<DocumentService.DocumentUploadResult> upload(
            @RequestParam(defaultValue = "phones") String collection,
            @RequestParam(defaultValue = "/root/coding_plan/newsay-server-ai/newsay-server-ai-rag/src/main/resources/docs/phones.md") String filePath) {
        try {
            log.info("Uploading document to collection: {}, path: {}", collection, filePath);
            var result = documentService.uploadDocument(collection, filePath);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Upload failed", e);
            return Result.error("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/init")
    public Result<String> initialize() {
        try {
            log.info("Initializing phone collection...");
            documentService.initializePhoneCollection();
            return Result.success("Phone collection initialized successfully");
        } catch (Exception e) {
            log.error("Initialization failed", e);
            return Result.error("Initialization failed: " + e.getMessage());
        }
    }
}
