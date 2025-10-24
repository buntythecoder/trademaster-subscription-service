package com.trademaster.subscription.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Registry Configuration
 * MANDATORY: Single Responsibility - Circuit breaker configuration only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #24 - Circuit breaker for all external calls
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class CircuitBreakerRegistryConfig {

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
}
