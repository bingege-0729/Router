package com.javaee.backend.entity;

import com.javaee.backend.enums.OptimizationGoal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    private String id;
    private String query;
    private String description;
    
    // 路线信息
    private List<POI> pois;                  // POI列表
    private List<RouteSegment> segments;     // 路线段列表
    
    // 汇总信息
    private Integer totalDuration;           // 总时长(分钟)
    private Double totalCost;                // 总花费
    private Integer totalDistance;           // 总距离(米)
    private String summary;                  // 概述文案

    // 元信息
    private LocalDateTime createdAt;
}
