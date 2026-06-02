package com.javaee.backend.service;

import com.javaee.backend.dto.ParsedIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LLMDegradationService LLM降级服务测试")
class LLMDegradationServiceTest {

    private LLMDegradationService degradationService;

    @BeforeEach
    void setUp() {
        degradationService = new LLMDegradationService();
    }

    @Test
    @DisplayName("解析公园相关查询应提取PARK类别")
    void testParkCategoryExtraction() {
        ParsedIntent intent = degradationService.fallbackParse("我想去公园玩");
        
        assertNotNull(intent);
        assertTrue(intent.getCategories().contains("PARK"),
                "应该包含PARK类别");
    }

    @Test
    @DisplayName("解析博物馆和餐厅应提取多个类别")
    void testMultipleCategories() {
        ParsedIntent intent = degradationService.fallbackParse("想去博物馆和餐厅吃饭");
        
        assertNotNull(intent);
        assertTrue(intent.getCategories().containsAll(List.of("MUSEUM", "RESTAURANT")),
                "应该包含MUSEUM和RESTAURANT类别");
    }

    @Test
    @DisplayName("解析时间信息 - 半天应返回4小时")
    void testHalfDayTime() {
        ParsedIntent intent = degradationService.fallbackParse("半天时间");
        
        assertEquals(4, intent.getTotalHours());
    }

    @Test
    @DisplayName("解析时间信息 - 具体小时数")
    void testSpecificHours() {
        ParsedIntent intent3h = degradationService.fallbackParse("玩3个小时");
        assertEquals(3, intent3h.getTotalHours());
        
        ParsedIntent intent6h = degradationService.fallbackParse("游玩6小时");
        assertEquals(6, intent6h.getTotalHours());
    }

    @Test
    @DisplayName("解析预算信息")
    void testBudgetExtraction() {
        ParsedIntent intent = degradationService.fallbackParse("预算500元");
        
        assertEquals(500.0, intent.getMaxBudget(), 0.01);
    }

    @Test
    @DisplayName("不想排队应推断LEAST_WAITING优化目标")
    void testLeastWaitingGoal() {
        ParsedIntent intent = degradationService.fallbackParse("不想排队");
        
        assertEquals("LEAST_WAITING", intent.getOptimizationGoal());
    }

    @Test
    @DisplayName("省钱应推断LOWEST_COST优化目标")
    void testLowCostGoal() {
        ParsedIntent intent = degradationService.fallbackParse("想省钱，便宜点");
        
        assertEquals("LOWEST_COST", intent.getOptimizationGoal());
    }

    @Test
    @DisplayName("最佳体验应推断HIGHEST_RATED优化目标")
    void testHighestRatedGoal() {
        ParsedIntent intent = degradationService.fallbackParse("想要最佳体验");
        
        assertEquals("HIGHEST_RATED", intent.getOptimizationGoal());
    }

    @Test
    @DisplayName("默认参数应为合理值")
    void testDefaultParameters() {
        ParsedIntent intent = degradationService.fallbackParse("");
        
        assertNotNull(intent);
        assertEquals(4, intent.getTotalHours(), "默认时长应为4小时");
        assertEquals(500.0, intent.getMaxBudget(), 0.01, "默认预算应为500元");
        assertEquals("BALANCED", intent.getOptimizationGoal(), "默认目标应为BALANCED");
        assertTrue(intent.getConfidenceScore() > 0 && intent.getConfidenceScore() <= 1,
                "置信度应在0-1之间");
    }

    @Test
    @DisplayName("生成降级描述不应为空")
    void testFallbackDescriptionGeneration() {
        String description = degradationService.generateFallbackDescription(null, 4);
        
        assertNotNull(description);
        assertFalse(description.isEmpty(), "降级描述不能为空");
        assertTrue(description.contains("4"), "描述应包含时长信息");
    }

    @Test
    @DisplayName("生成POI亮点介绍不应为空")
    void testFallbackHighlightGeneration() {
        Object poi = new Object();
        String highlight = degradationService.generateFallbackHighlight(poi);
        
        assertNotNull(highlight);
        assertFalse(highlight.isEmpty(), "POI亮点不能为空");
    }

    @Test
    @DisplayName("生成个性化说明应包含对应目标的建议")
    void testPersonalizationNoteGeneration() {
        String note1 = degradationService.generateFallbackPersonalizationNote("LEAST_WAITING", null);
        assertTrue(note1.contains("人流量") || note1.contains("排队"),
                "LEAST_WAITING的说明应提到避免排队");
        
        String note2 = degradationService.generateFallbackPersonalizationNote("LOWEST_COST", null);
        assertTrue(note2.contains("性价比") || note2.contains("预算"),
                "LOWEST_COST的说明应提到性价比或预算");
    }

    @Test
    @DisplayName("复杂查询的综合测试")
    void testComplexQuery() {
        String query = "周末带孩子去公园和博物馆玩，不想排队，预算300元内";
        ParsedIntent intent = degradationService.fallbackParse(query);
        
        assertNotNull(intent);
        assertTrue(intent.getCategories().contains("PARK"), "应识别公园");
        assertTrue(intent.getCategories().contains("MUSEUM"), "应识别博物馆");
        assertEquals("LEAST_WAITING", intent.getOptimizationGoal(), "应识别不想排队");
        assertEquals(300.0, intent.getMaxBudget(), 0.01, "应正确解析预算");
    }
}
