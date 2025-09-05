package com.trademaster.subscription.event;

import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Base Subscription Event
 * 
 * Base class for all subscription-related events published to Kafka.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionEvent {

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

    // Event Types
    public static final String SUBSCRIPTION_CREATED = "subscription.created";
    public static final String SUBSCRIPTION_ACTIVATED = "subscription.activated";
    public static final String SUBSCRIPTION_UPGRADED = "subscription.upgraded";
    public static final String SUBSCRIPTION_DOWNGRADED = "subscription.downgraded";
    public static final String SUBSCRIPTION_CANCELLED = "subscription.cancelled";
    public static final String SUBSCRIPTION_SUSPENDED = "subscription.suspended";
    public static final String SUBSCRIPTION_EXPIRED = "subscription.expired";
    public static final String SUBSCRIPTION_RENEWED = "subscription.renewed";
    public static final String SUBSCRIPTION_PAYMENT_FAILED = "subscription.payment.failed";
    public static final String SUBSCRIPTION_PAYMENT_RETRY = "subscription.payment.retry";
    public static final String USAGE_LIMIT_EXCEEDED = "subscription.usage.limit.exceeded";
    public static final String USAGE_WARNING_THRESHOLD = "subscription.usage.warning.threshold";
    public static final String TRIAL_STARTED = "subscription.trial.started";
    public static final String TRIAL_ENDING_SOON = "subscription.trial.ending.soon";
    public static final String TRIAL_EXPIRED = "subscription.trial.expired";

    /**
     * Create a subscription created event
     */
    public static SubscriptionEvent subscriptionCreated(UUID subscriptionId, UUID userId, 
                                                       SubscriptionTier tier, String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SUBSCRIPTION_CREATED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.PENDING)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "tier", tier.toString(),
                "createdAt", LocalDateTime.now().toString()
            ))
            .build();
    }

    /**
     * Create a subscription activated event
     */
    public static SubscriptionEvent subscriptionActivated(UUID subscriptionId, UUID userId, 
                                                         SubscriptionTier tier, UUID paymentTransactionId,
                                                         String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SUBSCRIPTION_ACTIVATED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.ACTIVE)
            .previousStatus(SubscriptionStatus.PENDING)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "tier", tier.toString(),
                "paymentTransactionId", paymentTransactionId.toString(),
                "activatedAt", LocalDateTime.now().toString()
            ))
            .build();
    }

    /**
     * Create a subscription upgraded event
     */
    public static SubscriptionEvent subscriptionUpgraded(UUID subscriptionId, UUID userId, 
                                                        SubscriptionTier newTier, SubscriptionTier oldTier,
                                                        String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SUBSCRIPTION_UPGRADED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(newTier)
            .status(SubscriptionStatus.ACTIVE)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "newTier", newTier.toString(),
                "oldTier", oldTier.toString(),
                "upgradedAt", LocalDateTime.now().toString()
            ))
            .build();
    }

    /**
     * Create a subscription cancelled event
     */
    public static SubscriptionEvent subscriptionCancelled(UUID subscriptionId, UUID userId, 
                                                         SubscriptionTier tier, boolean immediate,
                                                         String reason, String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SUBSCRIPTION_CANCELLED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.CANCELLED)
            .previousStatus(SubscriptionStatus.ACTIVE)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "tier", tier.toString(),
                "immediate", immediate,
                "reason", reason != null ? reason : "",
                "cancelledAt", LocalDateTime.now().toString()
            ))
            .build();
    }

    /**
     * Create a usage limit exceeded event
     */
    public static SubscriptionEvent usageLimitExceeded(UUID subscriptionId, UUID userId, 
                                                      String featureName, Long currentUsage, 
                                                      Long limit, String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(USAGE_LIMIT_EXCEEDED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "featureName", featureName,
                "currentUsage", currentUsage,
                "limit", limit,
                "exceededAt", LocalDateTime.now().toString()
            ))
            .build();
    }

    /**
     * Create a trial started event
     */
    public static SubscriptionEvent trialStarted(UUID subscriptionId, UUID userId, 
                                                LocalDateTime trialEndDate, String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(TRIAL_STARTED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(SubscriptionTier.PRO) // Assuming PRO trial
            .status(SubscriptionStatus.TRIAL)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "trialStartedAt", LocalDateTime.now().toString(),
                "trialEndsAt", trialEndDate.toString()
            ))
            .build();
    }

    /**
     * Create a payment failed event
     */
    public static SubscriptionEvent paymentFailed(UUID subscriptionId, UUID userId, 
                                                 SubscriptionTier tier, int failedAttempts,
                                                 String errorMessage, String correlationId) {
        return SubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SUBSCRIPTION_PAYMENT_FAILED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.PAYMENT_FAILED)
            .source("subscription-service")
            .version("1.0")
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "tier", tier.toString(),
                "failedAttempts", failedAttempts,
                "errorMessage", errorMessage,
                "failedAt", LocalDateTime.now().toString()
            ))
            .build();
    }
}