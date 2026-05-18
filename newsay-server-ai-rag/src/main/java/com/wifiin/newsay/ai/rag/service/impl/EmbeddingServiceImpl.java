package com.wifiin.newsay.ai.rag.service.impl;

import com.wifiin.newsay.ai.rag.model.EmbeddingResult;
import com.wifiin.newsay.ai.rag.service.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final int DEFAULT_DIMENSION = 1536;

    @Override
    public EmbeddingResult embed(String text) {
        if (text == null || text.isEmpty()) {
            return new EmbeddingResult(new float[DEFAULT_DIMENSION], "", DEFAULT_DIMENSION);
        }

        float[] vector = generateFallbackEmbedding(text);
        return new EmbeddingResult(vector, text, vector.length);
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        return texts.stream()
            .map(this::embed)
            .toList();
    }

    @Override
    public double similarity(String text1, String text2) {
        EmbeddingResult emb1 = embed(text1);
        EmbeddingResult emb2 = embed(text2);
        return cosineSimilarity(emb1.getVector(), emb2.getVector());
    }

    @Override
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must be non-null and have same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public int getEmbeddingDimension() {
        return DEFAULT_DIMENSION;
    }

    private float[] generateFallbackEmbedding(String text) {
        float[] embedding = new float[DEFAULT_DIMENSION];
        if (text.isEmpty()) {
            return embedding;
        }

        for (int i = 0; i < DEFAULT_DIMENSION; i++) {
            embedding[i] = (float) (Math.sin(text.hashCode() + i * 0.1) * 0.1);
        }

        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DEFAULT_DIMENSION; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }
}
