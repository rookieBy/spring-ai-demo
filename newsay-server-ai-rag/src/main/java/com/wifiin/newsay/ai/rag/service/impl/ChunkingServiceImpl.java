package com.wifiin.newsay.ai.rag.service.impl;

import com.wifiin.newsay.ai.rag.model.ChunkingStrategy;
import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.service.ChunkingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChunkingServiceImpl implements ChunkingService {

    @Override
    public List<DocumentChunk> chunk(String content, ChunkingStrategy strategy, Map<String, String> metadata) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        return switch (strategy.getType()) {
            case FIXED_SIZE -> chunkFixedSize(content, strategy, metadata);
            case RECURSIVE -> chunkRecursive(content, strategy, metadata);
            case SEMANTIC -> chunkSemantic(content, strategy, metadata);
            case DOCUMENT_AWARE -> chunkDocumentAware(content, strategy, metadata);
        };
    }

    @Override
    public List<DocumentChunk> chunkWithOverlap(String content, int chunkSize, int overlap, Map<String, String> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String documentId = metadata != null ? metadata.get("documentId") : null;
        int index = 0;
        int position = 0;

        while (position < content.length()) {
            int end = Math.min(position + chunkSize, content.length());
            String chunkContent = content.substring(position, end);

            DocumentChunk chunk = new DocumentChunk(
                UUID.randomUUID().toString(),
                documentId,
                chunkContent,
                index,
                position,
                end
            );
            chunk.setMetadata(metadata);
            chunks.add(chunk);

            index++;
            position = position + chunkSize - overlap;
        }

        return chunks;
    }

    @Override
    public List<DocumentChunk> smartChunk(String content, Map<String, String> metadata) {
        ChunkingStrategy semanticStrategy = ChunkingStrategy.semanticStrategy();
        return chunkSemantic(content, semanticStrategy, metadata);
    }

    private List<DocumentChunk> chunkFixedSize(String content, ChunkingStrategy strategy, Map<String, String> metadata) {
        return chunkWithOverlap(content, strategy.getChunkSize(), strategy.getOverlap(), metadata);
    }

    private List<DocumentChunk> chunkRecursive(String content, ChunkingStrategy strategy, Map<String, String> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String documentId = metadata != null ? metadata.get("documentId") : null;
        String[] separators = {"\n\n", "\n", ". ", " "};
        int index = 0;
        int position = 0;

        for (String separator : separators) {
            if (chunks.size() >= 10) break;
            chunks.addAll(splitBySeparator(content, separator, strategy.getChunkSize(), documentId, metadata));
        }

        return chunks;
    }

    private List<DocumentChunk> chunkSemantic(String content, ChunkingStrategy strategy, Map<String, String> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String documentId = metadata != null ? metadata.get("documentId") : null;
        String[] paragraphs = content.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        int index = 0;
        int position = 0;

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > strategy.getChunkSize()) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(documentId, currentChunk.toString(), index, position, metadata));
                    index++;
                    position += currentChunk.length();
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(createChunk(documentId, currentChunk.toString().trim(), index, position, metadata));
        }

        return chunks;
    }

    private List<DocumentChunk> chunkDocumentAware(String content, ChunkingStrategy strategy, Map<String, String> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        String documentId = metadata != null ? metadata.get("documentId") : null;
        Pattern headerPattern = Pattern.compile("^#{1,6}\\s", Pattern.MULTILINE);
        String[] sections = headerPattern.split(content);

        int index = 0;
        int position = 0;

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            if (section.length() > strategy.getChunkSize()) {
                List<DocumentChunk> subChunks = chunkWithOverlap(section, strategy.getChunkSize(), strategy.getOverlap(), metadata);
                for (DocumentChunk subChunk : subChunks) {
                    subChunk.setChunkIndex(index);
                    chunks.add(subChunk);
                    index++;
                }
            } else {
                chunks.add(createChunk(documentId, section.trim(), index, position, metadata));
                index++;
            }
            position += section.length();
        }

        return chunks;
    }

    private List<DocumentChunk> splitBySeparator(String content, String separator, int chunkSize, String documentId, Map<String, String> metadata) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] parts = content.split(Pattern.quote(separator));
        StringBuilder current = new StringBuilder();
        int index = 0;
        int position = 0;

        for (String part : parts) {
            if (current.length() + part.length() > chunkSize && current.length() > 0) {
                chunks.add(createChunk(documentId, current.toString().trim(), index, position, metadata));
                index++;
                position += current.length();
                current = new StringBuilder();
            }
            current.append(part).append(separator);
        }

        if (current.length() > 0) {
            chunks.add(createChunk(documentId, current.toString().trim(), index, position, metadata));
        }

        return chunks;
    }

    private DocumentChunk createChunk(String documentId, String content, int index, int position, Map<String, String> metadata) {
        DocumentChunk chunk = new DocumentChunk(
            UUID.randomUUID().toString(),
            documentId,
            content,
            index,
            position,
            position + content.length()
        );
        chunk.setMetadata(metadata);
        return chunk;
    }
}
