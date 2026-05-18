package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.service.impl.ChunkingServiceImpl;
import com.wifiin.newsay.ai.rag.service.impl.DocumentServiceImpl;
import com.wifiin.newsay.ai.rag.service.impl.EmbeddingServiceImpl;
import com.wifiin.newsay.ai.rag.service.impl.MilvusVectorStoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentService 测试")
class DocumentServiceTest {

    private DocumentService documentService;
    private VectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        ChunkingService chunkingService = new ChunkingServiceImpl();
        EmbeddingService embeddingService = new EmbeddingServiceImpl();
        vectorStoreService = new MilvusVectorStoreServiceImpl(embeddingService);
        documentService = new DocumentServiceImpl(chunkingService, embeddingService, vectorStoreService);
    }

    @Test
    @DisplayName("smartChunk should split document into semantic chunks")
    void smartChunk_shouldSplitIntoSemanticChunks() {
        String content = "## 手机A\n\n这是一款拍照手机。\n\n价格：3000元\n\n优点：拍照好\n\n## 手机B\n\n这是一款游戏手机。\n\n价格：2000元\n\n优点：性能强";

        ChunkingService chunkingService = new ChunkingServiceImpl();
        List<DocumentChunk> chunks = chunkingService.smartChunk(content, Map.of("source", "test"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getContent()).contains("手机A");
    }

    @Test
    @DisplayName("uploadDocument should return chunks created count")
    void uploadDocument_shouldReturnChunkCount() {
        String testFilePath = "/tmp/test_phones.md";

        DocumentService.DocumentUploadResult result = documentService.uploadDocument("test_collection", testFilePath);

        assertThat(result.chunksCreated()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("vectorStore should store and retrieve chunks")
    void vectorStore_shouldStoreAndRetrieve() {
        ChunkingService chunkingService = new ChunkingServiceImpl();
        String content = "## 苹果 iPhone 16 Pro Max\n\n价格：9999元\n\n摄像头：4800万像素\n\n优点：拍照顶级";
        List<DocumentChunk> chunks = chunkingService.smartChunk(content, Map.of("brand", "Apple"));

        vectorStoreService.createCollectionIfNotExists("test_phones", 1536);
        vectorStoreService.insert("test_phones", chunks);

        VectorStoreService.CollectionStats stats = vectorStoreService.getCollectionStats("test_phones");
        assertThat(stats.vectorCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("similaritySearch should find relevant chunks by query")
    void similaritySearch_shouldFindRelevantChunks() {
        ChunkingService chunkingService = new ChunkingServiceImpl();
        EmbeddingService embeddingService = new EmbeddingServiceImpl();

        String phone1 = "## 小米 14 Ultra\n\n价格：5999元\n\n摄像头：5000万像素徕卡镜头\n\n优点：拍照出色";
        String phone2 = "## OPPO Find X8 Pro\n\n价格：4999元\n\n摄像头：5000万像素\n\n优点：长焦优秀";

        List<DocumentChunk> chunks1 = chunkingService.smartChunk(phone1, Map.of("brand", "小米", "price", "5999"));
        List<DocumentChunk> chunks2 = chunkingService.smartChunk(phone2, Map.of("brand", "OPPO", "price", "4999"));

        vectorStoreService.createCollectionIfNotExists("camera_phones", 1536);
        vectorStoreService.insert("camera_phones", chunks1);
        vectorStoreService.insert("camera_phones", chunks2);

        List<VectorStoreService.SearchResult> results =
            vectorStoreService.similaritySearch("camera_phones", "照相功能最好的手机", 2);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("embedChunks should generate embeddings for chunks")
    void embedChunks_shouldGenerateEmbeddings() {
        ChunkingService chunkingService = new ChunkingServiceImpl();
        EmbeddingService embeddingService = new EmbeddingServiceImpl();

        String content = "## 测试手机\n\n摄像头参数：5000万像素";
        List<DocumentChunk> chunks = chunkingService.smartChunk(content, Map.of());

        List<String> chunkTexts = chunks.stream()
            .map(DocumentChunk::getContent)
            .toList();

        var embeddings = documentService.embedChunks(chunkTexts);

        assertThat(embeddings).hasSameSizeAs(chunks);
        assertThat(embeddings.get(0).getVector()).hasSize(1536);
    }
}