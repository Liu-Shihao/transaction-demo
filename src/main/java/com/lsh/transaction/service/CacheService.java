package com.lsh.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced cache service for handling concurrent scenarios and cache consistency.
 * This service provides advanced caching strategies to prevent cache-related issues.
 */
@Service
@Slf4j
public class CacheService {
    
    private final CacheManager cacheManager;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();
    
    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        log.info("CacheService initialized with cache manager");
    }
    
    /**
     * Get cache with null value protection to prevent cache penetration.
     * @param cacheName cache name
     * @param key cache key
     * @param supplier data supplier
     * @param <T> return type
     * @return cached value or supplier result
     */
    public <T> T getWithNullProtection(String cacheName, String key, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        log.info("Cache operation - Name: {}, Key: {}, Operation: getWithNullProtection", cacheName, key);
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.error("Cache not found - Name: {}", cacheName);
                throw new IllegalArgumentException("Cache not found: " + cacheName);
            }
            
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null && wrapper.get() != null) {
                incrementCacheHits(cacheName);
                long duration = System.currentTimeMillis() - startTime;
                log.info("Cache hit - Name: {}, Key: {}, Duration: {}ms", cacheName, key, duration);
                return (T) wrapper.get();
            }
            
            incrementCacheMisses(cacheName);
            log.info("Cache miss - Name: {}, Key: {}, Loading value", cacheName, key);
            
            T value = supplier.get();
            if (value != null) {
                cache.put(key, value);
                long duration = System.currentTimeMillis() - startTime;
                log.info("Value loaded and cached - Name: {}, Key: {}, Duration: {}ms", cacheName, key, duration);
            } else {
                log.info("Value loaded but is null - Name: {}, Key: {}", cacheName, key);
            }
            
            return value;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cache operation failed - Name: {}, Key: {}, Duration: {}ms, Error: {}", 
                    cacheName, key, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Atomic cache update with proper eviction strategy.
     * @param cacheName cache name
     * @param key cache key
     * @param value new value
     */
    public void atomicUpdate(String cacheName, String key, Object value) {
        long startTime = System.currentTimeMillis();
        log.info("Cache atomic update - Name: {}, Key: {}", cacheName, key);
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.error("Cache not found for atomic update - Name: {}", cacheName);
                throw new IllegalArgumentException("Cache not found: " + cacheName);
            }
            
            // Use lock to ensure atomic update
            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
            lock.lock();
            try {
                // Atomic update using put operation
                cache.put(key, value);
                long duration = System.currentTimeMillis() - startTime;
                log.info("Cache atomic update completed - Name: {}, Key: {}, Duration: {}ms", cacheName, key, duration);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cache atomic update failed - Name: {}, Key: {}, Duration: {}ms, Error: {}", 
                    cacheName, key, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Atomic cache update with version control to prevent stale data.
     * @param cacheName cache name
     * @param key cache key
     * @param value new value
     * @param version version number for optimistic locking
     */
    public boolean atomicUpdateWithVersion(String cacheName, String key, Object value, long version) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
            lock.lock();
            try {
                // Check if current version is older than new version
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null && wrapper.get() instanceof VersionedData) {
                    VersionedData currentData = (VersionedData) wrapper.get();
                    if (currentData.getVersion() >= version) {
                        return false; // Stale update, ignore
                    }
                }
                
                // Create versioned data
                VersionedData versionedData = new VersionedData(value, version);
                cache.put(key, versionedData);
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
    
    /**
     * Conditional cache eviction based on business logic.
     * @param cacheName cache name
     * @param key cache key
     * @param condition eviction condition
     */
    public void conditionalEvict(String cacheName, String key, boolean condition) {
        if (!condition) {
            log.info("Cache eviction skipped - Name: {}, Key: {}, Condition: false", cacheName, key);
            return;
        }
        
        long startTime = System.currentTimeMillis();
        log.info("Cache conditional eviction - Name: {}, Key: {}", cacheName, key);
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.error("Cache not found for eviction - Name: {}", cacheName);
                return;
            }
            
            cache.evict(key);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache eviction completed - Name: {}, Key: {}, Duration: {}ms", cacheName, key, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Cache eviction failed - Name: {}, Key: {}, Duration: {}ms, Error: {}", 
                    cacheName, key, duration, e.getMessage(), e);
        }
    }
    
    /**
     * Batch cache eviction with pattern matching.
     * @param cacheName cache name
     * @param pattern key pattern
     */
    public void evictByPattern(String cacheName, String pattern) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && cache.getNativeCache() instanceof java.util.Map) {
            java.util.Map nativeCache = (java.util.Map) cache.getNativeCache();
            nativeCache.entrySet().removeIf(entry -> 
                ((java.util.Map.Entry) entry).getKey().toString().matches(pattern));
        }
    }
    
    /**
     * Get cache statistics for monitoring.
     * @param cacheName cache name
     * @return cache statistics
     */
    public String getCacheStats(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && cache.getNativeCache() instanceof java.util.Map) {
            java.util.Map nativeCache = (java.util.Map) cache.getNativeCache();
            return String.format("Cache '%s' size: %d", cacheName, nativeCache.size());
        }
        return String.format("Cache '%s' not found", cacheName);
    }
    
    /**
     * Get cache statistics.
     * @param cacheName cache name
     * @return cache statistics
     */
    public CacheStatistics getCacheStatistics(String cacheName) {
        long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).get();
        long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0.0;
        
        log.info("Cache statistics - Name: {}, Hits: {}, Misses: {}, Total: {}, HitRate: {:.2f}%", 
                cacheName, hits, misses, total, hitRate);
        
        return new CacheStatistics(hits, misses, total, hitRate);
    }
    
    /**
     * Get all cache statistics.
     * @return all cache statistics
     */
    public ConcurrentHashMap<String, CacheStatistics> getAllCacheStatistics() {
        ConcurrentHashMap<String, CacheStatistics> allStats = new ConcurrentHashMap<>();
        
        cacheHits.keySet().forEach(cacheName -> {
            allStats.put(cacheName, getCacheStatistics(cacheName));
        });
        
        log.info("All cache statistics retrieved - Cache count: {}", allStats.size());
        return allStats;
    }
    
    private void incrementCacheHits(String cacheName) {
        cacheHits.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    private void incrementCacheMisses(String cacheName) {
        cacheMisses.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Cache statistics data class.
     */
    public static class CacheStatistics {
        private final long hits;
        private final long misses;
        private final long total;
        private final double hitRate;
        
        public CacheStatistics(long hits, long misses, long total, double hitRate) {
            this.hits = hits;
            this.misses = misses;
            this.total = total;
            this.hitRate = hitRate;
        }
        
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getTotal() { return total; }
        public double getHitRate() { return hitRate; }
    }
    
    /**
     * Versioned data wrapper for optimistic locking.
     */
    public static class VersionedData {
        private final Object data;
        private final long version;
        
        public VersionedData(Object data, long version) {
            this.data = data;
            this.version = version;
        }
        
        public Object getData() {
            return data;
        }
        
        public long getVersion() {
            return version;
        }
    }
} 