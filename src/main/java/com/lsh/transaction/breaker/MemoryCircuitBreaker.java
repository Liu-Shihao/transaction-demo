package com.lsh.transaction.breaker;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory circuit breaker implementation
 */
@Component
public class MemoryCircuitBreaker {
    
    private final ConcurrentHashMap<String, CircuitBreakerState> breakers = new ConcurrentHashMap<>();
    
    /**
     * Circuit breaker states
     */
    public enum State {
        CLOSED,     // Normal operation
        OPEN,       // Circuit is open, requests are rejected
        HALF_OPEN   // Testing if service is back to normal
    }
    
    /**
     * Check if circuit breaker allows the request
     */
    public boolean isAllowed(String name) {
        CircuitBreakerState state = breakers.get(name);
        if (state == null) {
            state = new CircuitBreakerState();
            breakers.put(name, state);
        }
        
        return state.isAllowed();
    }
    
    /**
     * Record a successful call
     */
    public void recordSuccess(String name) {
        CircuitBreakerState state = breakers.get(name);
        if (state != null) {
            state.recordSuccess();
        }
    }
    
    /**
     * Record a failed call
     */
    public void recordFailure(String name) {
        CircuitBreakerState state = breakers.get(name);
        if (state != null) {
            state.recordFailure();
        }
    }
    
    /**
     * Get current state of circuit breaker
     */
    public State getState(String name) {
        CircuitBreakerState state = breakers.get(name);
        return state != null ? state.getCurrentState() : State.CLOSED;
    }
    
    /**
     * Reset circuit breaker
     */
    public void reset(String name) {
        breakers.remove(name);
    }
    
    /**
     * Inner class to hold circuit breaker state
     */
    private static class CircuitBreakerState {
        private volatile State state = State.CLOSED;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger totalCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;
        private volatile long openTime = 0;
        
        // Configuration
        private static final double FAILURE_RATE_THRESHOLD = 50.0;
        private static final int MINIMUM_CALLS = 10;
        private static final int SLIDING_WINDOW_SIZE = 60; // seconds
        private static final int WAIT_DURATION = 30; // seconds
        
        public boolean isAllowed() {
            long currentTime = Instant.now().getEpochSecond();
            
            switch (state) {
                case CLOSED:
                    return true;
                    
                case OPEN:
                    if (currentTime - openTime >= WAIT_DURATION) {
                        state = State.HALF_OPEN;
                        return true;
                    }
                    return false;
                    
                case HALF_OPEN:
                    return true;
                    
                default:
                    return true;
            }
        }
        
        public void recordSuccess() {
            totalCount.incrementAndGet();
            successCount.incrementAndGet();
            
            if (state == State.HALF_OPEN) {
                // If we get enough successes in half-open state, close the circuit
                if (successCount.get() >= MINIMUM_CALLS / 2) {
                    state = State.CLOSED;
                    resetCounters();
                }
            }
        }
        
        public void recordFailure() {
            totalCount.incrementAndGet();
            failureCount.incrementAndGet();
            lastFailureTime = Instant.now().getEpochSecond();
            
            if (state == State.CLOSED) {
                // Check if we should open the circuit
                if (totalCount.get() >= MINIMUM_CALLS) {
                    double failureRate = (double) failureCount.get() / totalCount.get() * 100;
                    if (failureRate >= FAILURE_RATE_THRESHOLD) {
                        state = State.OPEN;
                        openTime = Instant.now().getEpochSecond();
                    }
                }
            } else if (state == State.HALF_OPEN) {
                // If we get a failure in half-open state, open the circuit again
                state = State.OPEN;
                openTime = Instant.now().getEpochSecond();
            }
        }
        
        public State getCurrentState() {
            return state;
        }
        
        private void resetCounters() {
            successCount.set(0);
            failureCount.set(0);
            totalCount.set(0);
        }
    }
} 