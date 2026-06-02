package com.javaee.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PerformanceStatsService {

    private final Map<String, ApiStats> apiStatsMap = new ConcurrentHashMap<>();

    public void recordApiCall(String apiName, long durationMs, boolean success) {
        ApiStats stats = apiStatsMap.computeIfAbsent(apiName, k -> new ApiStats());
        
        stats.totalCalls.incrementAndGet();
        stats.totalDuration.addAndGet(durationMs);
        
        if (success) {
            stats.successCalls.incrementAndGet();
        } else {
            stats.failureCalls.incrementAndGet();
        }
        
        if (durationMs > 1000) {
            stats.slowCalls.incrementAndGet();
        }
        
        if (durationMs > stats.maxDuration.get()) {
            stats.maxDuration.set(durationMs);
        }
        
        if (durationMs < stats.minDuration.get() || stats.minDuration.get() == 0) {
            stats.minDuration.set(durationMs);
        }

        if (stats.totalCalls.get() % 100 == 0) {
            logApiStats(apiName, stats);
        }
    }

    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, ApiStats> entry : apiStatsMap.entrySet()) {
            String apiName = entry.getKey();
            ApiStats stats = entry.getValue();
            
            long totalCalls = stats.totalCalls.get();
            
            if (totalCalls > 0) {
                Map<String, Object> apiInfo = new ConcurrentHashMap<>();
                apiInfo.put("totalCalls", totalCalls);
                apiInfo.put("successRate", String.format("%.2f%%", 
                        (stats.successCalls.get() * 100.0 / totalCalls)));
                apiInfo.put("avgDuration", stats.totalDuration.get() / totalCalls);
                apiInfo.put("maxDuration", stats.maxDuration.get());
                apiInfo.put("minDuration", stats.minDuration.get());
                apiInfo.put("slowCalls", stats.slowCalls.get());
                apiInfo.put("slowRate", String.format("%.2f%%", 
                        (stats.slowCalls.get() * 100.0 / totalCalls)));
                
                report.put(apiName, apiInfo);
            }
        }
        
        return report;
    }

    public void logCurrentStats() {
        log.info("📊 ========== 性能统计报告 ==========");
        
        for (Map.Entry<String, ApiStats> entry : apiStatsMap.entrySet()) {
            logApiStats(entry.getKey(), entry.getValue());
        }
        
        log.info("📊 ==================================");
    }

    private void logApiStats(String apiName, ApiStats stats) {
        long totalCalls = stats.totalCalls.get();
        
        if (totalCalls == 0) return;
        
        double successRate = stats.successCalls.get() * 100.0 / totalCalls;
        double avgDuration = stats.totalDuration.get() / (double) totalCalls;
        double slowRate = stats.slowCalls.get() * 100.0 / totalCalls;
        
        log.info("📈 [API统计] {} | 调用: {}次 | 成功率: {:.2f}% | 平均耗时: {:.0f}ms | 慢接口率: {:.2f}% | 最大: {}ms | 最小: {}ms",
                apiName,
                totalCalls,
                successRate,
                avgDuration,
                slowRate,
                stats.maxDuration.get(),
                stats.minDuration.get()
        );
    }

    static class ApiStats {
        final AtomicLong totalCalls = new AtomicLong(0);
        final AtomicLong successCalls = new AtomicLong(0);
        final AtomicLong failureCalls = new AtomicLong(0);
        final AtomicLong slowCalls = new AtomicLong(0);
        final AtomicLong totalDuration = new AtomicLong(0);
        final AtomicLong maxDuration = new AtomicLong(0);
        final AtomicLong minDuration = new AtomicLong(0);
    }
}
