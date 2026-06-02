package com.javaee.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.backend.dto.ParsedIntent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentParserService {

    private final OpenAiChatModel chatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        你是一个智能路线规划助手，专门解析用户的自然语言查询并提取结构化参数。
        
        请分析用户输入的查询，提取以下信息并以JSON格式返回：
        
        {
            "intentType": "route_planning|poi_search|general_query",
            "totalHours": 数字（默认4）,
            "maxBudget": 数字（默认500）,
            "maxWaitTime": 数字（默认30）,
            "categories": ["RESTAURANT", "ATTRACTION", "PARK", "SHOPPING", "MUSEUM", "CAFE"],
            "mustVisit": ["地点名称"],
            "preferences": ["偏好描述"],
            "optimizationGoal": "SHORTEST_TIME|LEAST_WALKING|LOWEST_COST|LEAST_WAITING|HIGHEST_RATED|MOST_PERSONALIZED|BALANCED",
            "additionalContext": "其他重要信息",
            "confidenceScore": 0.0-1.0
        }
        
        规则：
        1. 如果用户提到具体时间（如"半天"、"3小时"、"一天"），转换为小时数
        2. 如果用户提到预算（如"500元内"、"不差钱"），提取或设置合理值
        3. 根据关键词推断类别：公园→PARK, 餐厅→RESTAURANT, 景点→ATTRACTION, 博物馆→MUSEUM, 购物→SHOPPING, 咖啡→CAFE
        4. 如果用户说"不想排队"、"怕挤"，设置LEAST_WAITING优化目标
        5. 如果用户说"省钱"、"预算有限"，设置LOWEST_COST优化目标
        6. 如果用户说"最佳体验"、"最推荐"，设置HIGHEST_RATED优化目标
        7. 只返回JSON，不要其他文字
        """;

    public ParsedIntent parseIntent(String query) {
        log.info("🧠 [LLM] 开始解析用户意图: {}", query);
        
        long startTime = System.currentTimeMillis();
        
        try {
            String userPrompt = String.format("请解析以下用户查询：\n%s", query);
            
            String fullPrompt = SYSTEM_PROMPT + "\n\n" + userPrompt;
            
            String llmResponse = chatLanguageModel.chat(fullPrompt);
            
            log.debug("🤖 [LLM原始响应]: {}", llmResponse);
            
            ParsedIntent intent = extractJSONFromResponse(llmResponse);
            
            if (intent != null) {
                intent.setOriginalQuery(query);
                
                if (intent.getTotalHours() == null || intent.getTotalHours() <= 0) {
                    intent.setTotalHours(4);
                }
                if (intent.getMaxBudget() == null || intent.getMaxBudget() <= 0) {
                    intent.setMaxBudget(500.0);
                }
                if (intent.getOptimizationGoal() == null || intent.getOptimizationGoal().isEmpty()) {
                    intent.setOptimizationGoal("BALANCED");
                }
                if (intent.getConfidenceScore() <= 0) {
                    intent.setConfidenceScore(0.8);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ [LLM] 意图解析完成, 耗时{}ms, 置信度: {}, 类别: {}, 时长: {}h, 预算: {}元",
                        duration, intent.getConfidenceScore(), 
                        intent.getCategories(), intent.getTotalHours(), intent.getMaxBudget());
                
                return intent;
            } else {
                log.warn("⚠️ [LLM] JSON解析失败，使用规则回退");
                return fallbackParse(query);
            }
            
        } catch (Exception e) {
            log.error("❌ [LLM] 意图解析异常: {}", e.getMessage(), e);
            return fallbackParse(query);
        }
    }

    private ParsedIntent extractJSONFromResponse(String response) {
        try {
            Pattern pattern = Pattern.compile("\\{[^{}]*\\}");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                String jsonStr = matcher.group();
                return objectMapper.readValue(jsonStr, ParsedIntent.class);
            }
            
            return objectMapper.readValue(response, ParsedIntent.class);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ JSON解析异常: {}", e.getMessage());
            return null;
        }
    }

    private ParsedIntent fallbackParse(String query) {
        log.info("🔄 使用规则引擎回退解析");
        
        ParsedIntent intent = ParsedIntent.builder()
                .originalQuery(query)
                .intentType("route_planning")
                .totalHours(4)
                .maxBudget(500.0)
                .maxWaitTime(30)
                .optimizationGoal("BALANCED")
                .confidenceScore(0.6)
                .build();

        String lowerQuery = query.toLowerCase();
        
        List<String> categories = new java.util.ArrayList<>();
        if (lowerQuery.contains("公园") || lowerQuery.contains("park")) {
            categories.add("PARK");
        }
        if (lowerQuery.contains("餐厅") || lowerQuery.contains("美食") || lowerQuery.contains("吃饭")) {
            categories.add("RESTAURANT");
        }
        if (lowerQuery.contains("景点") || lowerQuery.contains("旅游") || lowerQuery.contains("玩")) {
            categories.add("ATTRACTION");
        }
        if (lowerQuery.contains("博物馆") || lowerQuery.contains("展览")) {
            categories.add("MUSEUM");
        }
        if (lowerQuery.contains("购物") || lowerQuery.contains("商场")) {
            categories.add("SHOPPING");
        }
        if (lowerQuery.contains("咖啡") || lowerQuery.contains("cafe")) {
            categories.add("CAFE");
        }
        
        if (!categories.isEmpty()) {
            intent.setCategories(categories);
        }

        Matcher timeMatcher = Pattern.compile("(\\d+)\\s*(小时|小时|个?半天|天)").matcher(lowerQuery);
        if (timeMatcher.find()) {
            int hours = Integer.parseInt(timeMatcher.group(1));
            if (timeMatcher.group(2).contains("天")) {
                hours *= (timeMatcher.group(2).contains("半") ? 4 : 8);
            } else if (timeMatcher.group(2).contains("半天")) {
                hours *= 4;
            }
            intent.setTotalHours(Math.min(hours, 12));
        }

        Matcher budgetMatcher = Pattern.compile("(\\d+)\\s*元").matcher(lowerQuery);
        if (budgetMatcher.find()) {
            intent.setMaxBudget(Double.parseDouble(budgetMatcher.group(1)));
        }

        if (lowerQuery.contains("不想排队") || lowerQuery.contains("怕挤")) {
            intent.setOptimizationGoal("LEAST_WAITING");
        } else if (lowerQuery.contains("省钱") || lowerQuery.contains("便宜") || lowerQuery.contains("预算有限")) {
            intent.setOptimizationGoal("LOWEST_COST");
        } else if (lowerQuery.contains("最好") || lowerQuery.contains("推荐") || lowerQuery.contains("精品")) {
            intent.setOptimizationGoal("HIGHEST_RATED");
        } else if (lowerQuery.contains("快") || lowerQuery.contains("省时")) {
            intent.setOptimizationGoal("SHORTEST_TIME");
        }

        return intent;
    }
}
