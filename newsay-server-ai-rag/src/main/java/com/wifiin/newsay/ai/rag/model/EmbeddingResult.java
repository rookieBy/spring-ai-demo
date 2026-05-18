package com.wifiin.newsay.ai.rag.model;

public class EmbeddingResult {
    private float[] vector;
    private String text;
    private int dimension;

    public EmbeddingResult() {}

    public EmbeddingResult(float[] vector, String text, int dimension) {
        this.vector = vector;
        this.text = text;
        this.dimension = dimension;
    }

    public float[] getVector() { return vector; }
    public void setVector(float[] vector) { this.vector = vector; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getDimension() { return dimension; }
    public void setDimension(int dimension) { this.dimension = dimension; }
}
