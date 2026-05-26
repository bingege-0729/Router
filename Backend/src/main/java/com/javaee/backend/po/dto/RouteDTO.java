package com.javaee.backend.po.dto;


import com.javaee.backend.entity.geo.Location;
import com.javaee.backend.enums.OptimizationGoal;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteDTO{
    // 用户输入
    private String query;                    // "周末上午去朝阳公园，然后吃日料"
    private String userId;

    // 约束条件(可选，可从用户画像获取)
    private Location startPoint;             // 起点(默认家)
    private LocalDateTime startTime;         // 出发时间
    private Integer totalHours;              // 总共几小时(默认6)

    // 必去POI(用户指定)
    private List<String> mustVisitPOIs;      // ["朝阳公园"]

    // 可选偏好
    private List<String> preferredCategories;
    private List<String> avoidedTags;
    private Double maxBudget;
    private Integer maxWaitTime;

    // 优化目标
    private OptimizationGoal goal;            // 默认BALANCED

    // 是否返回备选方案
    private Boolean needAlternatives;
}