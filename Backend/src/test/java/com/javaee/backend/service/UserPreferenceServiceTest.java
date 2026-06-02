package com.javaee.backend.service;

import com.javaee.backend.entity.POI;
import com.javaee.backend.entity.UserProfile;
import com.javaee.backend.enums.POICategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserPreferenceService 用户偏好服务测试")
class UserPreferenceServiceTest {

    private UserPreferenceService userPreferenceService;
    private static final String TEST_USER_ID = "test-user-001";

    @BeforeEach
    void setUp() {
        userPreferenceService = new UserPreferenceService();
    }

    @Test
    @DisplayName("获取不存在的用户应创建默认画像")
    void testGetNonExistentUser() {
        UserProfile profile = userPreferenceService.getUserProfile(TEST_USER_ID);
        
        assertNotNull(profile);
        assertEquals(TEST_USER_ID, profile.getUserId());
        assertEquals(500.0, profile.getBudgetPreference(), 0.01);
        assertEquals(4, profile.getTimePreference());
        assertNotNull(profile.getCategoryWeights());
        assertNotNull(profile.getTagPreferences());
    }

    @Test
    @DisplayName("更新用户画像应成功保存")
    void testUpdateUserProfile() {
        UserProfile updatedProfile = UserProfile.builder()
                .userId(TEST_USER_ID)
                .nickname("测试用户")
                .budgetPreference(1000.0)
                .timePreference(8)
                .crowdPreference("不喜欢拥挤")
                .build();
        
        userPreferenceService.updateUserProfile(TEST_USER_ID, updatedProfile);
        
        UserProfile retrieved = userPreferenceService.getUserProfile(TEST_USER_ID);
        
        assertEquals(1000.0, retrieved.getBudgetPreference(), 0.01);
        assertEquals(8, retrieved.getTimePreference());
        assertEquals("不喜欢拥挤", retrieved.getCrowdPreference());
    }

    @Test
    @DisplayName("计算个性化得分 - 偏好类别应获得更高分")
    void testPersonalizedScoreWithPreferredCategory() {
        POI parkPOI = POI.builder()
                .id("1")
                .name("西湖公园")
                .category(POICategory.PARK)
                .rating(4.5)
                .avgCost(50.0)
                .build();
        
        UserProfile profileWithParkPreference = UserProfile.builder()
                .userId(TEST_USER_ID)
                .categoryWeights(new HashMap<>(Map.of("PARK", 0.5)))
                .build();
        
        double scoreWithPreference = userPreferenceService.calculatePersonalizedScore(
                parkPOI, profileWithParkPreference, "公园");
        
        double scoreWithoutPreference = userPreferenceService.calculatePersonalizedScore(
                parkPOI, new UserProfile(), "公园");
        
        assertTrue(scoreWithPreference > scoreWithoutPreference,
                "偏好的类别应该获得更高的个性化得分");
    }

    @Test
    @DisplayName("计算个性化得分 - 预算敏感度测试")
    void testPersonalizedScoreWithBudgetSensitivity() {
        POI cheapPOI = POI.builder()
                .id("2")
                .name("免费公园")
                .category(POICategory.PARK)
                .avgCost(0.0)
                .build();
        
        POI expensivePOI = POI.builder()
                .id("3")
                .name("高档餐厅")
                .category(POICategory.RESTAURANT)
                .avgCost(500.0)
                .build();
        
        UserProfile budgetConsciousUser = UserProfile.builder()
                .userId(TEST_USER_ID)
                .budgetPreference(300.0)
                .build();
        
        double cheapScore = userPreferenceService.calculatePersonalizedScore(
                cheapPOI, budgetConsciousUser, null);
        
        double expensiveScore = userPreferenceService.calculatePersonalizedScore(
                expensivePOI, budgetConsciousUser, null);
        
        assertTrue(cheapScore > expensiveScore,
                "预算有限的用户应该给便宜的POI更高分");
    }

