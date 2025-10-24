package com.trademaster.subscription.config;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * TimeLimiter Registry Configuration
 * MANDATORY: Single Responsibility - TimeLimiter configuration only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #24 - Timeout handling for all operations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
public class TimeLimiterRegistryConfig {

    private static final String SUBSCRIPTION_SERVICE = "subscription-service";
    private static final String PAYMENT_SERVICE = "payment-service";
    private static final String NOTIFICATION_SERVICE = "notification-service";
    private static final String DATABASE_SERVICE = "database-service";

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
