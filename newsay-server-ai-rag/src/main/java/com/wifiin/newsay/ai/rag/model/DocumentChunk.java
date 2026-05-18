package com.wifiin.newsay.ai.rag.model;

import java.time.LocalDateTime;
import java.util.Map;

public class DocumentChunk {
    private String id;
    private String documentId;
    private String content;
    private int chunkIndex;
    private int startPosition;
    private int endPosition;
    private float[] embedding;
    private LocalDateTime createdAt;
    private Map<String, String> metadata;

    public DocumentChunk() {}

    public DocumentChunk(String id, String documentId, String content, int chunkIndex,
                        int startPosition, int endPosition) {
        this.id = id;
        this.documentId = documentId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public int getStartPosition() { return startPosition; }
    public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
    public int getEndPosition() { return endPosition; }
    public void setEndPosition(int endPosition) { this.endPosition = endPosition; }
    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
