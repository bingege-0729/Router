package com.javaee.backend.entity.enrichment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class POIEnrichment {
    private String poiId;

    // UGC分析结果
    private SentimentScore sentiment;
    private List<String> topHighlights;      // 高频好评词
    private List<String> topComplaints;      // 高频差评词
    private Map<String, Integer> tagFrequency; // 标签频率

    // 实用信息提取
    private Integer avgWaitTime;              // 平均排队时间
    private String bestTimeToVisit;           // "工作日上午"
    private String crowdTip;                  // "周末人很多，建议早去"

    // 用户画像相关
    private Map<String, Double> categoryAffinity; // 类别亲和度
    private Double personalizedScore;         // 个性化得分

    private LocalDateTime analyzedAt;
}