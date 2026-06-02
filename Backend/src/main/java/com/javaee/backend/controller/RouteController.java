package com.javaee.backend.controller;

import com.javaee.backend.common.Result;
import com.javaee.backend.dto.RouteRequest;
import com.javaee.backend.entity.Route;
import com.javaee.backend.enums.OptimizationGoal;
import com.javaee.backend.po.vo.RouteVO;
import com.javaee.backend.service.DescriptionGeneratorService;
import com.javaee.backend.service.RoutePlannerService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@Slf4j
@RequestMapping("/route")
public class RouteController {

    @Autowired
    private RoutePlannerService routePlannerService;

    @Autowired
    private DescriptionGeneratorService descriptionGeneratorService;

    @PostMapping("/plan")
    public Result<RouteVO> planRoute(@RequestBody RouteRequest request) {
        log.info("📍 [Controller] 收到路线规划请求: query={}, user={}", 
                request.getQuery(), request.getUserId());

        try {
            OptimizationGoal optimizationGoal = null;
            if (request.getOptimizationGoal() != null && !request.getOptimizationGoal().isEmpty()) {
                try {
                    optimizationGoal = OptimizationGoal.valueOf(request.getOptimizationGoal());
                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ 无效的优化目标: {}, 使用默认BALANCED", request.getOptimizationGoal());
                    optimizationGoal = OptimizationGoal.BALANCED;
                }
            }

            RouteVO result = routePlannerService.planRoute(
                    request.getQuery(),
                    request.getUserId(),
                    request.getStartLat(),
                    request.getStartLng(),
                    request.getTotalHours(),
                    request.getMustVisit(),
                    request.getCategories(),
                    request.getMaxBudget(),
                    request.getMaxWaitTime(),
                    optimizationGoal
            );

            return Result.success("路线规划成功", result);

        } catch (Exception e) {
            log.error("❌ [Controller] 路线规划异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("路线规划服务运行正常");
    }

    @PostMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> planRouteStream(@RequestBody RouteRequest request) {
        log.info("🌊 [流式Controller] 收到路线规划请求: query={}, user={}", 
                request.getQuery(), request.getUserId());

        try {
            OptimizationGoal optimizationGoal = null;
            if (request.getOptimizationGoal() != null && !request.getOptimizationGoal().isEmpty()) {
                try {
                    optimizationGoal = OptimizationGoal.valueOf(request.getOptimizationGoal());
                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ 无效的优化目标: {}, 使用默认BALANCED", request.getOptimizationGoal());
                    optimizationGoal = OptimizationGoal.BALANCED;
                }
            }

            RouteVO result = routePlannerService.planRoute(
                    request.getQuery(),
                    request.getUserId(),
                    request.getStartLat(),
                    request.getStartLng(),
                    request.getTotalHours(),
                    request.getMustVisit(),
                    request.getCategories(),
                    request.getMaxBudget(),
                    request.getMaxWaitTime(),
                    optimizationGoal
            );

            Route mainRoute = result.getMainRoute();
            Integer totalHours = mainRoute != null && mainRoute.getTotalDuration() != null 
                    ? mainRoute.getTotalDuration() / 60 
                    : request.getTotalHours();
            
            return descriptionGeneratorService.generateRouteDescriptionStream(
                    request.getQuery(),
                    mainRoute != null ? mainRoute.getPois() : List.of(),
                    totalHours,
                    mainRoute != null ? mainRoute.getTotalCost() : null
            );

        } catch (Exception e) {
            log.error("❌ [流式Controller] 路线规划异常: {}", e.getMessage(), e);
            return Flux.just("错误: " + e.getMessage());
        }
    }
}
