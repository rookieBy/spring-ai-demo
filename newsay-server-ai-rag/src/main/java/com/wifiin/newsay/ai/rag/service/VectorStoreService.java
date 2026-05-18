package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.DocumentChunk;

import java.util.List;
import java.util.Map;

public interface VectorStoreService {

    record SearchResult(String chunkId, String content, double score, Map<String, String> metadata) {}

    record CollectionStats(long vectorCount, int dimension) {}

    void createCollectionIfNotExists(String collectionName, int dimension);

    void insert(String collectionName, List<DocumentChunk> chunks);

    List<SearchResult> similaritySearch(String collectionName, String queryText, int topK);

    List<SearchResult> hybridSearch(String collectionName, String queryText, int topK);

    List<SearchResult> similaritySearchWithFilter(String collectionName, String queryText, int topK, Map<String, String> filter);

    void deleteByIds(String collectionName, List<String> ids);

    void dropCollection(String collectionName);

    CollectionStats getCollectionStats(String collectionName);
}
