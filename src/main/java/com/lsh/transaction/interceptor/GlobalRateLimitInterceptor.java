package com.lsh.transaction.interceptor;

import com.lsh.transaction.exception.RateLimitExceededException;
import com.lsh.transaction.limiter.MemoryRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Global rate limiting interceptor for all API requests
 */
@Component
public class GlobalRateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    @Qualifier("globalRateLimiter")
    private MemoryRateLimiter globalRateLimiter;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Generate rate limit key: IP + User-Agent
        String rateLimitKey = "global:" + clientIp + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
        
        // Global rate limiting: Based on actual performance test results
        // QPS: ~2500, Safe operating range: 80% of max capacity
        // 2500 * 0.8 * 60 = 120,000 requests/minute per client
        int globalLimit = (int)(2500 * 0.8 * 60); // 120,000 requests/minute
        if (!globalRateLimiter.isAllowed(rateLimitKey, globalLimit, 60)) {
            throw new RateLimitExceededException("Global rate limit exceeded. Please try again later.");
        }
        
        return true;
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 