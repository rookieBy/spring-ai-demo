package com.wifiin.newsay.ai.rag.service.impl;

import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MilvusVectorStoreServiceImpl implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreServiceImpl.class);

    private final Map<String, Map<String, DocumentChunk>> collections = new HashMap<>();

    @Override
    public void createCollectionIfNotExists(String collectionName, int dimension) {
        log.info("Creating collection: {} with dimension: {}", collectionName, dimension);
        collections.computeIfAbsent(collectionName, k -> new HashMap<>());
    }

    @Override
    public void insert(String collectionName, List<DocumentChunk> chunks) {
        Map<String, DocumentChunk> collection = collections.computeIfAbsent(collectionName, k -> new HashMap<>());
        for (DocumentChunk chunk : chunks) {
            collection.put(chunk.getId(), chunk);
        }
        log.info("Inserted {} chunks into collection: {}", chunks.size(), collectionName);
    }

    @Override
    public List<SearchResult> similaritySearch(String collectionName, String queryText, int topK) {
        Map<String, DocumentChunk> collection = collections.get(collectionName);
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }

        List<SearchResult> results = new ArrayList<>();
        for (DocumentChunk chunk : collection.values()) {
            results.add(new SearchResult(chunk.getId(), chunk.getContent(), 0.9, chunk.getMetadata()));
        }

        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results.stream().limit(topK).toList();
    }

    @Override
    public List<SearchResult> hybridSearch(String collectionName, String queryText, int topK) {
        return similaritySearch(collectionName, queryText, topK);
    }

    @Override
    public List<SearchResult> similaritySearchWithFilter(String collectionName, String queryText, int topK, Map<String, String> filter) {
        List<SearchResult> results = similaritySearch(collectionName, queryText, topK * 2);
        if (filter == null || filter.isEmpty()) {
            return results.stream().limit(topK).toList();
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
            .toList();
    }

    @Override
    public void deleteByIds(String collectionName, List<String> ids) {
        Map<String, DocumentChunk> collection = collections.get(collectionName);
        if (collection != null) {
            ids.forEach(collection::remove);
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
        Map<String, DocumentChunk> collection = collections.get(collectionName);
        int count = collection != null ? collection.size() : 0;
        return new CollectionStats(count, 1536);
    }
}
