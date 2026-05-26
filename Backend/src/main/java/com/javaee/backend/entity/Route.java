package com.javaee.backend.entity;



import com.javaee.backend.enums.OptimizationGoal;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class Route {
    private String routeId;
    private String userId;
    private String queryIntent;              // 原始用户意图

    // 路线信息
    private List<RouteSegment> segments;     // 路线段列表
    private POI startPoint;                  // 起点(可选，默认家)
    private POI endPoint;                    // 终点(可选)

    // 汇总信息
    private Integer totalDuration;           // 总时长(分钟)
    private Integer totalWalkingDistance;    // 总步行距离(米)
    private Double totalCost;                // 总花费
    private Integer totalWaitTime;           // 总排队时间

    // 元信息
    private OptimizationGoal optimizationGoal; // 优化目标
    private Double optimizationScore;        // 路线评分
    private LocalDateTime createdAt;

    // 备选方案
    private List<Route> alternatives;        // 其他可选路线

    // 个性化说明
    private String personalizationReason;    // "根据你不喜欢排队的偏好..."
    private List<String> avoidedPOIs;        // 被排除的POI及原因
}