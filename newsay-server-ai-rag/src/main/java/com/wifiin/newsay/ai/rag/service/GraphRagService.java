package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.kg.Entity;
import com.wifiin.newsay.ai.rag.model.kg.KnowledgeGraph;
import com.wifiin.newsay.ai.rag.model.kg.Relation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GraphRagService {

    record GraphExtractionResult(List<Entity> entities, List<Relation> relations, Map<String, Double> entityScores) {}

    record RelationPath(List<Entity> nodes, List<Relation> edges, double score) {}

    record GraphStats(long entityCount, long relationCount, Map<String, Long> entityTypeDistribution) {}

    KnowledgeGraph buildGraphFromDocument(String documentId);

    GraphExtractionResult extractEntitiesAndRelations(String text);

    String graphAugmentedRetrieval(String query, String collectionName, int depth);

    List<Entity> linkEntities(String query);

    List<RelationPath> findRelationPaths(String sourceEntity, String targetEntity, int maxHops);

    KnowledgeGraph getSubgraph(Set<String> entityIds, int depth);

    GraphStats getGraphStats(String collectionName);
}
