package com.wifiin.newsay.ai.rag.model.kg;

import java.util.List;
import java.util.Map;

public class KnowledgeGraph {
    private String id;
    private String documentId;
    private List<Entity> entities;
    private List<Relation> relations;
    private Map<String, Object> metadata;

    public KnowledgeGraph() {}

    public KnowledgeGraph(String id, String documentId, List<Entity> entities, List<Relation> relations) {
        this.id = id;
        this.documentId = documentId;
        this.entities = entities;
        this.relations = relations;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public List<Entity> getEntities() { return entities; }
    public void setEntities(List<Entity> entities) { this.entities = entities; }
    public List<Relation> getRelations() { return relations; }
    public void setRelations(List<Relation> relations) { this.relations = relations; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
