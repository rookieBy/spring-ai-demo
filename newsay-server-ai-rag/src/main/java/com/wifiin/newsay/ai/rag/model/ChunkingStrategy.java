package com.wifiin.newsay.ai.rag.model;

public class ChunkingStrategy {
    private StrategyType type;
    private int chunkSize;
    private int overlap;
    private String separator;
    private boolean preserveStructure;

    public enum StrategyType {
        FIXED_SIZE,
        RECURSIVE,
        SEMANTIC,
        DOCUMENT_AWARE
    }

    public ChunkingStrategy() {
        this.type = StrategyType.FIXED_SIZE;
        this.chunkSize = 500;
        this.overlap = 50;
        this.separator = "\n";
        this.preserveStructure = false;
    }

    public ChunkingStrategy(StrategyType type, int chunkSize, int overlap) {
        this.type = type;
        this.chunkSize = chunkSize;
        this.overlap = overlap;
        this.separator = "\n";
        this.preserveStructure = false;
    }

    public static ChunkingStrategy defaultStrategy() {
        return new ChunkingStrategy(StrategyType.FIXED_SIZE, 500, 50);
    }

    public static ChunkingStrategy semanticStrategy() {
        return new ChunkingStrategy(StrategyType.SEMANTIC, 1000, 100);
    }

    public StrategyType getType() { return type; }
    public void setType(StrategyType type) { this.type = type; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    public int getOverlap() { return overlap; }
    public void setOverlap(int overlap) { this.overlap = overlap; }
    public String getSeparator() { return separator; }
    public void setSeparator(String separator) { this.separator = separator; }
    public boolean isPreserveStructure() { return preserveStructure; }
    public void setPreserveStructure(boolean preserveStructure) { this.preserveStructure = preserveStructure; }
}
