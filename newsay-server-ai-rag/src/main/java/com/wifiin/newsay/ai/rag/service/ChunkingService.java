package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.ChunkingStrategy;
import com.wifiin.newsay.ai.rag.model.DocumentChunk;

import java.util.List;
import java.util.Map;

public interface ChunkingService {

    List<DocumentChunk> chunk(String content, ChunkingStrategy strategy, Map<String, String> metadata);

    List<DocumentChunk> chunkWithOverlap(String content, int chunkSize, int overlap, Map<String, String> metadata);

    List<DocumentChunk> smartChunk(String content, Map<String, String> metadata);
}
