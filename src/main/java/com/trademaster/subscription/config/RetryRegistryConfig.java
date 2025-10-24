package com.trademaster.subscription.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Retry Registry Configuration
 * MANDATORY: Single Responsibility - Retry configuration only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #24 - Retry mechanism for transient failures
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class RetryRegistryConfig {

    private static final String SUBSCRIPTION_SERVICE = "subscription-service";
    private static final String PAYMENT_SERVICE = "payment-service";
    private static final String NOTIFICATION_SERVICE = "notification-service";
    private static final String DATABASE_SERVICE = "database-service";

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
}
