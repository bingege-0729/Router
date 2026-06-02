package com.javaee.backend.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    @Around("execution(* com.javaee.backend.controller..*(..)) || execution(* com.javaee.backend.service..*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        
        long startTime = System.currentTimeMillis();
        
        log.debug("⏱️ [方法调用] 开始 {}.{}()", className, methodName);
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (duration > 1000) {
                log.warn("🐌 [慢接口] {}.{}() 耗时: {}ms", className, methodName, duration);
            } else if (duration > 500) {
                log.info("⚠️ [较慢] {}.{}() 耗时: {}ms", className, methodName, duration);
            } else {
                log.debug("✅ [正常] {}.{}() 耗时: {}ms", className, methodName, duration);
            }
            
            return result;
            
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [异常] {}.{}() 耗时: {}ms, 异常: {}", 
                    className, methodName, duration, e.getMessage());
            throw e;
        }
    }
}
