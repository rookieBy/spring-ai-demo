package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.EmbeddingResult;

import java.util.List;

public interface DocumentService {

    record DocumentUploadResult(int chunksCreated, String message) {}

    void loadAndChunkDocument(String collectionName, String filePath);

    List<EmbeddingResult> embedChunks(List<String> chunks);

    DocumentUploadResult uploadDocument(String collectionName, String filePath);

    void initializePhoneCollection();
}