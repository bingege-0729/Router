package com.javaee.backend.config;

import com.javaee.backend.interceptor.RateLimitInterceptor;
import com.javaee.backend.interceptor.RequestLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final RequestLogInterceptor requestLogInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor, 
                     RequestLogInterceptor requestLogInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.requestLogInterceptor = requestLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLogInterceptor)
                .addPathPatterns("/**");
                
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/route/**")
                .excludePathPatterns("/api/poi/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
