package com.javaee.backend.cache;


import com.javaee.backend.entity.Route;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RouteCacheEntry {
    private String cacheKey;        // 请求的hash
    private Route route;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}