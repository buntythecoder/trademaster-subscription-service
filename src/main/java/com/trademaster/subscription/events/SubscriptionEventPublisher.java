package com.trademaster.subscription.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Subscription Event Publisher for Event-Driven Architecture
 * 
 * MANDATORY: Event-Driven Communication Pattern
 * MANDATORY: Circuit Breaker for Kafka Resilience
 * MANDATORY: Functional Programming Patterns
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker kafkaCircuitBreaker;
    
    @Value("${trademaster.kafka.topics.subscription-events:subscription-events}")
    private String subscriptionEventsTopic;
    
    @Value("${trademaster.kafka.topics.user-events:user-events}")
    private String userEventsTopic;
    
    @Value("${trademaster.kafka.topics.billing-events:billing-events}")
    private String billingEventsTopic;
    
    /**
     * Publish subscription created event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCreated(
            UUID subscriptionId, UUID userId, String tier, String billingCycle) {
        
        SubscriptionEvent event = new SubscriptionEvent(
            UUID.randomUUID(),
            "SUBSCRIPTION_CREATED",
            subscriptionId,
            userId,
            Map.of(
                "tier", tier,
                "billingCycle", billingCycle,
                "status", "PENDING"
            ),
            LocalDateTime.now()
        );
        
        return publishEvent(subscriptionEventsTopic, subscriptionId.toString(), event);
    }
    
    /**
     * Publish subscription activated event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionActivated(
            UUID subscriptionId, UUID userId, String tier) {
        
        SubscriptionEvent event = new SubscriptionEvent(
            UUID.randomUUID(),
            "SUBSCRIPTION_ACTIVATED",
            subscriptionId,
            userId,
            Map.of(
                "tier", tier,
                "status", "ACTIVE"
            ),
            LocalDateTime.now()
        );
        
        return publishEvent(subscriptionEventsTopic, subscriptionId.toString(), event);
    }
    
    /**
     * Publish subscription cancelled event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCancelled(
            UUID subscriptionId, UUID userId, String tier, String reason) {
        
        SubscriptionEvent event = new SubscriptionEvent(
            UUID.randomUUID(),
            "SUBSCRIPTION_CANCELLED",
            subscriptionId,
            userId,
            Map.of(
                "tier", tier,
                "cancellationReason", reason != null ? reason : "",
                "status", "CANCELLED"
            ),
            LocalDateTime.now()
        );
        
        return publishEvent(subscriptionEventsTopic, subscriptionId.toString(), event);
    }
    
    /**
     * Publish subscription upgraded event
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionUpgraded(
            UUID subscriptionId, UUID userId, String oldTier, String newTier) {
        
        SubscriptionEvent event = new SubscriptionEvent(
            UUID.randomUUID(),
            "SUBSCRIPTION_UPGRADED",
            subscriptionId,
            userId,
            Map.of(
                "oldTier", oldTier,
                "newTier", newTier,
                "status", "ACTIVE"
            ),
            LocalDateTime.now()
        );
        
        return publishEvent(subscriptionEventsTopic, subscriptionId.toString(), event);
    }
    
    /**
     * Publish billing failure event
     */
    public CompletableFuture<Result<Void, String>> publishBillingFailure(
            UUID subscriptionId, UUID userId, String tier, int failedAttempts) {
        
        BillingEvent event = new BillingEvent(
            UUID.randomUUID(),
            "BILLING_FAILED",
            subscriptionId,
            userId,
            Map.of(
                "tier", tier,
                "failedAttempts", String.valueOf(failedAttempts),
                "status", "PAYMENT_FAILED"
            ),
            LocalDateTime.now()
        );
        
        return publishBillingEvent(billingEventsTopic, subscriptionId.toString(), event);
    }
    
    /**
     * Publish usage limit exceeded event
     */
    public CompletableFuture<Result<Void, String>> publishUsageLimitExceeded(
            UUID subscriptionId, UUID userId, String featureName, String tier) {
        
        UserEvent event = new UserEvent(
            UUID.randomUUID(),
            "USAGE_LIMIT_EXCEEDED",
            userId,
            subscriptionId,
            Map.of(
                "featureName", featureName,
                "tier", tier,
                "action", "LIMIT_ENFORCEMENT"
            ),
            LocalDateTime.now()
        );
        
        return publishUserEvent(userEventsTopic, userId.toString(), event);
    }
    
    /**
     * Publish trial ending event
     */
    public CompletableFuture<Result<Void, String>> publishTrialEnding(
            UUID subscriptionId, UUID userId, int daysRemaining) {
        
        SubscriptionEvent event = new SubscriptionEvent(
            UUID.randomUUID(),
            "TRIAL_ENDING",
            subscriptionId,
            userId,
            Map.of(
                "daysRemaining", String.valueOf(daysRemaining),
                "status", "TRIAL"
            ),
            LocalDateTime.now()
        );
        
        return publishEvent(subscriptionEventsTopic, subscriptionId.toString(), event);
    }
    
    // Generic event publishing with circuit breaker
    private CompletableFuture<Result<Void, String>> publishEvent(String topic, String key, SubscriptionEvent event) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    String eventJson = objectMapper.writeValueAsString(event);
                    
                    log.info("Publishing {} event for subscription: {}", event.eventType(), event.subscriptionId());
                    
                    CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
                    future.join(); // Wait for completion
                    
                    return Result.<Void, String>success(null);
                } catch (JsonProcessingException e) {
                    log.error("Event serialization failed for subscription: {}", event.subscriptionId(), e);
                    return Result.<Void, String>failure("Event serialization failed: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Event publishing failed for subscription: {}", event.subscriptionId(), e);
                    return Result.<Void, String>failure("Event publishing failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    private CompletableFuture<Result<Void, String>> publishBillingEvent(String topic, String key, BillingEvent event) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    String eventJson = objectMapper.writeValueAsString(event);
                    
                    log.info("Publishing {} billing event for subscription: {}", event.eventType(), event.subscriptionId());
                    
                    CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
                    future.join(); // Wait for completion
                    
                    return Result.<Void, String>success(null);
                } catch (JsonProcessingException e) {
                    log.error("Billing event serialization failed for subscription: {}", event.subscriptionId(), e);
                    return Result.<Void, String>failure("Billing event serialization failed: " + e.getMessage());
                } catch (Exception e) {
                    log.error("Billing event publishing failed for subscription: {}", event.subscriptionId(), e);
                    return Result.<Void, String>failure("Billing event publishing failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    private CompletableFuture<Result<Void, String>> publishUserEvent(String topic, String key, UserEvent event) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    String eventJson = objectMapper.writeValueAsString(event);
                    
                    log.info("Publishing {} user event for user: {}", event.eventType(), event.userId());
                    
                    CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);
                    future.join(); // Wait for completion
                    
                    return Result.<Void, String>success(null);
                } catch (JsonProcessingException e) {
                    log.error("User event serialization failed for user: {}", event.userId(), e);
                    return Result.<Void, String>failure("User event serialization failed: " + e.getMessage());
                } catch (Exception e) {
                    log.error("User event publishing failed for user: {}", event.userId(), e);
                    return Result.<Void, String>failure("User event publishing failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        return kafkaCircuitBreaker.executeSupplier(operation);
    }
    
    // Event DTOs
    
    public record SubscriptionEvent(
        UUID eventId,
        String eventType,
        UUID subscriptionId,
        UUID userId,
        Map<String, String> metadata,
        LocalDateTime timestamp
    ) {}
    
    public record BillingEvent(
        UUID eventId,
        String eventType,
        UUID subscriptionId,
        UUID userId,
        Map<String, String> metadata,
        LocalDateTime timestamp
    ) {}
    
    public record UserEvent(
        UUID eventId,
        String eventType,
        UUID userId,
        UUID subscriptionId,
        Map<String, String> metadata,
        LocalDateTime timestamp
    ) {}
}