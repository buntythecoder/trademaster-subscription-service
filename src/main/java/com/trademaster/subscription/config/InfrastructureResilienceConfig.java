package com.trademaster.subscription.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Infrastructure Resilience Configuration
 * MANDATORY: Circuit Breaker Pattern - Rule #24
 * MANDATORY: Single Responsibility - Infrastructure resilience only
 *
 * Configures circuit breakers and retries for infrastructure services:
 * - Database operations
 * - Kafka event publishing
 *
 * @author TradeMaster Development Team
 */
@Configuration
@Slf4j
public class InfrastructureResilienceConfig {

    /**
     * Kafka Circuit Breaker for Event Publishing
     */
    @Bean("kafkaCircuitBreaker")
    public CircuitBreaker kafkaCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(80.0f)
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .slidingWindowSize(12)
            .minimumNumberOfCalls(6)
            .slowCallRateThreshold(95.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .recordExceptions(Exception.class)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("kafka", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.info("Kafka Circuit Breaker state transition: {}", event))
            .onFailureRateExceeded(event -> log.warn("Kafka Circuit Breaker failure rate exceeded: {}%", event.getFailureRate()))
            .onCallNotPermitted(event -> log.warn("Kafka Circuit Breaker call not permitted - events will be lost"));

        return circuitBreaker;
    }

    /**
     * Database Circuit Breaker for Repository Operations
     */
    @Bean("databaseCircuitBreaker")
    public CircuitBreaker databaseCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(90.0f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(15)
            .minimumNumberOfCalls(8)
            .slowCallRateThreshold(95.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(15))
            .recordExceptions(Exception.class)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("database", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.error("Database Circuit Breaker state transition: {}", event))
            .onFailureRateExceeded(event -> log.error("Database Circuit Breaker failure rate exceeded: {}%", event.getFailureRate()))
            .onCallNotPermitted(event -> log.error("Database Circuit Breaker call not permitted - critical system failure"));

        return circuitBreaker;
    }

    /**
     * Database Retry Configuration
     */
    @Bean("databaseRetry")
    public Retry databaseRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(3))
            .retryExceptions(Exception.class)
            .build();

        Retry retry = Retry.of("database", config);

        retry.getEventPublisher()
            .onRetry(event -> log.warn("Database retry attempt #{}: {}",
                event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        return retry;
    }
}
