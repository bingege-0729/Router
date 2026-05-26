package com.javaee.backend.entity;



import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class UserProfile {
    private String userId;

    // 静态偏好(用户明确设置)
    private List<String> preferredCategories;    // ["日料", "艺术展"]
    private List<String> avoidedCategories;      // ["川菜", "游乐场"]
    private List<String> preferredTags;          // ["网红打卡", "拍照好看"]
    private List<String> avoidedTags;            // ["排队久", "贵", "人多"]

    // 约束条件
    private Integer maxWalkDistance;              // 最大步行距离(米)
    private Integer maxWaitTime;                  // 最大排队容忍(分钟)
    private Double maxBudget;                     // 预算上限
    private Double priceSensitivity;              // 价格敏感度 0-1

    // 动态偏好(从历史行为学习)
    private Map<String, Double> categoryWeights;  // 类别权重 {"日料":0.8, "展馆":0.9}
    private List<String> recentlyVisited;         // 最近去过
    private List<String> favoritePOIs;            // 收藏的POI

    // 出行习惯
    private String defaultStartLocation;          // 默认起点(家)
    private String defaultTransportMode;          // 默认交通方式
    private boolean needRestBreak;                // 是否需要休息
    private Integer maxPOICount;                  // 单次最多几个点

    private LocalDateTime lastActive;
}