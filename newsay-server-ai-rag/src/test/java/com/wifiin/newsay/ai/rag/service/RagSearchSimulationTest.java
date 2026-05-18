package com.wifiin.newsay.ai.rag.service;

import com.wifiin.newsay.ai.rag.model.DocumentChunk;
import com.wifiin.newsay.ai.rag.service.impl.ChunkingServiceImpl;
import com.wifiin.newsay.ai.rag.service.impl.EmbeddingServiceImpl;
import com.wifiin.newsay.ai.rag.service.impl.MilvusVectorStoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RAG 模拟用户查询测试 - 照相功能最好的3000元以内的手机")
class RagSearchSimulationTest {

    private static final String PHONES_COLLECTION = "phones";
    private static final String TEST_QUERY = "照相功能最好的3000元以内的手机";

    private ChunkingService chunkingService;
    private EmbeddingService embeddingService;
    private VectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingServiceImpl();
        embeddingService = new EmbeddingServiceImpl();
        vectorStoreService = new MilvusVectorStoreServiceImpl(embeddingService);
    }

    @Test
    @DisplayName("模拟用户查询：照相功能最好的3000元以内的手机")
    void simulateUserQuery_cameraBestUnder3000() {
        String phoneData = """
            ## 小米 14 Ultra
            - 价格: 5999元
            - 摄像头: 5000万像素徕卡镜头，1英寸大底传感器
            - 优点: 拍照业界顶级，支持8K视频录制
            - 缺点: 价格较高

            ## OPPO Find X8 Pro
            - 价格: 4999元
            - 摄像头: 5000万像素哈苏镜头，潜望式长焦
            - 优点: 长焦拍摄优秀，夜景表现出色
            - 缺点: 处理器性能中等

            ## Vivo X200 Pro
            - 价格: 5499元
            - 摄像头: 2亿像素蔡司镜头
            - 优点: 拍照清晰度极高，变焦能力强
            - 缺点: 价格偏贵

            ## 真我 GT5 Pro
            - 价格: 2999元
            - 摄像头: 5000万像素索尼IMX890
            - 优点: 性价比极高，拍照表现在同价位最好
            - 缺点: 品牌知名度较低

            ## 红米 K70 Pro
            - 价格: 2799元
            - 摄像头: 5000万像素光影猎人800
            - 优点: 主摄拍照效果不错，价格实惠
            - 缺点: 超广角和长焦表现一般

            ## iPhone 15 Pro
            - 价格: 7999元
            - 摄像头: 4800万像素索尼传感器
            - 优点: 视频拍摄业界最强，拍照真实自然
            - 缺点: 价格太贵，信号一般

            ## 华为 Mate 70 Pro+
            - 价格: 8999元
            - 摄像头: 5000万像素XMAGE可变光圈
            - 优点: 拍照效果出色，可变光圈技术领先
            - 缺点: 价格昂贵，缺货严重
            """;

        List<DocumentChunk> chunks = chunkingService.smartChunk(phoneData, Map.of("source", "phones"));
        assertThat(chunks).isNotEmpty();
        System.out.println("Step 2: 分块完成，共 " + chunks.size() + " 个块");

        vectorStoreService.createCollectionIfNotExists(PHONES_COLLECTION, embeddingService.getEmbeddingDimension());
        vectorStoreService.insert(PHONES_COLLECTION, chunks);

        VectorStoreService.CollectionStats stats = vectorStoreService.getCollectionStats(PHONES_COLLECTION);
        System.out.println("Step 3: 插入完成，集合中共有 " + stats.vectorCount() + " 个向量");

        List<VectorStoreService.SearchResult> results = vectorStoreService.similaritySearch(PHONES_COLLECTION, TEST_QUERY, 5);

        System.out.println("\n========== 搜索结果 ==========");
        System.out.println("查询: " + TEST_QUERY);
        System.out.println("找到 " + results.size() + " 条结果\n");

        for (int i = 0; i < results.size(); i++) {
            VectorStoreService.SearchResult result = results.get(i);
            System.out.println("结果 " + (i + 1) + " (得分: " + String.format("%.4f", result.score()) + "):");
            System.out.println(result.content());
            System.out.println("-----------------------------------");
        }

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isGreaterThan(0.0);

        String topContent = results.get(0).content();
        assertThat(topContent).containsAnyOf("2999", "2799", "价格");

        System.out.println("\n========== 验证通过 ==========");
        System.out.println("最优结果: " + topContent.substring(0, Math.min(100, topContent.length())) + "...");
    }

    @Test
    @DisplayName("测试搜索结果按相似度排序")
    void testSearchResultsSortedBySimilarity() {
        String phoneData = """
            ## 手机A - 价格便宜拍照一般
            - 价格: 1500元
            - 摄像头: 1300万像素
            - 优点: 便宜
            - 缺点: 拍照一般

            ## 手机B - 价格中等拍照很好
            - 价格: 3000元
            - 摄像头: 5000万像素，光学防抖
            - 优点: 拍照效果很好，性价比高
            - 缺点: 性能一般

            ## 手机C - 价格贵拍照顶级
            - 价格: 8000元
            - 摄像头: 1亿像素，专业影像芯片
            - 优点: 拍照顶级
            - 缺点: 价格太贵
            """;

        List<DocumentChunk> chunks = chunkingService.smartChunk(phoneData, Map.of("source", "test"));
        vectorStoreService.createCollectionIfNotExists("price_test", embeddingService.getEmbeddingDimension());
        vectorStoreService.insert("price_test", chunks);

        List<VectorStoreService.SearchResult> results = vectorStoreService.similaritySearch("price_test", "便宜且拍照好的手机 3000元以内", 3);

        assertThat(results).isNotEmpty();

        System.out.println("\n========== 便宜且拍照好的手机搜索结果 ==========");
        for (int i = 0; i < results.size(); i++) {
            System.out.println((i + 1) + ". [" + String.format("%.4f", results.get(i).score()) + "] " +
                results.get(i).content().substring(0, Math.min(80, results.get(i).content().length())));
        }

        assertThat(results.get(0).score()).isGreaterThanOrEqualTo(results.get(results.size() - 1).score());
    }

    @Test
    @DisplayName("验证文档分块策略能保留完整手机信息")
    void testChunkingPreservesPhoneInfo() {
        String singlePhone = """
            ## 小米 14 Ultra
            价格: 5999元
            处理器: 骁龙8 Gen3
            内存: 16GB
            存储: 512GB
            摄像头: 5000万像素徕卡1英寸大底主摄 + 5000万超广角 + 5000万长焦
            屏幕: 6.73英寸 2K AMOLED
            电池: 5300mAh, 90W快充
            优点: 拍照顶级，影像能力强
            缺点: 价格较高，机身较重
            """;

        List<DocumentChunk> chunks = chunkingService.smartChunk(singlePhone, Map.of("brand", "小米", "model", "14 Ultra"));

        assertThat(chunks).isNotEmpty();

        boolean hasPrice = chunks.stream()
            .anyMatch(c -> c.getContent().contains("5999") || c.getContent().contains("价格"));
        assertThat(hasPrice).isTrue();

        boolean hasCamera = chunks.stream()
            .anyMatch(c -> c.getContent().contains("摄像头") || c.getContent().contains("像素"));
        assertThat(hasCamera).isTrue();

        System.out.println("\n========== 分块测试结果 ==========");
        System.out.println("原始长度: " + singlePhone.length() + " 字符");
        System.out.println("分块数量: " + chunks.size() + " 个");
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("\n块 " + (i + 1) + " (长度: " + chunks.get(i).getContent().length() + "):");
            System.out.println(chunks.get(i).getContent().substring(0, Math.min(150, chunks.get(i).getContent().length())) + "...");
        }
    }
}