    @Test
    @DisplayName("计算个性化得分 - 人流敏感度测试")
    void testPersonalizedScoreWithCrowdSensitivity() {
        POI quietPOI = POI.builder()
                .id("4")
                .name("清静公园")
                .currentOccupancy(20)
                .build();
        
        POI crowdedPOI = POI.builder()
                .id("5")
                .name("热门景点")
                .currentOccupancy(80)
                .build();
        
        UserProfile crowdAverseUser = UserProfile.builder()
                .userId(TEST_USER_ID)
                .crowdPreference("不喜欢拥挤")
                .build();
        
        double quietScore = userPreferenceService.calculatePersonalizedScore(
                quietPOI, crowdAverseUser, null);
        
        double crowdedScore = userPreferenceService.calculatePersonalizedScore(
                crowdedPOI, crowdAverseUser, null);
        
        assertTrue(quietScore > crowdedScore,
                "不喜欢拥挤的用户应该给人少的POI更高分");
    }

    @Test
    @DisplayName("个性化排序 - 偏好的POI应该排在前面")
    void testPersonalizePOIRanking() {
        List<POI> pois = Arrays.asList(
                POI.builder().id("6").name("普通景点").category(POICategory.SCENIC_SPOT).build(),
                POI.builder().id("7").name("用户喜欢的公园").category(POICategory.PARK).build(),
                POI.builder().id("8").name("一般餐厅").category(POICategory.RESTAURANT).build()
        );
        
        UserProfile profile = UserProfile.builder()
                .userId(TEST_USER_ID)
                .categoryWeights(Map.of("PARK", 0.8))
                .tagPreferences(Map.of("用户喜欢的公园", 0.9))
                .build();
        
        List<POI> rankedPOIs = userPreferenceService.personalizePOIRanking(pois, profile, "公园");
        
        assertEquals("用户喜欢的公园", rankedPOIs.get(0).getName(),
                "用户喜欢的公园应该排在第一位");
    }

    @Test
    @DisplayName("记录反馈 - 正面反馈应增加偏好权重")
    void testRecordPositiveFeedback() {
        String routeId = "route-001";
        List<String> likedPOIs = Collections.singletonList("西湖");
        
        userPreferenceService.recordRouteFeedback(TEST_USER_ID, routeId, true, likedPOIs, null);
        
        UserProfile profile = userPreferenceService.getUserProfile(TEST_USER_ID);
        
        Double preference = profile.getTagPreferences().get("西湖".toLowerCase());
        
        assertNotNull(preference, "应该记录对西湖的偏好");
        assertTrue(preference > 0.5, "正面反馈后，偏好值应该增加");
    }

    @Test
    @DisplayName("记录反馈 - 负面反馈应减少偏好权重")
    void testRecordNegativeFeedback() {
        String routeId = "route-002";
        List<String> dislikedPOIs = Collections.singletonList("人多的地方");
        
        userPreferenceService.recordRouteFeedback(TEST_USER_ID, routeId, false, null, dislikedPOIs);
        
        UserProfile profile = userPreferenceService.getUserProfile(TEST_USER_ID);
        
        Double preference = profile.getTagPreferences().get("人多的地方".toLowerCase());
        
        assertNotNull(preference, "应该记录对人多地方的负面评价");
        assertTrue(preference < 0.5, "负面反馈后，偏好值应该减少");
    }

    @Test
    @DisplayName("推断优化目标 - 不喜欢拥挤的用户应返回LEAST_WAITING")
    void testInferOptimizationGoalForCrowdAverseUser() {
        UserProfile crowdAverseUser = UserProfile.builder()
                .userId(TEST_USER_ID)
                .crowdPreference("不喜欢拥挤")
                .build();
        
        var goal = userPreferenceService.inferOptimizationGoal(crowdAverseUser);
        
        assertEquals(com.javaee.backend.enums.OptimizationGoal.LEAST_WAITING, goal);
    }

    @Test
    @DisplayName("推断优化目标 - 预算有限用户应返回LOWEST_COST")
    void testInferOptimizationGoalForBudgetUser() {
        UserProfile budgetUser = UserProfile.builder()
                .userId(TEST_USER_ID)
                .budgetPreference(200.0)
                .build();
        
        var goal = userPreferenceService.inferOptimizationGoal(budgetUser);
        
        assertEquals(com.javaee.backend.enums.OptimizationGoal.LOWEST_COST, goal);
    }

    @Test
    @DisplayName("推断优化目标 - 默认用户应返回BALANCED")
    void testInferOptimizationGoalForDefaultUser() {
        var goal = userPreferenceService.inferOptimizationGoal(null);
        
        assertEquals(com.javaee.backend.enums.OptimizationGoal.BALANCED, goal);
    }
}
