package com.javaee.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.javaee.backend.dto.ParsedIntent;
import com.javaee.backend.entity.POI;
import com.javaee.backend.entity.Route;
import com.javaee.backend.entity.RouteSegment;
import com.javaee.backend.entity.UserProfile;
import com.javaee.backend.enums.OptimizationGoal;
import com.javaee.backend.enums.TransportMode;
import com.javaee.backend.mapper.POIMapper;
import com.javaee.backend.po.vo.RouteVO;
import com.utils.DistanceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePlannerService {
    @Autowired
    private POIMapper poiMapper;
    @Autowired
    private POIService poiService;
    private final Cache<Object, Object> routeResultCache;
    
    @Autowired
    private IntentParserService intentParserService;
    @Autowired
    private DescriptionGeneratorService descriptionGeneratorService;
    @Autowired
    private UserPreferenceService userPreferenceService;


    public RouteVO planRoute(String query, String userId, BigDecimal startLat, BigDecimal startLng,
                             Integer totalHours, List<String> mustVisit,
                             List<String> categories, Double maxBudget,
                             Integer maxWaitTime, OptimizationGoal optimizationGoal) {

        log.info("🚀 开始智能路线规划: user={}, goal={}, query={}, hours={}", 
                userId, optimizationGoal, query, totalHours);

        UserProfile userProfile = null;
        if (userId != null && !userId.isEmpty()) {
            userProfile = userPreferenceService.getUserProfile(userId);
            log.info("👤 [个性化] 加载用户画像: {}", userProfile.getNickname());
            
            if (optimizationGoal == null) {
                optimizationGoal = userPreferenceService.inferOptimizationGoal(userProfile);
                log.info("🎯 [个性化] 推断优化目标: {}", optimizationGoal);
            }
        }

        // 生成缓存key（包含用户ID以实现个性化缓存）
        String cacheKey = generateCacheKey(query + (userId != null ? ":" + userId : ""), 
                startLat, startLng, totalHours, mustVisit, categories, maxBudget, maxWaitTime, optimizationGoal);

        // 检查缓存
        Object cachedObj = routeResultCache.getIfPresent(cacheKey);
        if (cachedObj instanceof RouteVO) {
            RouteVO cachedResult = (RouteVO) cachedObj;
            log.info("✅ [缓存命中] 路线规划结果已缓存，直接返回");
            log.info("📊 缓存统计: 命中次数={}", routeResultCache.stats().hitCount());
            return cachedResult;
        }

        long startTime = System.currentTimeMillis();

        // 0. 使用LLM智能解析用户意图
        ParsedIntent parsedIntent = null;
        if (query != null && !query.isEmpty()) {
            try {
                parsedIntent = intentParserService.parseIntent(query);
                
                // 如果LLM成功解析，使用解析后的参数（用户显式传入的参数优先）
                if (parsedIntent != null && parsedIntent.getConfidenceScore() > 0.7) {
                    log.info("🧠 [LLM] 使用AI解析的意图参数");
                    
                    if (totalHours == null) {
                        totalHours = parsedIntent.getTotalHours();
                    }
                    if (maxBudget == null) {
                        maxBudget = parsedIntent.getMaxBudget();
                    }
                    if (maxWaitTime == null) {
                        maxWaitTime = parsedIntent.getMaxWaitTime();
                    }
                    if ((categories == null || categories.isEmpty()) && parsedIntent.getCategories() != null) {
                        categories = parsedIntent.getCategories();
                    }
                    if ((mustVisit == null || mustVisit.isEmpty()) && parsedIntent.getMustVisit() != null) {
                        mustVisit = parsedIntent.getMustVisit();
                    }
                    if (optimizationGoal == null && parsedIntent.getOptimizationGoal() != null) {
                        try {
                            optimizationGoal = OptimizationGoal.valueOf(parsedIntent.getOptimizationGoal());
                        } catch (IllegalArgumentException e) {
                            log.warn("⚠️ [LLM] 无效的优化目标: {}, 使用默认", parsedIntent.getOptimizationGoal());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [LLM] 意图解析失败，使用原始参数: {}", e.getMessage());
            }
        }

        // 1. 搜索候选POI（使用智能选择策略）
        List<POI> candidates = searchCandidatesWithScoring(query, mustVisit, categories);
        log.info("📍 找到 {} 个候选POI", candidates.size());

        if (candidates.isEmpty()) {
            RouteVO emptyResult = createEmptyResult(query, "未找到符合条件的POI，请尝试其他关键词");
            routeResultCache.put(cacheKey, emptyResult);
            return emptyResult;
        }

        // 2. 基于约束过滤和排序POI
        List<POI> selectedPOIs = filterAndSelectPOIs(candidates, totalHours, maxBudget, maxWaitTime);

        // 2.5 应用个性化推荐（如果有用户画像）
        if (userProfile != null && !selectedPOIs.isEmpty()) {
            log.info("🎯 [个性化] 应用用户偏好重新排序POI");
            selectedPOIs = userPreferenceService.personalizePOIRanking(selectedPOIs, userProfile, query);
        }

        if (selectedPOIs.isEmpty()) {
            RouteVO emptyResult = createEmptyResult(query, "无法在给定约束内完成路线规划");
            routeResultCache.put(cacheKey, emptyResult);
            return emptyResult;
        }

        // 3. 使用OR-Tools求解最优路径（TSP问题）
        List<POI> optimizedRoute = solveTSPWithORTools(selectedPOIs, startLat, startLng, optimizationGoal);

        // 4. 构建详细时间轴
        List<RouteSegment> segments = buildDetailedTimeline(optimizedRoute, startLat, startLng, totalHours);

        // 5. 生成主路线
        RouteVO mainRoute = buildRouteVO(optimizedRoute, segments, query, "主推荐方案", totalHours);

        // 6. 生成备选方案
        List<RouteVO> alternatives = generateSmartAlternatives(candidates, startLat, startLng,
                totalHours, maxBudget, optimizationGoal);

        long endTime = System.currentTimeMillis();
        log.info("✅ 路线规划完成，耗时: {} ms", endTime - startTime);

        // 7. 使用LLM生成个性化描述
        String llmDescription = null;
        String llmPersonalizationNote = null;
        
        if (query != null && !selectedPOIs.isEmpty()) {
            try {
                // 生成路线整体描述
                llmDescription = descriptionGeneratorService.generateRouteDescription(
                        query, 
                        optimizedRoute, 
                        totalHours, 
                        mainRoute.getMainRoute() != null ? mainRoute.getMainRoute().getTotalCost() : null
                );
                
                log.info("✍️ [LLM] 已生成智能路线描述");
            } catch (Exception e) {
                log.warn("⚠️ [LLM] 路线描述生成失败: {}", e.getMessage());
            }
            
            try {
                // 为每个POI生成亮点介绍
                for (POI poi : optimizedRoute) {
                    if (poi.getDescription() == null || poi.getDescription().isEmpty()) {
                        String highlight = descriptionGeneratorService.generatePOIHighlight(poi);
                        poi.setDescription(highlight);
                    }
                }
                
                log.info("✍️ [LLM] 已为{}个POI生成亮点介绍", optimizedRoute.size());
            } catch (Exception e) {
                log.warn("⚠️ [LLM] POI亮点生成失败: {}", e.getMessage());
            }
            
            try {
                // 生成个性化推荐说明
                llmPersonalizationNote = descriptionGeneratorService.generatePersonalizationNote(
                        optimizationGoal != null ? optimizationGoal.name() : "BALANCED",
                        selectedPOIs
                );
            } catch (Exception e) {
                log.warn("⚠️ [LLM] 个性化说明生成失败: {}", e.getMessage());
            }
        }

        RouteVO result = RouteVO.builder()
                .requestId(UUID.randomUUID().toString())
                .success(true)
                .message(llmDescription != null ? llmDescription : "路线规划成功")
                .mainRoute(mainRoute.getMainRoute())
                .alternatives(alternatives.stream().map(RouteVO::getMainRoute).collect(Collectors.toList()))
                .personalizationNote(llmPersonalizationNote != null ? llmPersonalizationNote : generatePersonalizationNote(optimizationGoal, selectedPOIs))
                .build();

        // 存入缓存
        routeResultCache.put(cacheKey, result);
        log.info("💾 [缓存存储] 路线规划结果已缓存，key={}", cacheKey.substring(0, Math.min(20, cacheKey.length())) + "...");

        return result;
    }

    /**
     * 智能POI搜索与评分
     */
    private List<POI> searchCandidatesWithScoring(String query, List<String> mustVisit, List<String> categories) {
        Map<POI, Double> scoredPOIs = new LinkedHashMap<>();

        // 必去POI（最高优先级，权重 1.0）
        if (mustVisit != null && !mustVisit.isEmpty()) {
            for (String name : mustVisit) {
                POI poi = poiService.getByName(name);
                if (poi != null) {
                    scoredPOIs.put(poi, 1.0);
                    log.info("✓ 必去POI: {} (评分: 1.0)", name);
                }
            }
        }

        // 按类别搜索（综合评分）
        if (categories != null && !categories.isEmpty()) {
            for (String category : categories) {
                List<POI> byCategory = poiService.getByCategory(category);
                for (POI poi : byCategory) {
                    if (!scoredPOIs.containsKey(poi)) {
                        double score = calculatePOIScore(poi);
                        scoredPOIs.put(poi, score);
                    }
                }
            }
        }

        // 关键词搜索（如果上面都没结果）
        if (scoredPOIs.isEmpty() && query != null && !query.isEmpty()) {
            List<POI> allPOIs = poiService.getAll();
            for (POI poi : allPOIs) {
                double relevanceScore = calculateRelevance(poi, query);
                if (relevanceScore > 0.3) {
                    double score = calculatePOIScore(poi) * relevanceScore;
                    scoredPOIs.put(poi, score);
                }
            }
        }

        // 如果还没有，返回热门POI
        if (scoredPOIs.isEmpty()) {
            List<POI> hotPOIs = poiService.getAll();
            hotPOIs.stream()
                    .limit(15)
                    .forEach(poi -> scoredPOIs.put(poi, calculatePOIScore(poi)));
        }

        return scoredPOIs.entrySet().stream()
                .sorted(Map.Entry.<POI, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 计算POI综合评分（0-1）
     */
    private double calculatePOIScore(POI poi) {
        double score = 0.0;

        // 评分权重 (0-30%)
        if (poi.getRating() != null) {
            score += (poi.getRating() / 5.0) * 0.30;
        }

        // 热度权重 (0-25%)
        if (poi.getReviewCount() != null && poi.getReviewCount() > 0) {
            int reviewCount = Math.min(poi.getReviewCount(), 1000); // 上限1000
            double popularity = Math.log10(reviewCount + 1) / Math.log10(1001);
            score += popularity * 0.25;
        }

        // 当前状态权重 (0-20%)
        if (poi.getCurrentOccupancy() != null) {
            int occupancy = poi.getCurrentOccupancy();
            if (occupancy < 30) {
                score += 0.20; // 不拥挤，加分
            } else if (occupancy > 80) {
                score -= 0.10; // 太拥挤，减分
            }
        }

        // 排队时间权重 (0-15%)
        if (poi.getCurrentWaitTime() != null) {
            int waitTime = poi.getCurrentWaitTime();
            if (waitTime < 10) {
                score += 0.15; // 排队短，加分
            } else if (waitTime > 30) {
                score -= 0.05; // 排队长，减分
            }
        }

        // 标签多样性奖励 (0-10%)
        if (poi.getTags() != null && !poi.getTags().isEmpty()) {
            score += Math.min(poi.getTags().size() * 0.02, 0.10);
        }

        return Math.max(0, Math.min(1, score));
    }

    /**
     * 计算关键词相关性
     */
    private double calculateRelevance(POI poi, String query) {
        double relevance = 0.0;
        String lowerQuery = query.toLowerCase();

        if (poi.getName() != null && poi.getName().toLowerCase().contains(lowerQuery)) {
            relevance += 0.5;
        }
        if (poi.getCategory() != null && poi.getCategory().name().toLowerCase().contains(lowerQuery)) {
            relevance += 0.3;
        }
        if (poi.getTags() != null) {
            boolean tagMatch = poi.getTags().stream()
                    .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery));
            if (tagMatch) relevance += 0.2;
        }

        return relevance;
    }

    /**
     * 基于约束过滤和选择POI
     */
    private List<POI> filterAndSelectPOIs(List<POI> candidates, Integer totalHours,
                                          Double maxBudget, Integer maxWaitTime) {
        int maxPOIs = calculateMaxPOIs(totalHours);
        log.info("📊 最大可容纳POI数量: {} (总时长: {}小时)", maxPOIs, totalHours);

        List<POI> filtered = new ArrayList<>();
        double currentCost = 0;
        int totalTime = 0;

        // 按推荐时长升序排列，优先选择耗时短的POI（贪心算法）
        List<POI> sortedCandidates = candidates.stream()
                .sorted(Comparator.comparingInt(p -> 
                        p.getRecommendedDuration() != null ? p.getRecommendedDuration() : 90))
                .collect(Collectors.toList());

        for (POI poi : sortedCandidates) {
            if (filtered.size() >= maxPOIs) break;

            // 预算检查
            if (maxBudget != null && poi.getAvgCost() != null) {
                if (currentCost + poi.getAvgCost() > maxBudget) {
                    log.debug("💰 超过预算: {} (累计: {}/{})",
                            poi.getName(), currentCost + poi.getAvgCost(), maxBudget);
                    continue;
                }
            }

            // 排队时间检查
            if (maxWaitTime != null && poi.getCurrentWaitTime() != null) {
                if (poi.getCurrentWaitTime() > maxWaitTime) {
                    log.debug("⏰ 排队超时: {} ({}分钟)",
                            poi.getName(), poi.getCurrentWaitTime());
                    continue;
                }
            }

            // 时间检查（放宽到95%，留5%缓冲）
            int stayDuration = poi.getRecommendedDuration() != null ? poi.getRecommendedDuration() : 90;
            if (totalTime + stayDuration > totalHours * 60 * 0.95) {
                log.debug("⏱️ 时间不足: {} (需要{}min, 剩余{}/{}min)",
                        poi.getName(), stayDuration, (int)(totalHours * 60 * 0.95 - totalTime), totalHours * 60);
                continue; // 改为continue，继续尝试其他短时间POI
            }

            filtered.add(poi);
            if (poi.getAvgCost() != null) currentCost += poi.getAvgCost();
            totalTime += stayDuration;
            
            log.info("✅ 选择POI: {} (时长:{}min, 累计:{}/{}min)", 
                    poi.getName(), stayDuration, totalTime, totalHours * 60);
        }

        // 兜底逻辑：如果没选中任何POI但候选不为空，强制选择第一个最短的
        if (filtered.isEmpty() && !sortedCandidates.isEmpty()) {
            POI fallbackPOI = sortedCandidates.get(0);
            filtered.add(fallbackPOI);
            log.warn("⚠️ [兜底] 强制选择POI: {} (原约束过严)", fallbackPOI.getName());
        }

        log.info("🎯 最终选中 {} 个POI (预算: {}/{}, 时间: {}/{}min)",
                filtered.size(), currentCost, maxBudget != null ? maxBudget : "∞",
                totalTime, totalHours * 60);

        return filtered;
    }

    /**
     * 计算最大POI数量
     */
    private int calculateMaxPOIs(Integer totalHours) {
        int avgStayTime = 90; // 平均停留90分钟
        int travelBuffer = 30; // 交通缓冲
        int availableMinutes = totalHours * 60 - travelBuffer;
        return Math.max(2, Math.min(8, availableMinutes / avgStayTime));
    }

    /**
     * 使用纯Java算法解决 TSP 问题（最近邻 + 2-opt优化）
     */
    private List<POI> solveTSPWithORTools(List<POI> pois, BigDecimal startLat, BigDecimal startLng,
                                           OptimizationGoal goal) {
        if (pois.size() <= 2) {
            return pois; // 少于3个点无需优化
        }

        log.info("🧮 开始 TSP 求解 ({}个POI, 目标: {}) [纯Java算法]...", pois.size(), goal);

        try {
            int n = pois.size();

            // 构建带权重的距离矩阵
            double[][] weightedMatrix = new double[n][n];
            double[][] actualDistances = new double[n][n];

            double[] lats = new double[n + 1];
            double[] lngs = new double[n + 1];

            lats[0] = DistanceUtil.toDouble(startLat);
            lngs[0] = DistanceUtil.toDouble(startLng);

            for (int i = 0; i < n; i++) {
                lats[i + 1] = DistanceUtil.toDouble(pois.get(i).getLat());
                lngs[i + 1] = DistanceUtil.toDouble(pois.get(i).getLng());
            }

            // 填充距离矩阵（从起点到各POI，以及POI之间）
            for (int i = 0; i < n; i++) {
                // 从起点到POI i 的距离
                double distFromStart = DistanceUtil.calculateDistance(lats[0], lngs[0], lats[i + 1], lngs[i + 1]);
                actualDistances[0][i] = distFromStart;
                weightedMatrix[0][i] = applyOptimizationWeight(distFromStart, pois, -1, i, goal);

                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        double dist = DistanceUtil.calculateDistance(lats[i + 1], lngs[i + 1], lats[j + 1], lngs[j + 1]);
                        actualDistances[i][j] = dist;
                        weightedMatrix[i][j] = applyOptimizationWeight(dist, pois, i, j, goal);
                    }
                }
            }

            // 第一步：最近邻贪心初始化
            List<POI> ordered = nearestNeighborHeuristic(pois, weightedMatrix);

            log.info("📍 贪心初始路径: {}", ordered.stream().map(POI::getName).collect(Collectors.joining(" → ")));

            // 第二步：2-opt局部优化
            if (ordered.size() > 3) {
                ordered = twoOptOptimizationWithWeights(ordered, startLat, startLng, weightedMatrix);
            }

            log.info("✨ TSP 优化完成，路径包含 {} 个POI", ordered.size());

            return ordered;

        } catch (Exception e) {
            log.error("❌ TSP 求解失败: {}", e.getMessage(), e);
            return fallbackGreedyAlgorithm(pois, startLat, startLng);
        }
    }

    /**
     * 应用优化目标权重
     */
    private long applyOptimizationWeight(double distance, List<POI> pois, int fromIdx, int toIdx,
                                          OptimizationGoal goal) {
        if (fromIdx < 0 || toIdx < 0 || fromIdx >= pois.size() || toIdx >= pois.size()) {
            return (long) distance;
        }

        POI toPoi = pois.get(toIdx);
        double weightedDistance = distance;

        switch (goal) {
            case SHORTEST_TIME:
                // 考虑停留时间
                int stayTime = toPoi.getRecommendedDuration() != null ? toPoi.getRecommendedDuration() : 90;
                weightedDistance = distance + stayTime * 10; // 1分钟 ≈ 10米
                break;

            case LEAST_WALKING:
                // 纯距离，不调整
                break;

            case LOWEST_COST:
                // 高消费地点增加"距离"
                if (toPoi.getAvgCost() != null) {
                    weightedDistance = distance + toPoi.getAvgCost() * 50; // 1元 ≈ 50米
                }
                break;

            case LEAST_WAITING:
                // 排队时间长增加"距离"
                if (toPoi.getCurrentWaitTime() != null) {
                    weightedDistance = distance + toPoi.getCurrentWaitTime() * 20; // 1分钟排队 ≈ 20米
                }
                break;

            case HIGHEST_RATED:
                // 高评分减少"距离"（更吸引人）
                if (toPoi.getRating() != null) {
                    double ratingBonus = (5.0 - toPoi.getRating()) * 200; // 评分越高越近
                    weightedDistance = distance + ratingBonus;
                }
                break;

            case MOST_PERSONALIZED:
                // 综合考虑评分、热度、当前状态
                double personalScore = calculatePOIScore(toPoi);
                weightedDistance = distance * (2.0 - personalScore); // 分越高越近
                break;

            case BALANCED:
            default:
                // 默认：综合平衡
                double balanceScore = calculatePOIScore(toPoi);
                weightedDistance = distance * (1.5 - balanceScore);
                break;
        }

        return (long) Math.max(1, weightedDistance);
    }

    /**
     * 最近邻贪心算法初始化
     */
    private List<POI> nearestNeighborHeuristic(List<POI> pois, double[][] weightedMatrix) {
        List<POI> remaining = new ArrayList<>(pois);
        List<POI> ordered = new ArrayList<>();
        int currentIdx = -1; // -1 表示起点

        while (!remaining.isEmpty()) {
            POI nearest = null;
            double minWeight = Double.MAX_VALUE;
            int nearestIdx = -1;

            for (int i = 0; i < remaining.size(); i++) {
                POI poi = remaining.get(i);
                int poiGlobalIdx = pois.indexOf(poi);

                double weight;
                if (currentIdx == -1) {
                    // 从起点出发的距离权重
                    weight = weightedMatrix[0][poiGlobalIdx];
                } else {
                    // 从当前POI到下一个POI的距离权重
                    weight = weightedMatrix[currentIdx][poiGlobalIdx];
                }

                if (weight < minWeight) {
                    minWeight = weight;
                    nearest = poi;
                    nearestIdx = poiGlobalIdx;
                }
            }

            if (nearest != null) {
                ordered.add(nearest);
                currentIdx = nearestIdx;
                remaining.remove(nearest);
            } else {
                break;
            }
        }

        return ordered;
    }

    /**
     * 带权重的2-opt局部优化
     */
    private List<POI> twoOptOptimizationWithWeights(List<POI> route, BigDecimal startLat, BigDecimal startLng,
                                                     double[][] weightedMatrix) {
        boolean improved = true;
        List<POI> bestRoute = new ArrayList<>(route);
        double bestCost = calculateRouteCost(bestRoute, startLat, startLng, weightedMatrix);

        int iterations = 0;
        final int MAX_ITERATIONS = 100;

        while (improved && iterations++ < MAX_ITERATIONS) {
            improved = false;

            for (int i = 0; i < bestRoute.size() - 1; i++) {
                for (int j = i + 1; j < bestRoute.size(); j++) {
                    List<POI> newRoute = twoOptSwap(bestRoute, i, j);
                    double newCost = calculateRouteCost(newRoute, startLat, startLng, weightedMatrix);

                    if (newCost < bestCost - 0.001) { // 允许微小误差
                        bestRoute = newRoute;
                        bestCost = newCost;
                        improved = true;
                    }
                }
            }
        }

        log.info("🔄 2-opt优化: {}次迭代", iterations);
        return bestRoute;
    }

    /**
     * 回退算法：改进的贪心+2-opt
     */
    private List<POI> fallbackGreedyAlgorithm(List<POI> pois, BigDecimal startLat, BigDecimal startLng) {
        log.info("🔄 使用改进的贪心算法...");

        // 第一步：贪心初始化
        List<POI> remaining = new ArrayList<>(pois);
        List<POI> ordered = new ArrayList<>();

        double currentLat = DistanceUtil.toDouble(startLat);
        double currentLng = DistanceUtil.toDouble(startLng);

        while (!remaining.isEmpty()) {
            POI nearest = null;
            double minScore = Double.MAX_VALUE;

            for (POI poi : remaining) {
                double distance = DistanceUtil.calculateDistance(
                        currentLat, currentLng,
                        DistanceUtil.toDouble(poi.getLat()),
                        DistanceUtil.toDouble(poi.getLng())
                );

                // 综合考虑距离和POI质量
                double qualityScore = calculatePOIScore(poi);
                double combinedScore = distance / (0.5 + qualityScore);

                if (combinedScore < minScore) {
                    minScore = combinedScore;
                    nearest = poi;
                }
            }

            if (nearest != null) {
                ordered.add(nearest);
                currentLat = DistanceUtil.toDouble(nearest.getLat());
                currentLng = DistanceUtil.toDouble(nearest.getLng());
                remaining.remove(nearest);
            } else {
                break;
            }
        }

        // 第二步：2-opt 局部优化
        if (ordered.size() > 3) {
            ordered = twoOptOptimization(ordered, startLat, startLng);
        }

        return ordered;
    }

    /**
     * 2-opt 局部搜索优化
     */
    private List<POI> twoOptOptimization(List<POI> route, BigDecimal startLat, BigDecimal startLng) {
        boolean improved = true;
        List<POI> bestRoute = new ArrayList<>(route);

        while (improved) {
            improved = false;
            for (int i = 1; i < bestRoute.size() - 1; i++) {
                for (int j = i + 1; j < bestRoute.size(); j++) {
                    List<POI> newRoute = twoOptSwap(bestRoute, i, j);
                    if (calculateTotalDistance(newRoute, startLat, startLng) <
                            calculateTotalDistance(bestRoute, startLat, startLng)) {
                        bestRoute = newRoute;
                        improved = true;
                    }
                }
            }
        }

        return bestRoute;
    }

    /**
     * 2-opt 交换操作
     */
    private List<POI> twoOptSwap(List<POI> route, int i, int j) {
        List<POI> newRoute = new ArrayList<>();

        for (int k = 0; k < i; k++) {
            newRoute.add(route.get(k));
        }
        for (int k = j; k >= i; k--) {
            newRoute.add(route.get(k));
        }
        for (int k = j + 1; k < route.size(); k++) {
            newRoute.add(route.get(k));
        }

        return newRoute;
    }

    /**
     * 计算路径总距离
     */
    private double calculateTotalDistance(List<POI> route, BigDecimal startLat, BigDecimal startLng) {
        double totalDist = 0;
        double prevLat = DistanceUtil.toDouble(startLat);
        double prevLng = DistanceUtil.toDouble(startLng);

        for (POI poi : route) {
            double dist = DistanceUtil.calculateDistance(
                    prevLat, prevLng,
                    DistanceUtil.toDouble(poi.getLat()),
                    DistanceUtil.toDouble(poi.getLng())
            );
            totalDist += dist;
            prevLat = DistanceUtil.toDouble(poi.getLat());
            prevLng = DistanceUtil.toDouble(poi.getLng());
        }

        return totalDist;
    }

    /**
     * 计算路径总成本（带权重）
     */
    private double calculateRouteCost(List<POI> route, BigDecimal startLat, BigDecimal startLng,
                                       double[][] weightedMatrix) {
        double totalCost = 0;

        // 起点到第一个POI
        if (!route.isEmpty()) {
            int firstIdx = -1; // 需要在原始列表中查找索引
            // 简化处理：直接使用距离计算
            double firstDist = DistanceUtil.calculateDistance(
                    DistanceUtil.toDouble(startLat), DistanceUtil.toDouble(startLng),
                    DistanceUtil.toDouble(route.get(0).getLat()), DistanceUtil.toDouble(route.get(0).getLng())
            );
            totalCost += firstDist;
        }

        // POI之间
        for (int i = 0; i < route.size() - 1; i++) {
            double dist = DistanceUtil.calculateDistance(
                    DistanceUtil.toDouble(route.get(i).getLat()), DistanceUtil.toDouble(route.get(i).getLng()),
                    DistanceUtil.toDouble(route.get(i + 1).getLat()), DistanceUtil.toDouble(route.get(i + 1).getLng())
            );
            totalCost += dist;
        }

        return totalCost;
    }

    /**
     * 构建详细时间轴
     */
    private List<RouteSegment> buildDetailedTimeline(List<POI> pois, BigDecimal startLat,
                                                     BigDecimal startLng, Integer totalHours) {
        List<RouteSegment> segments = new ArrayList<>();

        if (pois.isEmpty()) return segments;

        double currentLat = DistanceUtil.toDouble(startLat);
        double currentLng = DistanceUtil.toDouble(startLng);
        LocalDateTime currentTime = LocalDateTime.now().with(LocalTime.of(10, 0));

        int sequence = 1;

        for (POI poi : pois) {
            double distance = DistanceUtil.calculateDistance(
                    currentLat, currentLng,
                    DistanceUtil.toDouble(poi.getLat()),
                    DistanceUtil.toDouble(poi.getLng())
            );

            int travelTime = estimateTransportMode(distance);
            int stayTime = poi.getRecommendedDuration() != null ? poi.getRecommendedDuration() : 90;

            LocalDateTime arriveTime = currentTime.plusMinutes(travelTime);
            LocalDateTime leaveTime = arriveTime.plusMinutes(stayTime);

            RouteSegment segment = RouteSegment.builder()
                    .sequence(sequence++)
                    .poi(poi)
                    .arriveTime(arriveTime)
                    .leaveTime(leaveTime)
                    .stayDuration(stayTime)
                    .transportMode(determineTransportMode(distance))
                    .travelDuration(travelTime)
                    .travelDistance((int) distance)
                    .expectedWaitTime(poi.getCurrentWaitTime())
                    .waitTimeTip(generateWaitTimeTip(poi))
                    .entryTip(generateEntryTip(poi))
                    .parkingInfo(generateParkingInfo(poi))
                    .nearbyAlternatives(findNearbyAlternatives(poi, pois))
                    .build();

            segments.add(segment);

            currentLat = DistanceUtil.toDouble(poi.getLat());
            currentLng = DistanceUtil.toDouble(poi.getLng());
            currentTime = leaveTime;
        }

        return segments;
    }

    /**
     * 估算交通方式和时间
     */
    private int estimateTransportMode(double distanceMeters) {
        if (distanceMeters < 500) {
            return (int) Math.ceil(distanceMeters / 80); // 步行
        } else if (distanceMeters < 3000) {
            return (int) Math.ceil(distanceMeters / 200); // 骑行/公交
        } else {
            return (int) Math.ceil(distanceMeters / 400); // 打车
        }
    }

    /**
     * 决定交通方式
     */
    private TransportMode determineTransportMode(double distanceMeters) {
        if (distanceMeters < 500) return TransportMode.WALK;
        if (distanceMeters < 3000) return TransportMode.BIKE;
        return TransportMode.DRIVE;
    }

    /**
     * 生成排队提示
     */
    private String generateWaitTimeTip(POI poi) {
        if (poi.getCurrentWaitTime() == null || poi.getCurrentWaitTime() == 0) {
            return null;
        }

        int waitTime = poi.getCurrentWaitTime();
        if (waitTime <= 10) {
            return "当前人少，建议立即前往";
        } else if (waitTime <= 30) {
            return String.format("预计排队%d分钟，建议提前预约", waitTime);
        } else {
            LocalTime now = LocalTime.now();
            if (now.isAfter(LocalTime.of(11, 30)) && now.isBefore(LocalTime.of(13, 30))) {
                return "午餐高峰期，建议14:00后前往";
            } else if (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(19, 0))) {
                return "晚餐高峰期，建议19:30后前往";
            }
            return String.format("当前排队较长(%d分钟)，建议错峰出行", waitTime);
        }
    }

    /**
     * 生成入场提示
     */
    private String generateEntryTip(POI poi) {
        if (poi.getFacilities() != null && poi.getFacilities().contains("停车场")) {
            return "有停车场，建议自驾";
        }
        if (poi.getTags() != null && poi.getTags().contains("需预约")) {
            return "记得提前在线预约";
        }
        return null;
    }

    /**
     * 生成停车信息
     */
    private String generateParkingInfo(POI poi) {
        if (poi.getFacilities() != null && poi.getFacilities().contains("停车场")) {
            return "景区停车场";
        }
        return null;
    }

    /**
     * 查找附近备选POI
     */
    private List<String> findNearbyAlternatives(POI currentPOI, List<POI> allPOIs) {
        return allPOIs.stream()
                .filter(p -> !p.getId().equals(currentPOI.getId()))
                .filter(p -> {
                    double dist = DistanceUtil.calculateDistance(
                            DistanceUtil.toDouble(currentPOI.getLat()),
                            DistanceUtil.toDouble(currentPOI.getLng()),
                            DistanceUtil.toDouble(p.getLat()),
                            DistanceUtil.toDouble(p.getLng())
                    );
                    return dist < 1000; // 1公里以内
                })
                .sorted(Comparator.comparingDouble(p -> {
                    return DistanceUtil.calculateDistance(
                            DistanceUtil.toDouble(currentPOI.getLat()),
                            DistanceUtil.toDouble(currentPOI.getLng()),
                            DistanceUtil.toDouble(p.getLat()),
                            DistanceUtil.toDouble(p.getLng())
                    );
                }))
                .limit(3)
                .map(POI::getName)
                .collect(Collectors.toList());
    }

    /**
     * 构建RouteVO
     */
    private RouteVO buildRouteVO(List<POI> pois, List<RouteSegment> segments,
                                  String query, String description, Integer totalHours) {
        int totalDuration = segments.stream()
                .mapToInt(s -> s.getStayDuration() + s.getTravelDuration()).sum();
        double totalCost = pois.stream()
                .mapToDouble(p -> p.getAvgCost() != null ? p.getAvgCost() : 0).sum();
        int totalDistance = segments.stream()
                .mapToInt(RouteSegment::getTravelDistance).sum();

        String summary = generateSummary(pois, totalDuration, totalCost);

        return RouteVO.builder()
                .mainRoute(Route.builder()
                        .id(UUID.randomUUID().toString())
                        .query(query)
                        .description(description)
                        .pois(pois)
                        .segments(segments)
                        .totalDuration(totalDuration)
                        .totalCost(totalCost)
                        .totalDistance(totalDistance)
                        .summary(summary)
                        .createdAt(LocalDateTime.now())
                        .build())
                .build();
    }

    /**
     * 生成概述文案
     */
    private String generateSummary(List<POI> pois, int totalDuration, double totalCost) {
        if (pois.isEmpty()) {
            return "未找到合适的路线";
        }

        String names = pois.stream()
                .map(POI::getName)
                .collect(Collectors.joining(" → "));

        int hours = totalDuration / 60;
        int minutes = totalDuration % 60;

        return String.format("%s\n⏱️ 总时长: %d小时%d分钟 | 💰 预计花费: %.0f元 | 📍 共%d个景点",
                names, hours, minutes, totalCost, pois.size());
    }

    /**
     * 生成智能备选方案
     */
    private List<RouteVO> generateSmartAlternatives(List<POI> allCandidates, BigDecimal startLat,
                                                     BigDecimal startLng, Integer totalHours,
                                                     Double maxBudget, OptimizationGoal primaryGoal) {
        List<RouteVO> alternatives = new ArrayList<>();

        // 备选1：省时版（减少POI数量，缩短停留时间）
        List<POI> fastRoute = allCandidates.stream()
                .limit(Math.max(2, allCandidates.size() - 1))
                .collect(Collectors.toList());
        List<POI> fastOptimized = solveTSPWithORTools(fastRoute, startLat, startLng,
                OptimizationGoal.SHORTEST_TIME);
        List<RouteSegment> fastSegments = buildDetailedTimeline(fastOptimized, startLat, startLng, totalHours);

        alternatives.add(buildRouteVO(fastOptimized, fastSegments,
                "", "⚡ 省时版：快速打卡精华", totalHours));

        // 备选2：深度体验版（如果候选足够多）
        if (allCandidates.size() > 4) {
            List<POI> deepRoute = allCandidates.stream()
                    .limit(Math.min(allCandidates.size(), 6))
                    .collect(Collectors.toList());
            List<POI> deepOptimized = solveTSPWithORTools(deepRoute, startLat, startLng,
                    OptimizationGoal.HIGHEST_RATED);
            List<RouteSegment> deepSegments = buildDetailedTimeline(deepOptimized, startLat, startLng, totalHours + 2);

            alternatives.add(buildRouteVO(deepOptimized, deepSegments,
                    "", "🎯 深度版：沉浸式体验", totalHours + 2));
        }

        // 备选3：省钱版（低消费POI优先）
        if (maxBudget != null) {
            List<POI> budgetRoute = allCandidates.stream()
                    .sorted(Comparator.comparingDouble(p ->
                            p.getAvgCost() != null ? p.getAvgCost() : 0))
                    .limit(allCandidates.size())
                    .collect(Collectors.toList());
            List<POI> budgetOptimized = solveTSPWithORTools(budgetRoute, startLat, startLng,
                    OptimizationGoal.LOWEST_COST);
            List<RouteSegment> budgetSegments = buildDetailedTimeline(budgetOptimized, startLat, startLng, totalHours);

            alternatives.add(buildRouteVO(budgetOptimized, budgetSegments,
                    "", "💰 省钱版：高性价比之选", totalHours));
        }

        return alternatives;
    }

    /**
     * 生成个性化说明
     */
    private String generatePersonalizationNote(OptimizationGoal goal, List<POI> selectedPOIs) {
        switch (goal) {
            case SHORTEST_TIME:
                return "⏱️ 已为您优化时间效率，适合行程紧凑的朋友";
            case LEAST_WALKING:
                return "🚶‍♂️ 已减少步行距离，适合喜欢轻松游览的朋友";
            case LOWEST_COST:
                return "💰 已控制预算，精选高性价比景点";
            case LEAST_WAITING:
                return "⏳ 已避开人流高峰，减少排队等待";
            case HIGHEST_RATED:
                return "⭐ 已优先选择高评分口碑景点";
            case MOST_PERSONALIZED:
                return "🎨 已根据您的偏好定制专属路线";
            case BALANCED:
            default:
                return "⚖️ 已综合考虑时间、费用、体验等因素";
        }
    }

    /**
     * 生成缓存key（基于所有参数的MD5哈希）
     */
    private String generateCacheKey(String query, BigDecimal startLat, BigDecimal startLng,
                                    Integer totalHours, List<String> mustVisit,
                                    List<String> categories, Double maxBudget,
                                    Integer maxWaitTime, OptimizationGoal optimizationGoal) {
        try {
            String keyString = String.format("%s|%s|%s|%d|%s|%s|%.2f|%d|%s",
                    query != null ? query : "",
                    startLat != null ? startLat.toString() : "",
                    startLng != null ? startLng.toString() : "",
                    totalHours != null ? totalHours : 0,
                    mustVisit != null ? mustVisit.toString() : "[]",
                    categories != null ? categories.toString() : "[]",
                    maxBudget != null ? maxBudget : 0.0,
                    maxWaitTime != null ? maxWaitTime : 0,
                    optimizationGoal != null ? optimizationGoal.name() : "BALANCED");

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(keyString.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "route:" + sb.toString();
        } catch (Exception e) {
            log.warn("生成缓存key失败，使用默认key", e);
            return "route:default:" + System.currentTimeMillis();
        }
    }

    /**
     * 创建空结果
     */
    private RouteVO createEmptyResult(String query, String message) {
        return RouteVO.builder()
                .requestId(UUID.randomUUID().toString())
                .success(false)
                .message(message)
                .mainRoute(Route.builder()
                        .id(UUID.randomUUID().toString())
                        .query(query)
                        .description(message)
                        .pois(new ArrayList<>())
                        .segments(new ArrayList<>())
                        .summary(message)
                        .createdAt(LocalDateTime.now())
                        .build())
                .alternatives(new ArrayList<>())
                .build();
    }
}
