package com.lsh.transaction.config;

import com.lsh.transaction.interceptor.GlobalRateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for interceptors and other web-related settings
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private GlobalRateLimitInterceptor globalRateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register global rate limiting interceptor, apply to all API paths
        registry.addInterceptor(globalRateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**");
    }
} 