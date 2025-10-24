package com.trademaster.subscription.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.constants.SubscriptionEventConstants;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Lifecycle Event Publisher
 * MANDATORY: Single Responsibility - Subscription lifecycle events only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Publishes subscription lifecycle events (created, activated, cancelled, upgraded, trial).
 * Extends BaseEventPublisher for shared Kafka infrastructure.
 *
 * @author TradeMaster Development Team
 */
@Component
public class SubscriptionLifecycleEventPublisher extends BaseEventPublisher {

    @Value("${trademaster.kafka.topics.subscription-events:subscription-events}")
    private String subscriptionEventsTopic;

    public SubscriptionLifecycleEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            CircuitBreaker kafkaCircuitBreaker) {
        super(kafkaTemplate, objectMapper, kafkaCircuitBreaker);
    }

    /**
     * Publish subscription created event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCreated(
            UUID subscriptionId, UUID userId, String tier, String billingCycle) {

        SubscriptionEvent event = buildSubscriptionEvent(
            subscriptionId,
            userId,
            SubscriptionEventConstants.SUBSCRIPTION_CREATED,
            Map.of(
                "tier", tier,
                "billingCycle", billingCycle,
                "status", "PENDING"
            )
        );

        return publishToKafka(subscriptionEventsTopic, subscriptionId.toString(), event,
                "subscription created");
    }

    /**
     * Publish subscription activated event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionActivated(
            UUID subscriptionId, UUID userId, String tier) {

        SubscriptionEvent event = buildSubscriptionEvent(
            subscriptionId,
            userId,
            SubscriptionEventConstants.SUBSCRIPTION_ACTIVATED,
            Map.of(
                "tier", tier,
                "status", "ACTIVE"
            )
        );

        return publishToKafka(subscriptionEventsTopic, subscriptionId.toString(), event,
                "subscription activated");
    }

    /**
     * Publish subscription cancelled event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionCancelled(
            UUID subscriptionId, UUID userId, String tier, String reason) {

        SubscriptionEvent event = buildSubscriptionEvent(
            subscriptionId,
            userId,
            SubscriptionEventConstants.SUBSCRIPTION_CANCELLED,
            Map.of(
                "tier", tier,
                "cancellationReason", reason != null ? reason : "",
                "status", "CANCELLED"
            )
        );

        return publishToKafka(subscriptionEventsTopic, subscriptionId.toString(), event,
                "subscription cancelled");
    }

    /**
     * Publish subscription upgraded event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishSubscriptionUpgraded(
            UUID subscriptionId, UUID userId, String oldTier, String newTier) {

        SubscriptionEvent event = buildSubscriptionEvent(
            subscriptionId,
            userId,
            SubscriptionEventConstants.SUBSCRIPTION_UPGRADED,
            Map.of(
                "oldTier", oldTier,
                "newTier", newTier,
                "status", "ACTIVE"
            )
        );

        return publishToKafka(subscriptionEventsTopic, subscriptionId.toString(), event,
                "subscription upgraded");
    }

    /**
     * Publish trial ending event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishTrialEnding(
            UUID subscriptionId, UUID userId, int daysRemaining) {

        SubscriptionEvent event = buildSubscriptionEvent(
            subscriptionId,
            userId,
            SubscriptionEventConstants.TRIAL_ENDING,
            Map.of(
                "daysRemaining", String.valueOf(daysRemaining),
                "status", "TRIAL"
            )
        );

        return publishToKafka(subscriptionEventsTopic, subscriptionId.toString(), event,
                "trial ending");
    }

    /**
     * Build subscription event with common fields
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private SubscriptionEvent buildSubscriptionEvent(
            UUID subscriptionId,
            UUID userId,
            String eventType,
            Map<String, String> metadata) {

        return new SubscriptionEvent(
            UUID.randomUUID(),
            eventType,
            subscriptionId,
            userId,
            metadata,
            LocalDateTime.now()
        );
    }

    /**
     * Subscription Event DTO
     */
    public record SubscriptionEvent(
        UUID eventId,
        String eventType,
        UUID subscriptionId,
        UUID userId,
        Map<String, String> metadata,
        LocalDateTime timestamp
    ) {}
}
