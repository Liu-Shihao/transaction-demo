package com.banking.transaction.aspect;

import com.banking.transaction.annotation.RateLimit;
import com.banking.transaction.annotation.RateLimits;
import com.banking.transaction.exception.RateLimitExceededException;
import com.banking.transaction.limiter.MemoryRateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for handling rate limiting
 */
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    @Qualifier("memoryRateLimiter")
    private MemoryRateLimiter rateLimiter;
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = generateKey(joinPoint, rateLimit);
        
        if (!rateLimiter.isAllowed(key, rateLimit.limit(), rateLimit.window())) {
            throw new RateLimitExceededException(rateLimit.message());
        }
        
        return joinPoint.proceed();
    }
    
    @Around("@annotation(rateLimits)")
    public Object aroundMultiple(ProceedingJoinPoint joinPoint, RateLimits rateLimits) throws Throwable {
        // Check all rate limiting rules
        for (RateLimit rateLimit : rateLimits.value()) {
            String key = generateKey(joinPoint, rateLimit);
            
            if (!rateLimiter.isAllowed(key, rateLimit.limit(), rateLimit.window())) {
                throw new RateLimitExceededException(rateLimit.message());
            }
        }
        
        return joinPoint.proceed();
    }
    
    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // Priority: use custom key
        if (!rateLimit.key().isEmpty()) {
            return rateLimit.key();
        }
        
        // Use SpEL expression to generate dynamic key
        if (!rateLimit.keyExpression().isEmpty()) {
            return evaluateExpression(joinPoint, rateLimit.keyExpression());
        }
        
        // Default: use method signature as key
        String methodName = joinPoint.getSignature().toShortString();
        return methodName;
    }
    
    private String evaluateExpression(ProceedingJoinPoint joinPoint, String expression) {
        try {
            Expression exp = parser.parseExpression(expression);
            EvaluationContext context = new StandardEvaluationContext();
            
            // Set method parameters to context
            Object[] args = joinPoint.getArgs();
            String[] paramNames = getParameterNames(joinPoint);
            
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            
            Object result = exp.getValue(context);
            return result != null ? result.toString() : "unknown";
        } catch (Exception e) {
            // If expression parsing fails, return default key
            return joinPoint.getSignature().toShortString();
        }
    }
    
    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        // Simplified implementation, in real projects you can use more complex methods to get parameter names
        return new String[]{"request", "id", "accountNumber", "transactionType"};
    }
} 