package com.banking.transaction.aspect;

import com.banking.transaction.annotation.CircuitBreaker;
import com.banking.transaction.breaker.MemoryCircuitBreaker;
import com.banking.transaction.exception.CircuitBreakerOpenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for handling circuit breaker functionality
 */
@Aspect
@Component("customCircuitBreakerAspect")
public class CircuitBreakerAspect {
    
    @Autowired
    private MemoryCircuitBreaker circuitBreaker;
    
    @Around("@annotation(circuitBreaker)")
    public Object around(ProceedingJoinPoint joinPoint, CircuitBreaker circuitBreaker) throws Throwable {
        String name = circuitBreaker.name();
        
        // Check if circuit breaker is open
        if (!this.circuitBreaker.isAllowed(name)) {
            throw new CircuitBreakerOpenException(circuitBreaker.message());
        }
        
        try {
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Record success
            this.circuitBreaker.recordSuccess(name);
            
            return result;
        } catch (Exception e) {
            // Record failure
            this.circuitBreaker.recordFailure(name);
            
            // Re-throw the exception
            throw e;
        }
    }
} 