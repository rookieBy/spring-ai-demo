package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.EmbeddingResult;

import java.util.List;

public interface EmbeddingService {

    EmbeddingResult embed(String text);

    List<EmbeddingResult> embedBatch(List<String> texts);

    double similarity(String text1, String text2);

    double cosineSimilarity(float[] vec1, float[] vec2);

    int getEmbeddingDimension();
}
