package com.banking.transaction.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Circuit breaker annotation for API endpoints
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    
    /**
     * Name of the circuit breaker
     */
    String name();
    
    /**
     * Failure rate threshold (percentage) to open circuit breaker
     */
    double failureRateThreshold() default 50.0;
    
    /**
     * Minimum number of calls to evaluate failure rate
     */
    int minimumNumberOfCalls() default 10;
    
    /**
     * Time window for failure rate calculation (seconds)
     */
    int slidingWindowSize() default 60;
    
    /**
     * Wait duration before transitioning from OPEN to HALF_OPEN (seconds)
     */
    int waitDurationInOpenState() default 30;
    
    /**
     * Error message when circuit breaker is open
     */
    String message() default "Service temporarily unavailable. Please try again later.";
} 