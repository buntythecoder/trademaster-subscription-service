package com.trademaster.subscription.event;

import com.trademaster.subscription.enums.SubscriptionTier;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Subscription Event - Backward Compatibility Facade
 * MANDATORY: Single Responsibility - Backward compatibility only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Facade providing backward compatibility for existing code.
 * New code should use BaseSubscriptionEvent, SubscriptionEventTypes, and SubscriptionEventFactory.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 * @deprecated Use {@link BaseSubscriptionEvent}, {@link SubscriptionEventTypes}, and {@link SubscriptionEventFactory} instead
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@SuperBuilder
@Deprecated(since = "2.0.0", forRemoval = false)
public class SubscriptionEvent extends BaseSubscriptionEvent {

    // Event Type Constants - Delegate to SubscriptionEventTypes
    public static final String SUBSCRIPTION_CREATED = SubscriptionEventTypes.SUBSCRIPTION_CREATED;
    public static final String SUBSCRIPTION_ACTIVATED = SubscriptionEventTypes.SUBSCRIPTION_ACTIVATED;
    public static final String SUBSCRIPTION_UPGRADED = SubscriptionEventTypes.SUBSCRIPTION_UPGRADED;
    public static final String SUBSCRIPTION_DOWNGRADED = SubscriptionEventTypes.SUBSCRIPTION_DOWNGRADED;
    public static final String SUBSCRIPTION_CANCELLED = SubscriptionEventTypes.SUBSCRIPTION_CANCELLED;
    public static final String SUBSCRIPTION_SUSPENDED = SubscriptionEventTypes.SUBSCRIPTION_SUSPENDED;
    public static final String SUBSCRIPTION_EXPIRED = SubscriptionEventTypes.SUBSCRIPTION_EXPIRED;
    public static final String SUBSCRIPTION_RENEWED = SubscriptionEventTypes.SUBSCRIPTION_RENEWED;
    public static final String SUBSCRIPTION_PAYMENT_FAILED = SubscriptionEventTypes.SUBSCRIPTION_PAYMENT_FAILED;
    public static final String SUBSCRIPTION_PAYMENT_RETRY = SubscriptionEventTypes.SUBSCRIPTION_PAYMENT_RETRY;
    public static final String USAGE_LIMIT_EXCEEDED = SubscriptionEventTypes.USAGE_LIMIT_EXCEEDED;
    public static final String USAGE_WARNING_THRESHOLD = SubscriptionEventTypes.USAGE_WARNING_THRESHOLD;
    public static final String TRIAL_STARTED = SubscriptionEventTypes.TRIAL_STARTED;
    public static final String TRIAL_ENDING_SOON = SubscriptionEventTypes.TRIAL_ENDING_SOON;
    public static final String TRIAL_EXPIRED = SubscriptionEventTypes.TRIAL_EXPIRED;

    // Factory Methods - Delegate to SubscriptionEventFactory
    public static SubscriptionEvent subscriptionCreated(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.subscriptionCreated(
            subscriptionId, userId, tier, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    public static SubscriptionEvent subscriptionActivated(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, UUID paymentTransactionId,
            String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.subscriptionActivated(
            subscriptionId, userId, tier, paymentTransactionId, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    public static SubscriptionEvent subscriptionUpgraded(
            UUID subscriptionId, UUID userId,
            SubscriptionTier newTier, SubscriptionTier oldTier,
            String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.subscriptionUpgraded(
            subscriptionId, userId, newTier, oldTier, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    public static SubscriptionEvent subscriptionCancelled(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, boolean immediate,
            String reason, String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.subscriptionCancelled(
            subscriptionId, userId, tier, immediate, reason, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    public static SubscriptionEvent usageLimitExceeded(
            UUID subscriptionId, UUID userId,
            String featureName, Long currentUsage,
            Long limit, String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.usageLimitExceeded(
            subscriptionId, userId, featureName, currentUsage, limit, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    public static SubscriptionEvent trialStarted(
            UUID subscriptionId, UUID userId,
            LocalDateTime trialEndDate, String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.trialStarted(
            subscriptionId, userId, trialEndDate, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    public static SubscriptionEvent paymentFailed(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, int failedAttempts,
            String errorMessage, String correlationId) {
        BaseSubscriptionEvent baseEvent = SubscriptionEventFactory.paymentFailed(
            subscriptionId, userId, tier, failedAttempts, errorMessage, correlationId);
        return convertToSubscriptionEvent(baseEvent);
    }

    private static SubscriptionEvent convertToSubscriptionEvent(BaseSubscriptionEvent base) {
        return SubscriptionEvent.builder()
            .eventId(base.getEventId())
            .eventType(base.getEventType())
            .timestamp(base.getTimestamp())
            .subscriptionId(base.getSubscriptionId())
            .userId(base.getUserId())
            .tier(base.getTier())
            .status(base.getStatus())
            .previousStatus(base.getPreviousStatus())
            .source(base.getSource())
            .version(base.getVersion())
            .metadata(base.getMetadata())
            .payload(base.getPayload())
            .correlationId(base.getCorrelationId())
            .causationId(base.getCausationId())
            .build();
    }
}
