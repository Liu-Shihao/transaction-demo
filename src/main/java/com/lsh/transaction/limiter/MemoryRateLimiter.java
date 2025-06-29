package com.banking.transaction.limiter;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory rate limiter implementation using sliding window algorithm
 * Fixed for high concurrency scenarios
 */
@Component
public class MemoryRateLimiter {
    
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    /**
     * Check if request is allowed based on rate limit
     * Thread-safe implementation for high concurrency
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = currentTime - windowSeconds;
        
        // Get or create lock for this key
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        
        try {
            lock.lock();
            
            WindowCounter counter = counters.get(key);
            
            // If no counter exists or window has expired, create new counter
            if (counter == null || counter.windowStart < windowStart) {
                counter = new WindowCounter(windowStart, new AtomicInteger(1));
                counters.put(key, counter);
                return true;
            }
            
            // Check if limit is exceeded
            int currentCount = counter.count.incrementAndGet();
            return currentCount <= limit;
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Get current count for a key
     */
    public int getCurrentCount(String key) {
        WindowCounter counter = counters.get(key);
        return counter != null ? counter.count.get() : 0;
    }
    
    /**
     * Reset counter for a key
     */
    public void reset(String key) {
        counters.remove(key);
        locks.remove(key);
    }
    
    /**
     * Clear all counters
     */
    public void clear() {
        counters.clear();
        locks.clear();
    }
    
    /**
     * Get all active counters for monitoring
     */
    public ConcurrentHashMap<String, WindowCounter> getAllCounters() {
        return new ConcurrentHashMap<>(counters);
    }
    
    /**
     * Inner class to hold window counter information
     */
    public static class WindowCounter {
        final long windowStart;
        final AtomicInteger count;
        
        WindowCounter(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
        
        public long getWindowStart() {
            return windowStart;
        }
        
        public int getCount() {
            return count.get();
        }
    }
} 