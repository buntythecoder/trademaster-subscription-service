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
 * User Event Publisher
 * MANDATORY: Single Responsibility - User-related events only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Publishes user-related events (usage limits, user actions).
 * Extends BaseEventPublisher for shared Kafka infrastructure.
 *
 * @author TradeMaster Development Team
 */
@Component
public class UserEventPublisher extends BaseEventPublisher {

    @Value("${trademaster.kafka.topics.user-events:user-events}")
    private String userEventsTopic;

    public UserEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            CircuitBreaker kafkaCircuitBreaker) {
        super(kafkaTemplate, objectMapper, kafkaCircuitBreaker);
    }

    /**
     * Publish usage limit exceeded event
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public CompletableFuture<Result<Void, String>> publishUsageLimitExceeded(
            UUID subscriptionId, UUID userId, String featureName, String tier) {

        UserEvent event = buildUserEvent(
            userId,
            subscriptionId,
            SubscriptionEventConstants.USAGE_LIMIT_EXCEEDED,
            Map.of(
                "featureName", featureName,
                "tier", tier,
                "action", "LIMIT_ENFORCEMENT"
            )
        );

        return publishToKafka(userEventsTopic, userId.toString(), event,
                "usage limit exceeded");
    }

    /**
     * Build user event with common fields
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private UserEvent buildUserEvent(
            UUID userId,
            UUID subscriptionId,
            String eventType,
            Map<String, String> metadata) {

        return new UserEvent(
            UUID.randomUUID(),
            eventType,
            userId,
            subscriptionId,
            metadata,
            LocalDateTime.now()
        );
    }

    /**
     * User Event DTO
     */
    public record UserEvent(
        UUID eventId,
        String eventType,
        UUID userId,
        UUID subscriptionId,
        Map<String, String> metadata,
        LocalDateTime timestamp
    ) {}
}
