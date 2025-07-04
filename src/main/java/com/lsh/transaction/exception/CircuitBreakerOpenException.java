package com.lsh.transaction.exception;

/**
 * Exception thrown when circuit breaker is open
 */
public class CircuitBreakerOpenException extends RuntimeException {
    
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
    
    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
} 