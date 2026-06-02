package com.javaee.backend.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    int value() default 10;
    
    long timeout() default 1000;
    
    String message() default "请求过于频繁，请稍后再试";
}
