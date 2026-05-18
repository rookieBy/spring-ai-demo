package com.wifiin.newsay.ai.rag.model.kg;

import java.util.Map;

public class Relation {
    private String id;
    private String sourceEntityId;
    private String targetEntityId;
    private String type;
    private String description;
    private Map<String, String> properties;
    private double confidence;

    public Relation() {}

    public Relation(String id, String sourceEntityId, String targetEntityId, String type) {
        this.id = id;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceEntityId() { return sourceEntityId; }
    public void setSourceEntityId(String sourceEntityId) { this.sourceEntityId = sourceEntityId; }
    public String getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(String targetEntityId) { this.targetEntityId = targetEntityId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getProperties() { return properties; }
    public void setProperties(Map<String, String> properties) { this.properties = properties; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
