package com.javaee.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String userId;
    private String nickname;
    
    @Builder.Default
    private Map<String, Double> categoryWeights = new HashMap<>();
    
    @Builder.Default
    private Map<String, Double> tagPreferences = new HashMap<>();
    
    private Double budgetPreference;
    private Integer timePreference;
    private String crowdPreference;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private LocalDateTime lastUpdated;
    private Integer totalRoutesPlanned;
    private Integer totalFeedbackGiven;
}
