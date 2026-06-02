package com.javaee.backend.interceptor;

import com.javaee.backend.annotation.RateLimiter;
import com.javaee.backend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, AtomicInteger> requestCountMap = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStartMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimiter rateLimiter = handlerMethod.getMethodAnnotation(RateLimiter.class);

        if (rateLimiter == null) {
            return true;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + handlerMethod.getMethod().getName();
        
        int limit = rateLimiter.value();
        long timeout = rateLimiter.timeout();

        long currentTime = System.currentTimeMillis();
        long windowStart = windowStartMap.computeIfAbsent(key, k -> currentTime);

        if (currentTime - windowStart > timeout) {
            synchronized (this) {
                if (System.currentTimeMillis() - windowStart > timeout) {
                    requestCountMap.remove(key);
                    windowStartMap.put(key, System.currentTimeMillis());
                }
            }
        }

        AtomicInteger count = requestCountMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        log.debug("🔒 [限流] key={}, count={}, limit={}", key, currentCount, limit);

        if (currentCount > limit) {
            log.warn("⚠️ [限流触发] IP={}, 接口={}, 限制: {}/{}", 
                    clientIp, handlerMethod.getMethod().getName(), limit, timeout);
            
            throw new BusinessException(429, rateLimiter.message());
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "unknown";
    }
}
