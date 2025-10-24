package com.trademaster.subscription.event;

import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Base Subscription Event
 * MANDATORY: Single Responsibility - Event data structure only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Base class for all subscription-related events published to Kafka.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseSubscriptionEvent {

    /**
     * Event ID for tracking and deduplication
     */
    private String eventId;

    /**
     * Event type for routing and processing
     */
    private String eventType;

    /**
     * Event timestamp
     */
    private LocalDateTime timestamp;

    /**
     * Subscription ID
     */
    private UUID subscriptionId;

    /**
     * User ID
     */
    private UUID userId;

    /**
     * Subscription tier
     */
    private SubscriptionTier tier;

    /**
     * Subscription status
     */
    private SubscriptionStatus status;

    /**
     * Previous status (for status change events)
     */
    private SubscriptionStatus previousStatus;

    /**
     * Event source service
     */
    private String source;

    /**
     * Event version for schema evolution
     */
    private String version;

    /**
     * Additional event metadata
     */
    private Map<String, Object> metadata;

    /**
     * Event payload with specific data
     */
    private Map<String, Object> payload;

    /**
     * Correlation ID for tracking related events
     */
    private String correlationId;

    /**
     * Causation ID for tracking event chains
     */
    private String causationId;
}
