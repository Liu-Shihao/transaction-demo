package com.lsh.transaction.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

/**
 * Enhanced cache configuration for the application.
 * This class configures Spring Cache with advanced features for handling concurrent scenarios.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Configure enhanced cache manager for in-memory caching with concurrency support.
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Configure cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "transactions",
                "transactionStats",
                "transactionLocks"  // 用于分布式锁
        ));
        
        // Enable null values to prevent cache penetration
        cacheManager.setAllowNullValues(true);
        
        return cacheManager;
    }
    
    /**
     * Custom key generator for better cache key management.
     * @return KeyGenerator instance
     */
    @Bean
    public KeyGenerator customKeyGenerator() {
        return new SimpleKeyGenerator();
    }
} 