package com.javaee.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RouteRequest {
    private String query;
    private String userId;
    private BigDecimal startLat;
    private BigDecimal startLng;
    private Integer totalHours;
    private List<String> mustVisit;
    private List<String> categories;
    private Double maxBudget;
    private Integer maxWaitTime;
    private String optimizationGoal;
}
