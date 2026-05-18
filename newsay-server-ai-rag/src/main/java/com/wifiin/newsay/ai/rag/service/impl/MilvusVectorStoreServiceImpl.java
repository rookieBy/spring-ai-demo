package com.wifiin.newsay.ai.rag.service.impl;

import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.service.EmbeddingService;
import com.wifiin.newsay.ai.rag.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MilvusVectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreServiceImpl.class);

    private final EmbeddingService embeddingService;
    private final Map<String, List<DocumentChunk>> collections = new ConcurrentHashMap<>();

    public MilvusVectorStoreServiceImpl(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public void createCollectionIfNotExists(String collectionName, int dimension) {
        log.info("Creating collection: {} with dimension: {}", collectionName, dimension);
        collections.computeIfAbsent(collectionName, k -> new ArrayList<>());
    }

    @Override
    public void insert(String collectionName, List<DocumentChunk> chunks) {
        List<DocumentChunk> collection = collections.computeIfAbsent(collectionName, k -> new ArrayList<>());
        collection.addAll(chunks);
        log.info("Inserted {} chunks into collection: {} (total: {})", chunks.size(), collectionName, collection.size());
    }

    @Override
    public List<SearchResult> similaritySearch(String collectionName, String queryText, int topK) {
        List<DocumentChunk> collection = collections.get(collectionName);
        if (collection == null || collection.isEmpty()) {
            log.warn("Collection {} is empty or does not exist", collectionName);
            return List.of();
        }

        float[] queryVector = embeddingService.embed(queryText).getVector();
        
        List<SearchResult> results = new ArrayList<>();
        for (DocumentChunk chunk : collection) {
            float[] chunkVector = embeddingService.embed(chunk.getContent()).getVector();
            double score = embeddingService.cosineSimilarity(queryVector, chunkVector);
            results.add(new SearchResult(chunk.getId(), chunk.getContent(), score, chunk.getMetadata()));
        }

        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<SearchResult> topResults = results.stream().limit(topK).collect(Collectors.toList());
        
        log.info("Search '{}' returned {} results from {} chunks", queryText, topResults.size(), collection.size());
        return topResults;
    }

    @Override
    public List<SearchResult> hybridSearch(String collectionName, String queryText, int topK) {
        return similaritySearch(collectionName, queryText, topK);
    }

    @Override
    public List<SearchResult> similaritySearchWithFilter(String collectionName, String queryText, int topK, Map<String, String> filter) {
        List<SearchResult> results = similaritySearch(collectionName, queryText, topK * 2);
        if (filter == null || filter.isEmpty()) {
            return results.stream().limit(topK).collect(Collectors.toList());
        }

        return results.stream()
            .filter(r -> {
                if (r.metadata() == null) return false;
                for (Map.Entry<String, String> entry : filter.entrySet()) {
                    if (!entry.getValue().equals(r.metadata().get(entry.getKey()))) {
                        return false;
                    }
                }
                return true;
            })
            .limit(topK)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteByIds(String collectionName, List<String> ids) {
        List<DocumentChunk> collection = collections.get(collectionName);
        if (collection != null) {
            collection.removeIf(c -> ids.contains(c.getId()));
            log.info("Deleted {} chunks from collection: {}", ids.size(), collectionName);
        }
    }

    @Override
    public void dropCollection(String collectionName) {
        collections.remove(collectionName);
        log.info("Dropped collection: {}", collectionName);
    }

    @Override
    public CollectionStats getCollectionStats(String collectionName) {
        List<DocumentChunk> collection = collections.get(collectionName);
        int count = collection != null ? collection.size() : 0;
        return new CollectionStats(count, embeddingService.getEmbeddingDimension());
    }
}
