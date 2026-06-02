package com.javaee.backend.service;

import com.javaee.backend.entity.POI;
import com.javaee.backend.entity.UserProfile;
import com.javaee.backend.enums.OptimizationGoal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserPreferenceService {

    private final Map<String, UserProfile> userProfiles = new ConcurrentHashMap<>();

    public UserProfile getUserProfile(String userId) {
        return userProfiles.computeIfAbsent(userId, this::createDefaultProfile);
    }

    public void updateUserProfile(String userId, UserProfile profile) {
        profile.setLastUpdated(java.time.LocalDateTime.now());
        userProfiles.put(userId, profile);
        log.info("👤 [用户画像] 更新用户 {} 的画像", userId);
    }

    public double calculatePersonalizedScore(POI poi, UserProfile profile, String query) {
        double baseScore = 1.0;

        if (poi.getCategory() != null && profile.getCategoryWeights() != null) {
            String category = poi.getCategory().name();
            Double weight = profile.getCategoryWeights().get(category);
            if (weight != null) {
                baseScore *= (1.0 + weight);
            }
        }

        if (poi.getTags() != null && !poi.getTags().isEmpty() && profile.getTagPreferences() != null) {
            for (String tag : poi.getTags()) {
                Double tagPref = profile.getTagPreferences().get(tag.toLowerCase());
                if (tagPref != null && tagPref > 0.5) {
                    baseScore *= (1.0 + tagPref * 0.3);
                }
            }
        }

        if (profile.getBudgetPreference() != null && poi.getAvgCost() != null) {
            double budgetRatio = poi.getAvgCost() / profile.getBudgetPreference();
            if (budgetRatio < 0.8) {
                baseScore *= 1.2;
            } else if (budgetRatio > 1.5) {
                baseScore *= 0.7;
            }
        }

        if ("不喜欢拥挤".equals(profile.getCrowdPreference()) && poi.getCurrentOccupancy() != null) {
            if (poi.getCurrentOccupancy() < 30) {
                baseScore *= 1.3;
            } else if (poi.getCurrentOccupancy() > 70) {
                baseScore *= 0.6;
            }
        }

        if (query != null && poi.getName() != null && poi.getName().toLowerCase().contains(query.toLowerCase())) {
            baseScore *= 1.5;
        }

        return Math.max(0.1, Math.min(3.0, baseScore));
    }

    public List<POI> personalizePOIRanking(List<POI> pois, UserProfile profile, String query) {
        if (profile == null || pois == null || pois.isEmpty()) {
            return pois;
        }

        log.info("🎯 [个性化] 为{}个POI应用个性化排序", pois.size());

        Map<POI, Double> scoredPOIs = new LinkedHashMap<>();
        for (POI poi : pois) {
            double personalizedScore = calculatePersonalizedScore(poi, profile, query);
            scoredPOIs.put(poi, personalizedScore);
        }

        return scoredPOIs.entrySet().stream()
                .sorted(Map.Entry.<POI, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    public void recordRouteFeedback(String userId, String routeId, boolean adopted, 
                                    List<String> likedPOIs, List<String> dislikedPOIs) {
        UserProfile profile = getUserProfile(userId);

        if (likedPOIs != null) {
            for (String poiName : likedPOIs) {
                updatePreferenceFromFeedback(profile, poiName, true);
            }
        }

        if (dislikedPOIs != null) {
            for (String poiName : dislikedPOIs) {
                updatePreferenceFromFeedback(profile, poiName, false);
            }
        }

        profile.setTotalRoutesPlanned(
                (profile.getTotalRoutesPlanned() != null ? profile.getTotalRoutesPlanned() : 0) + 1
        );
        
        if (adopted) {
            profile.setTotalFeedbackGiven(
                    (profile.getTotalFeedbackGiven() != null ? profile.getTotalFeedbackGiven() : 0) + 1
            );
        }

        updateUserProfile(userId, profile);

        log.info("📊 [反馈记录] 用户{}, 路线{}, 采纳:{}, 喜欢:{}, 不喜欢:{}", 
                userId, routeId, adopted, likedPOIs, dislikedPOIs);
    }

    private void updatePreferenceFromFeedback(UserProfile profile, String poiName, boolean positive) {
        double delta = positive ? 0.1 : -0.15;

        if (profile.getCategoryWeights() == null) {
            profile.setCategoryWeights(new HashMap<>());
        }
        
        if (profile.getTagPreferences() == null) {
            profile.setTagPreferences(new HashMap<>());
        }

        String normalizedPoiName = poiName.toLowerCase();
        Double currentTagPref = profile.getTagPreferences().getOrDefault(normalizedPoiName, 0.5);
        double newTagPref = Math.max(0.0, Math.min(1.0, currentTagPref + delta));
        profile.getTagPreferences().put(normalizedPoiName, newTagPref);
    }

    private UserProfile createDefaultProfile(String userId) {
        log.info("🆕 [用户画像] 创建默认画像: {}", userId);
        
        return UserProfile.builder()
                .userId(userId)
                .categoryWeights(new HashMap<>())
                .tagPreferences(new HashMap<>())
                .budgetPreference(500.0)
                .timePreference(4)
                .crowdPreference("一般")
                .totalRoutesPlanned(0)
                .totalFeedbackGiven(0)
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
    }

    public OptimizationGoal inferOptimizationGoal(UserProfile profile) {
        if (profile == null) {
            return OptimizationGoal.BALANCED;
        }

        if ("不喜欢拥挤".equals(profile.getCrowdPreference())) {
            return OptimizationGoal.LEAST_WAITING;
        }

        if (profile.getBudgetPreference() != null && profile.getBudgetPreference() < 300) {
            return OptimizationGoal.LOWEST_COST;
        }

        if (profile.getTimePreference() != null && profile.getTimePreference() <= 2) {
            return OptimizationGoal.SHORTEST_TIME;
        }

        return OptimizationGoal.BALANCED;
    }
}
