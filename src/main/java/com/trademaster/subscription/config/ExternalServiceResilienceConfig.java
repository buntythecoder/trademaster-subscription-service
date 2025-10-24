package com.trademaster.subscription.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * External Service Resilience Configuration
 * MANDATORY: Circuit Breaker Pattern - Rule #24
 * MANDATORY: Single Responsibility - External service integrations only
 *
 * Configures circuit breakers and retries for external services:
 * - Payment Service
 * - User Profile Service
 * - Notification Service
 *
 * @author TradeMaster Development Team
 */
@Configuration
@Slf4j
public class ExternalServiceResilienceConfig {

    @Value("${trademaster.integration.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${trademaster.integration.read-timeout:10000}")
    private int readTimeout;

    /**
     * RestTemplate with connection pooling and timeouts
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(connectionTimeout);
                factory.setReadTimeout(readTimeout);
                return factory;
            })
            .build();
    }

    /**
     * Payment Service Circuit Breaker
     */
    @Bean("paymentServiceCircuitBreaker")
    public CircuitBreaker paymentServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .slowCallRateThreshold(90.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .recordExceptions(Exception.class)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("paymentService", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.info("Payment Service Circuit Breaker state transition: {}", event))
            .onFailureRateExceeded(event -> log.warn("Payment Service Circuit Breaker failure rate exceeded: {}%", event.getFailureRate()))
            .onCallNotPermitted(event -> log.warn("Payment Service Circuit Breaker call not permitted"))
            .onSlowCallRateExceeded(event -> log.warn("Payment Service Circuit Breaker slow call rate exceeded: {}%", event.getSlowCallRate()));

        return circuitBreaker;
    }

    /**
     * Payment Service Retry Configuration
     */
    @Bean("paymentServiceRetry")
    public Retry paymentServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .retryExceptions(Exception.class)
            .build();

        Retry retry = Retry.of("paymentService", config);

        retry.getEventPublisher()
            .onRetry(event -> log.info("Payment Service retry attempt #{}: {}",
                event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        return retry;
    }

    /**
     * User Profile Service Circuit Breaker
     */
    @Bean("userProfileServiceCircuitBreaker")
    public CircuitBreaker userProfileServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .slidingWindowSize(8)
            .minimumNumberOfCalls(4)
            .slowCallRateThreshold(80.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .recordExceptions(Exception.class)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("userProfileService", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.info("User Profile Service Circuit Breaker state transition: {}", event))
            .onFailureRateExceeded(event -> log.warn("User Profile Service Circuit Breaker failure rate exceeded: {}%", event.getFailureRate()))
            .onCallNotPermitted(event -> log.warn("User Profile Service Circuit Breaker call not permitted"));

        return circuitBreaker;
    }

    /**
     * User Profile Service Retry Configuration
     */
    @Bean("userProfileServiceRetry")
    public Retry userProfileServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(Exception.class)
            .build();

        Retry retry = Retry.of("userProfileService", config);

        retry.getEventPublisher()
            .onRetry(event -> log.info("User Profile Service retry attempt #{}: {}",
                event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        return retry;
    }

    /**
     * Notification Service Circuit Breaker
     */
    @Bean("notificationServiceCircuitBreaker")
    public CircuitBreaker notificationServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(70.0f)
            .waitDurationInOpenState(Duration.ofSeconds(15))
            .slidingWindowSize(6)
            .minimumNumberOfCalls(3)
            .slowCallRateThreshold(90.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(4))
            .recordExceptions(Exception.class)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("notificationService", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.info("Notification Service Circuit Breaker state transition: {}", event))
            .onFailureRateExceeded(event -> log.warn("Notification Service Circuit Breaker failure rate exceeded: {}%", event.getFailureRate()))
            .onCallNotPermitted(event -> log.warn("Notification Service Circuit Breaker call not permitted"));

        return circuitBreaker;
    }

    /**
     * Notification Service Retry Configuration
     */
    @Bean("notificationServiceRetry")
    public Retry notificationServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(Exception.class)
            .build();

        Retry retry = Retry.of("notificationService", config);

        retry.getEventPublisher()
            .onRetry(event -> log.info("Notification Service retry attempt #{}: {}",
                event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        return retry;
    }
}
