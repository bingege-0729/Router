package com.javaee.backend.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = generateRequestId();
        
        request.setAttribute("requestId", requestId);
        request.setAttribute("startTime", System.currentTimeMillis());
        
        log.info("📥 [请求开始] ID={}, Method={}, URI={}, IP={}, UA={}", 
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request),
                request.getHeader("User-Agent"));
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        String requestId = (String) request.getAttribute("requestId");
        Long startTime = (Long) request.getAttribute("startTime");
        
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        log.info("📤 [请求结束] ID={}, Status={}, 耗时: {}ms{}", 
                requestId,
                response.getStatus(),
                duration,
                ex != null ? ", 异常: " + ex.getMessage() : "");
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip : "unknown";
    }
}
