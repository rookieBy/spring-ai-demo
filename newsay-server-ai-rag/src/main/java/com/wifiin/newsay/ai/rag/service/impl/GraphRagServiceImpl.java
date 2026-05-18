package com.wifiin.newsay.ai.rag.service.impl;

import com.wifiin.newsay.ai.rag.model.kg.Entity;
import com.wifiin.newsay.ai.rag.model.kg.KnowledgeGraph;
import com.wifiin.newsay.ai.rag.model.kg.Relation;
import com.wifiin.newsay.ai.rag.service.GraphRagService;
import com.wifiin.newsay.ai.rag.service.VectorStoreService;
import com.wifiin.newsay.ai.rag.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphRagServiceImpl implements GraphRagService {

    private static final Logger log = LoggerFactory.getLogger(GraphRagServiceImpl.class);

    private final VectorStoreService vectorStoreService;
    private final EmbeddingService embeddingService;

    public GraphRagServiceImpl(VectorStoreService vectorStoreService, EmbeddingService embeddingService) {
        this.vectorStoreService = vectorStoreService;
        this.embeddingService = embeddingService;
    }

    @Override
    public KnowledgeGraph buildGraphFromDocument(String documentId) {
        log.info("Building knowledge graph from document: {}", documentId);
        return new KnowledgeGraph(UUID.randomUUID().toString(), documentId, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public GraphExtractionResult extractEntitiesAndRelations(String text) {
        if (text == null || text.isEmpty()) {
            return new GraphExtractionResult(List.of(), List.of(), Map.of());
        }

        List<Entity> entities = extractEntities(text);
        List<Relation> relations = extractRelations(text, entities);
        Map<String, Double> entityScores = new HashMap<>();
        entities.forEach(e -> entityScores.put(e.getId(), e.getConfidence()));

        return new GraphExtractionResult(entities, relations, entityScores);
    }

    @Override
    public String graphAugmentedRetrieval(String query, String collectionName, int depth) {
        List<VectorStoreService.SearchResult> searchResults = 
            vectorStoreService.similaritySearch(collectionName, query, 5);

        StringBuilder context = new StringBuilder();
        context.append("检索结果:\n");
        for (VectorStoreService.SearchResult result : searchResults) {
            context.append("- ").append(result.content()).append("\n");
        }

        List<Entity> linkedEntities = linkEntities(query);
        if (!linkedEntities.isEmpty()) {
            context.append("\n关联实体:\n");
            for (Entity entity : linkedEntities) {
                context.append("- ").append(entity.getName()).append(" (").append(entity.getType()).append(")\n");
            }
        }

        return context.toString();
    }

    @Override
    public List<Entity> linkEntities(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }

        String[] words = query.split("\\s+");
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            if (Character.isUpperCase(words[i].charAt(0))) {
                String name = words[i];
                if (i + 1 < words.length && Character.isUpperCase(words[i + 1].charAt(0))) {
                    name = name + " " + words[i + 1];
                }
                entities.add(new Entity(UUID.randomUUID().toString(), name, "ENTITY"));
            }
        }
        return entities;
    }

    @Override
    public List<RelationPath> findRelationPaths(String sourceEntity, String targetEntity, int maxHops) {
        return List.of();
    }

    @Override
    public KnowledgeGraph getSubgraph(Set<String> entityIds, int depth) {
        return new KnowledgeGraph(UUID.randomUUID().toString(), null, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public GraphStats getGraphStats(String collectionName) {
        VectorStoreService.CollectionStats stats = vectorStoreService.getCollectionStats(collectionName);
        return new GraphStats(stats.vectorCount(), 0, Map.of());
    }

    private List<Entity> extractEntities(String text) {
        List<Entity> entities = new ArrayList<>();
        String[] sentences = text.split("[.!?\n]");

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 20 && trimmed.length() < 100) {
                entities.add(new Entity(
                    UUID.randomUUID().toString(),
                    trimmed.substring(0, Math.min(30, trimmed.length())),
                    "PHRASE"
                ));
            }
        }
        return entities;
    }

    private List<Relation> extractRelations(String text, List<Entity> entities) {
        List<Relation> relations = new ArrayList<>();
        if (entities.size() < 2) {
            return relations;
        }

        for (int i = 0; i < entities.size() - 1; i++) {
            Entity source = entities.get(i);
            Entity target = entities.get(i + 1);
            relations.add(new Relation(
                UUID.randomUUID().toString(),
                source.getId(),
                target.getId(),
                "RELATED_TO"
            ));
        }
        return relations;
    }
}
