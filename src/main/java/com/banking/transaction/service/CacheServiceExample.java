package com.banking.transaction.service;

import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CacheService usage examples and best practices.
 * This class demonstrates how to use CacheService effectively in different scenarios.
 */
@Service
public class CacheServiceExample {
    
    private final CacheService cacheService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public CacheServiceExample(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    /**
     * Example 1: Basic cache usage with null protection
     */
    public String getCachedData(String key) {
        return cacheService.getWithNullProtection(
            "exampleCache",
            key,
            () -> {
                // 模拟从数据库获取数据
                System.out.println("Fetching data from database for key: " + key);
                return "Data for " + key;
            }
        );
    }
    
    /**
     * Example 2: Cache with expensive computation
     */
    public String getExpensiveData(String userId) {
        return cacheService.getWithNullProtection(
            "userStats",
            "stats_" + userId,
            () -> {
                // 模拟复杂的计算操作
                System.out.println("Computing expensive statistics for user: " + userId);
                try {
                    Thread.sleep(1000); // 模拟计算时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Expensive stats for user " + userId;
            }
        );
    }
    
    /**
     * Example 3: Atomic cache update
     */
    public void updateUserProfile(String userId, String newProfile) {
        // 先更新数据库
        // userRepository.updateProfile(userId, newProfile);
        
        // 然后原子性地更新缓存
        cacheService.atomicUpdate("userProfiles", userId, newProfile);
        
        System.out.println("Updated cache for user: " + userId);
    }
    
    /**
     * Example 4: Conditional cache eviction
     */
    public void deleteUserData(String userId, boolean forceDelete) {
        // 模拟删除操作
        boolean deleted = true; // userRepository.deleteUser(userId);
        
        // 只有在删除成功且强制删除时才清除缓存
        cacheService.conditionalEvict("userProfiles", userId, deleted && forceDelete);
        
        System.out.println("Conditional cache eviction for user: " + userId);
    }
    
    /**
     * Example 5: Pattern-based cache eviction
     */
    public void clearUserRelatedCache(String userId) {
        // 清除所有与该用户相关的缓存
        cacheService.evictByPattern("userStats", ".*" + userId + ".*");
        cacheService.evictByPattern("userProfiles", ".*" + userId + ".*");
        
        System.out.println("Cleared all cache related to user: " + userId);
    }
    
    /**
     * Example 6: Concurrent access simulation
     */
    public void simulateConcurrentAccess(String key) {
        List<CompletableFuture<String>> futures = List.of(
            CompletableFuture.supplyAsync(() -> getCachedData(key), executor),
            CompletableFuture.supplyAsync(() -> getCachedData(key), executor),
            CompletableFuture.supplyAsync(() -> getCachedData(key), executor),
            CompletableFuture.supplyAsync(() -> getCachedData(key), executor),
            CompletableFuture.supplyAsync(() -> getCachedData(key), executor)
        );
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        System.out.println("Concurrent access completed for key: " + key);
    }
    
    /**
     * Example 7: Cache monitoring
     */
    public void monitorCache() {
        System.out.println("=== Cache Statistics ===");
        System.out.println(cacheService.getCacheStats("exampleCache"));
        System.out.println(cacheService.getCacheStats("userStats"));
        System.out.println(cacheService.getCacheStats("userProfiles"));
        System.out.println("========================");
    }
    
    /**
     * Example 8: Cache warming
     */
    public void warmCache() {
        System.out.println("Warming up cache...");
        
        // 预热常用数据
        List<String> commonKeys = List.of("key1", "key2", "key3", "key4", "key5");
        
        commonKeys.parallelStream().forEach(key -> {
            getCachedData(key);
        });
        
        System.out.println("Cache warming completed");
    }
    
    /**
     * Example 9: Error handling in cache operations
     */
    public String getDataWithErrorHandling(String key) {
        try {
            return cacheService.getWithNullProtection(
                "errorHandlingCache",
                key,
                () -> {
                    // 模拟可能出错的操作
                    if (key.equals("error")) {
                        throw new RuntimeException("Simulated error");
                    }
                    return "Data for " + key;
                }
            );
        } catch (Exception e) {
            System.err.println("Error getting data for key: " + key + ", Error: " + e.getMessage());
            return "Default data";
        }
    }
    
    /**
     * Example 10: Cache with TTL simulation
     */
    public String getDataWithTTL(String key) {
        return cacheService.getWithNullProtection(
            "ttlCache",
            key + "_" + (System.currentTimeMillis() / 60000), // 每分钟更新一次
            () -> {
                System.out.println("Fetching fresh data for key: " + key);
                return "Fresh data for " + key + " at " + System.currentTimeMillis();
            }
        );
    }
    
    /**
     * Cleanup method
     */
    public void cleanup() {
        executor.shutdown();
    }
} 