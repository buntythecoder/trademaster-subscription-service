package com.trademaster.subscription.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration
 * 
 * MANDATORY: Resilience Patterns - Rule #24
 * MANDATORY: Enterprise-grade fault tolerance for subscription operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class CircuitBreakerConfig {
    
    private static final String SUBSCRIPTION_SERVICE = "subscription-service";
    private static final String PAYMENT_SERVICE = "payment-service";
    private static final String NOTIFICATION_SERVICE = "notification-service";
    private static final String DATABASE_SERVICE = "database-service";
    
    /**
     * Circuit Breaker Registry with subscription-specific configurations
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Subscription Service Circuit Breaker
        registry.circuitBreaker(SUBSCRIPTION_SERVICE, 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                    java.io.IOException.class,
                    java.util.concurrent.TimeoutException.class,
                    org.springframework.web.client.ResourceAccessException.class
                )
                .build());
        
        // Payment Service Circuit Breaker
        registry.circuitBreaker(PAYMENT_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(60.0f)
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build());
        
        // Notification Service Circuit Breaker  
        registry.circuitBreaker(NOTIFICATION_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(40.0f)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .build());
        
        // Database Circuit Breaker
        registry.circuitBreaker(DATABASE_SERVICE,
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(30.0f)
                .slidingWindowSize(25)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .build());
        
        return registry;
    }
    
    /**
     * Retry Registry with exponential backoff
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        
        // Subscription Service Retry
        registry.retry(SUBSCRIPTION_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                    java.io.IOException.class,
                    java.util.concurrent.TimeoutException.class
                )
                .build());
        
        // Payment Service Retry
        registry.retry(PAYMENT_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(2))
                .build());
        
        // Notification Service Retry
        registry.retry(NOTIFICATION_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(4)
                .waitDuration(Duration.ofMillis(500))
                .build());
        
        // Database Retry
        registry.retry(DATABASE_SERVICE,
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofMillis(200))
                .build());
        
        return registry;
    }
    
    /**
     * TimeLimiter Registry for timeout handling
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        
        // Subscription Service TimeLimiter
        registry.timeLimiter(SUBSCRIPTION_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build());
        
        // Payment Service TimeLimiter
        registry.timeLimiter(PAYMENT_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(15))
                .cancelRunningFuture(true)
                .build());
        
        // Notification Service TimeLimiter
        registry.timeLimiter(NOTIFICATION_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(8))
                .cancelRunningFuture(true)
                .build());
        
        // Database TimeLimiter
        registry.timeLimiter(DATABASE_SERVICE,
            io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build());
        
        return registry;
    }
    
    /**
     * Individual CircuitBreaker beans for injection
     */
    @Bean
    public CircuitBreaker subscriptionCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(SUBSCRIPTION_SERVICE);
    }
    
    @Bean
    public CircuitBreaker paymentServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(PAYMENT_SERVICE);
    }
    
    @Bean
    public CircuitBreaker notificationServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(NOTIFICATION_SERVICE);
    }
    
    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(DATABASE_SERVICE);
    }
    
    /**
     * Individual Retry beans for injection
     */
    @Bean
    public Retry subscriptionRetry(RetryRegistry registry) {
        return registry.retry(SUBSCRIPTION_SERVICE);
    }
    
    @Bean
    public Retry paymentServiceRetry(RetryRegistry registry) {
        return registry.retry(PAYMENT_SERVICE);
    }
    
    @Bean
    public Retry notificationServiceRetry(RetryRegistry registry) {
        return registry.retry(NOTIFICATION_SERVICE);
    }
    
    @Bean
    public Retry databaseRetry(RetryRegistry registry) {
        return registry.retry(DATABASE_SERVICE);
    }
    
    /**
     * Individual TimeLimiter beans for injection
     */
    @Bean
    public TimeLimiter subscriptionTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(SUBSCRIPTION_SERVICE);
    }
    
    @Bean
    public TimeLimiter paymentServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(PAYMENT_SERVICE);
    }
    
    @Bean
    public TimeLimiter notificationServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(NOTIFICATION_SERVICE);
    }
    
    @Bean
    public TimeLimiter databaseTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter(DATABASE_SERVICE);
    }
}