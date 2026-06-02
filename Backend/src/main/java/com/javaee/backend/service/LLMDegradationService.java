package com.javaee.backend.service;

import com.javaee.backend.dto.ParsedIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LLMDegradationService {

    private static final Map<String, List<String>> KEYWORD_TO_CATEGORY = new HashMap<>();
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*(小时|小时|半天|天|h|hour)");
    private static final Pattern BUDGET_PATTERN = Pattern.compile("(\\d+)\\s*元");
    private static final Set<String> LOW_CROWD_KEYWORDS = new HashSet<>(Arrays.asList(
            "不想排队", "怕挤", "人少", "清静", "安静"
    ));
    private static final Set<String> BUDGET_CONSCIOUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "省钱", "便宜", "预算有限", "免费", "不贵"
    ));

    static {
        KEYWORD_TO_CATEGORY.put("公园", Arrays.asList("PARK"));
        KEYWORD_TO_CATEGORY.put("餐厅", Arrays.asList("RESTAURANT"));
        KEYWORD_TO_CATEGORY.put("吃饭", Arrays.asList("RESTAURANT"));
        KEYWORD_TO_CATEGORY.put("景点", Arrays.asList("ATTRACTION"));
        KEYWORD_TO_CATEGORY.put("博物馆", Arrays.asList("MUSEUM"));
        KEYWORD_TO_CATEGORY.put("购物", Arrays.asList("SHOPPING"));
        KEYWORD_TO_CATEGORY.put("咖啡", Arrays.asList("CAFE"));
        KEYWORD_TO_CATEGORY.put("景点", Arrays.asList("ATTRACTION"));
        KEYWORD_TO_CATEGORY.put("玩", Arrays.asList("PARK", "ATTRACTION"));
    }

    public ParsedIntent fallbackParse(String query) {
        log.info("🔄 [降级] 使用规则引擎解析意图: {}", query);
        
        long startTime = System.currentTimeMillis();
        
        try {
            ParsedIntent intent = ParsedIntent.builder()
                    .intentType("route_planning")
                    .originalQuery(query)
                    .confidenceScore(0.6)
                    .build();

            intent.setCategories(extractCategories(query));
            intent.setTotalHours(extractTimeHours(query));
            intent.setMaxBudget(extractBudget(query));
            intent.setOptimizationGoal(inferOptimizationGoal(query));

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [降级] 规则引擎解析完成, 耗时{}ms, 类别: {}, 时长: {}h, 目标: {}", 
                    duration, intent.getCategories(), intent.getTotalHours(), intent.getOptimizationGoal());
            
            return intent;
            
        } catch (Exception e) {
            log.error("❌ [降级] 规则引擎也失败: {}", e.getMessage());
            return createDefaultIntent(query);
        }
    }

    private List<String> extractCategories(String query) {
        Set<String> categories = new LinkedHashSet<>();
        
        for (Map.Entry<String, List<String>> entry : KEYWORD_TO_CATEGORY.entrySet()) {
            if (query.contains(entry.getKey())) {
                categories.addAll(entry.getValue());
            }
        }
        
        return categories.isEmpty() ? null : new ArrayList<>(categories);
    }

    private Integer extractTimeHours(String query) {
        Matcher matcher = TIME_PATTERN.matcher(query);
        
        if (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            if ("半天".equals(unit) || "半".equals(unit)) {
                return 4;
            } else if ("天".equals(unit)) {
                return Math.min(number * 6, 12);
            }
            
            return Math.min(Math.max(number, 2), 12);
        }
        
        if (query.contains("半天")) {
            return 4;
        } else if (query.contains("一天") || query.contains("全天")) {
            return 8;
        }
        
        return 4;
    }

    private Double extractBudget(String query) {
        Matcher matcher = BUDGET_PATTERN.matcher(query);
        
        if (matcher.find()) {
            double budget = Double.parseDouble(matcher.group(1));
            
            if (budget < 50) budget *= 10;
            if (budget > 10000) budget /= 10;
            
            return Math.max(budget, 100);
        }
        
        if (BUDGET_CONSCIOUS_KEYWORDS.stream().anyMatch(query::contains)) {
            return 300.0;
        }
        
        return 500.0;
    }

    private String inferOptimizationGoal(String query) {
        if (LOW_CROWD_KEYWORDS.stream().anyMatch(query::contains)) {
            return "LEAST_WAITING";
        }
        
        if (BUDGET_CONSCIOUS_KEYWORDS.stream().anyMatch(query::contains)) {
            return "LOWEST_COST";
        }
        
        if (query.contains("最佳") || query.contains("推荐") || query.contains("最好")) {
            return "HIGHEST_RATED";
        }
        
        if (query.contains("快") || query.contains("省时间")) {
            return "SHORTEST_TIME";
        }
        
        return "BALANCED";
    }

    private ParsedIntent createDefaultIntent(String query) {
        log.warn("⚠️ [降级] 使用默认参数");
        
        return ParsedIntent.builder()
                .intentType("route_planning")
                .originalQuery(query)
                .totalHours(4)
                .maxBudget(500.0)
                .categories(Arrays.asList("PARK", "ATTRACTION"))
                .optimizationGoal("BALANCED")
                .confidenceScore(0.3)
                .build();
    }

    public String generateFallbackDescription(List<?> pois, Integer totalHours) {
        if (pois == null || pois.isEmpty()) {
            return "暂无路线推荐，请调整查询条件后重试。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("为您规划了一条包含%d个地点的路线，预计游玩时间约%d小时。", 
                pois.size(), totalHours != null ? totalHours : 4));
        
        sb.append("\n\n这条路线经过精心挑选，适合您的需求。建议您合理安排时间，享受愉快的旅程！");
        
        return sb.toString();
    }

    public String generateFallbackHighlight(Object poi) {
        try {
            java.lang.reflect.Field nameField = poi.getClass().getDeclaredField("name");
            nameField.setAccessible(true);
            String name = (String) nameField.get(poi);
            
            return String.format("%s 是一个值得游览的好去处，期待您的到来！", name);
        } catch (Exception e) {
            return "这个地点很有特色，值得一游！";
        }
    }

    public String generateFallbackPersonalizationNote(String goal, List<?> selectedPOIs) {
        switch (goal != null ? goal : "BALANCED") {
            case "LEAST_WAITING":
                return "💡 为您选择了人流量较少的时段和地点，避免长时间排队等待。";
            case "LOWEST_COST":
                return "💡 这条路线性价比很高，在控制预算的同时保证体验质量。";
            case "HIGHEST_RATED":
                return "💡 精选了高评分的优质POI，确保您获得最佳的游览体验。";
            case "SHORTEST_TIME":
                return "💡 优化了路线顺序和停留时间，让您在最短时间内游览更多地点。";
            default:
                return "💡 这条路线综合考虑了时间、成本、体验等多个维度，是最平衡的选择。";
        }
    }
}
