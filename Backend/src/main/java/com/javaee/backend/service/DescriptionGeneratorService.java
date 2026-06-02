package com.javaee.backend.service;

import com.javaee.backend.entity.POI;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DescriptionGeneratorService {

    private final OpenAiChatModel chatLanguageModel;
    private final OpenAiStreamingChatModel streamingChatModel;

    private static final String ROUTE_DESCRIPTION_PROMPT = """
        你是一个专业的旅行规划助手，擅长为用户生成生动有趣的路线描述。
        
        请根据以下路线信息，生成一段自然流畅的中文描述（200字以内）：
        
        用户需求：{query}
        路线包含 {poiCount} 个地点：
        {poiList}
        
        总时长：{totalHours}小时
        总花费：约{totalCost}元
        
        要求：
        1. 用生动的语言介绍这条路线的特色
        2. 突出2-3个亮点POI
        3. 给出实用的建议（如最佳游览时间、注意事项）
        4. 语气友好、专业，像朋友推荐一样
        5. 使用emoji增加趣味性（适当使用，不要过多）
        
        只返回描述文字，不要其他内容。
        """;

    private static final String POI_HIGHLIGHT_PROMPT = """
        为以下景点/地点生成一段简短的亮点介绍（50字以内）：
        
        名称：{name}
        类别：{category}
        评分：{rating}
        描述：{description}
        
        要求：
        1. 突出最吸引人的特色
        2. 包含实用信息（如推荐理由）
        3. 语言简洁有力
        4. 可以使用1个emoji
        
        只返回介绍文字。
        """;

    public String generateRouteDescription(String query, List<POI> pois, Integer totalHours, Double totalCost) {
        log.info("✍️ [LLM] 开始生成路线描述, POI数量: {}", pois.size());
        
        long startTime = System.currentTimeMillis();
        
        try {
            String poiList = pois.stream()
                    .map(poi -> String.format("- %s (%s, ⭐%.1f分)", 
                            poi.getName(), 
                            poi.getCategory() != null ? poi.getCategory().name() : "未知",
                            poi.getRating() != null ? poi.getRating() : 0.0))
                    .collect(Collectors.joining("\n"));
            
            String prompt = ROUTE_DESCRIPTION_PROMPT
                    .replace("{query}", query != null ? query : "休闲游玩")
                    .replace("{poiCount}", String.valueOf(pois.size()))
                    .replace("{poiList}", poiList)
                    .replace("{totalHours}", totalHours != null ? String.valueOf(totalHours) : "4")
                    .replace("{totalCost}", totalCost != null ? String.format("%.0f", totalCost) : "未知");
            
            String description = chatLanguageModel.chat(prompt);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [LLM] 路线描述生成完成, 耗时{}ms, 长度: {}字符", duration, description.length());
            
            return description.trim();
            
        } catch (Exception e) {
            log.error("❌ [LLM] 路线描述生成失败: {}", e.getMessage());
            return generateFallbackDescription(pois, totalHours);
        }
    }

    public Flux<String> generateRouteDescriptionStream(String query, List<POI> pois, Integer totalHours, Double totalCost) {
        log.info("🌊 [流式] 开始生成路线描述, POI数量: {}", pois.size());
        
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        
        try {
            String poiList = pois.stream()
                    .map(poi -> String.format("- %s (%s, ⭐%.1f分)", 
                            poi.getName(), 
                            poi.getCategory() != null ? poi.getCategory().name() : "未知",
                            poi.getRating() != null ? poi.getRating() : 0.0))
                    .collect(Collectors.joining("\n"));
            
            String prompt = ROUTE_DESCRIPTION_PROMPT
                    .replace("{query}", query != null ? query : "休闲游玩")
                    .replace("{poiCount}", String.valueOf(pois.size()))
                    .replace("{poiList}", poiList)
                    .replace("{totalHours}", totalHours != null ? String.valueOf(totalHours) : "4")
                    .replace("{totalCost}", totalCost != null ? String.format("%.0f", totalCost) : "未知");
            
            AtomicInteger tokenCount = new AtomicInteger(0);
            
            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    tokenCount.incrementAndGet();
                    sink.tryEmitNext(partialResponse);
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    log.info("✅ [流式] 路线描述生成完成, 总token数: {}", tokenCount.get());
                    sink.tryEmitComplete();
                }
                
                @Override
                public void onError(Throwable error) {
                    log.error("❌ [流式] 路线描述生成失败: {}", error.getMessage());
                    sink.tryEmitError(error);
                }
            });
            
        } catch (Exception e) {
            log.error("❌ [流式] 初始化失败: {}", e.getMessage());
            String fallback = generateFallbackDescription(pois, totalHours);
            sink.tryEmitNext(fallback);
            sink.tryEmitComplete();
        }
        
        return sink.asFlux();
    }

    public String generatePOIHighlight(POI poi) {
        if (poi == null) return "";
        
        log.debug("✍️ [LLM] 生成POI亮点: {}", poi.getName());
        
        try {
            String prompt = POI_HIGHLIGHT_PROMPT
                    .replace("{name}", poi.getName() != null ? poi.getName() : "未知")
                    .replace("{category}", poi.getCategory() != null ? poi.getCategory().name() : "未知")
                    .replace("{rating}", poi.getRating() != null ? String.format("%.1f", poi.getRating()) : "暂无")
                    .replace("{description}", poi.getDescription() != null ? poi.getDescription() : "暂无描述");
            
            return chatLanguageModel.chat(prompt).trim();
            
        } catch (Exception e) {
            log.warn("⚠️ [LLM] POI亮点生成失败，使用默认: {}", e.getMessage());
            return generateFallbackHighlight(poi);
        }
    }

    public String generatePersonalizationNote(String optimizationGoal, List<POI> selectedPOIs) {
        StringBuilder note = new StringBuilder("💡 ");
        
        switch (optimizationGoal) {
            case "SHORTEST_TIME":
                note.append("为您优化了时间安排，精选了最高效的游览路线");
                break;
            case "LEAST_WALKING":
                note.append("考虑到您希望少走路，我们选择了距离较近的景点组合");
                break;
            case "LOWEST_COST":
                note.append("根据您的预算要求，为您挑选了高性价比的地点");
                break;
            case "LEAST_WAITING":
                note.append("为了避免长时间排队，选择了当前人流量较少的时段和地点");
                break;
            case "HIGHEST_RATED":
                note.append("为您精选了评分最高的热门地点，保证最佳体验");
                break;
            default:
                note.append("综合平衡了时间、费用和体验质量，为您规划了最优路线");
        }
        
        if (selectedPOIs != null && !selectedPOIs.isEmpty()) {
            note.append("\n📍 特别推荐：").append(selectedPOIs.get(0).getName());
        }
        
        return note.toString();
    }

    private String generateFallbackDescription(List<POI> pois, Integer totalHours) {
        if (pois == null || pois.isEmpty()) {
            return "为您精心规划的路线，祝您旅途愉快！";
        }
        
        StringBuilder desc = new StringBuilder();
        desc.append("🎉 这条路线涵盖了 ").append(pois.size()).append(" 个精选地点");
        
        if (totalHours != null) {
            desc.append("，预计耗时约 ").append(totalHours).append(" 小时");
        }
        
        desc.append("。首站推荐「").append(pois.get(0).getName()).append("」");
        
        if (pois.size() > 1) {
            desc.append("，最后在「").append(pois.get(pois.size()-1).getName()).append("」结束美好的一天");
        }
        
        desc.append("！✨");
        
        return desc.toString();
    }

    private String generateFallbackHighlight(POI poi) {
        if (poi == null) return "";
        
        StringBuilder highlight = new StringBuilder();
        
        highlight.append(poi.getName() != null ? poi.getName() : "这个地点");
        
        if (poi.getRating() != null && poi.getRating() >= 4.5) {
            highlight.append(" ⭐ 高评分推荐！");
        } else if (poi.getDescription() != null && !poi.getDescription().isEmpty()) {
            String shortDesc = poi.getDescription().length() > 30 
                    ? poi.getDescription().substring(0, 30) + "..." 
                    : poi.getDescription();
            highlight.append(" - ").append(shortDesc);
        }
        
        return highlight.toString();
    }
}
