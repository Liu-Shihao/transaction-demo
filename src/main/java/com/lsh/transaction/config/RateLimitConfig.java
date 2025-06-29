package com.lsh.transaction.config;

import com.lsh.transaction.limiter.MemoryRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global rate limiting configuration
 */
@Configuration
public class RateLimitConfig {
    
    /**
     * Global rate limiter for entire service
     */
    @Bean("globalRateLimiter")
    public MemoryRateLimiter globalRateLimiter() {
        return new MemoryRateLimiter();
    }
    
    /**
     * API-specific rate limiter for transaction operations
     */
    @Bean("transactionApiRateLimiter")
    public MemoryRateLimiter transactionApiRateLimiter() {
        return new MemoryRateLimiter();
    }
    
    /**
     * Account-specific rate limiter for write operations
     */
    @Bean("accountWriteRateLimiter")
    public MemoryRateLimiter accountWriteRateLimiter() {
        return new MemoryRateLimiter();
    }
} 