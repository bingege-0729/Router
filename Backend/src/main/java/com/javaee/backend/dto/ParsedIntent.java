package com.javaee.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedIntent {
    private String originalQuery;
    
    private String intentType;
    
    private BigDecimal startLat;
    private BigDecimal startLng;
    
    @Builder.Default
    private Integer totalHours = 4;
    
    @Builder.Default
    private Double maxBudget = 500.0;
    
    @Builder.Default
    private Integer maxWaitTime = 30;
    
    private List<String> categories;
    private List<String> mustVisit;
    private List<String> preferences;
    
    private String optimizationGoal;
    
    private String additionalContext;
    
    private double confidenceScore;
}
