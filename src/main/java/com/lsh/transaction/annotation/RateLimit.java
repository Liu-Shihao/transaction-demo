package com.lsh.transaction.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate limiting annotation for API endpoints
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(com.lsh.transaction.annotation.RateLimits.class)
public @interface RateLimit {
    
    /**
     * Maximum number of requests allowed in the time window
     */
    int limit() default 1000;
    
    /**
     * Time window in seconds
     */
    int window() default 60;
    
    /**
     * Key for rate limiting (can be method name, user ID, etc.)
     */
    String key() default "";
    
    /**
     * SpEL expression to generate dynamic key (e.g., "#request.userId", "#request.accountNumber")
     */
    String keyExpression() default "";
    
    /**
     * Error message when rate limit is exceeded
     */
    String message() default "Rate limit exceeded. Please try again later.";
} 