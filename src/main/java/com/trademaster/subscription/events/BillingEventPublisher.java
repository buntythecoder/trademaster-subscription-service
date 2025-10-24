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
 * Billing Event Publisher
 * MANDATORY: Single Responsibility - Billing events only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Publishes billing-related events (payment failures, billing updates).
 * Extends BaseEventPublisher for shared Kafka infrastructure.
 *
 * @author TradeMaster Development Team
 */
@Component
public class BillingEventPublisher extends BaseEventPublisher {

    @Value("${trademaster.kafka.topics.billing-events:billing-events}")
    private String billingEventsTopic;

    public BillingEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            CircuitBreaker kafkaCircuitBreaker) {
        super(kafkaTemplate, objectMapper, kafkaCircuitBreaker);
    }

    /**
     * Publish billing failure event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishBillingFailure(
            UUID subscriptionId, UUID userId, String tier, int failedAttempts) {

        BillingEvent event = buildBillingEvent(
            subscriptionId,
            userId,
            SubscriptionEventConstants.BILLING_FAILED,
            Map.of(
                "tier", tier,
                "failedAttempts", String.valueOf(failedAttempts),
                "status", "PAYMENT_FAILED"
            )
        );

        return publishToKafka(billingEventsTopic, subscriptionId.toString(), event,
                "billing failure");
    }

    /**
     * Build billing event with common fields
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private BillingEvent buildBillingEvent(
            UUID subscriptionId,
            UUID userId,
            String eventType,
            Map<String, String> metadata) {

        return new BillingEvent(
            UUID.randomUUID(),
            eventType,
            subscriptionId,
            userId,
            metadata,
            LocalDateTime.now()
        );
    }

    /**
     * Billing Event DTO
     */
    public record BillingEvent(
        UUID eventId,
        String eventType,
        UUID subscriptionId,
        UUID userId,
        Map<String, String> metadata,
        LocalDateTime timestamp
    ) {}
}
