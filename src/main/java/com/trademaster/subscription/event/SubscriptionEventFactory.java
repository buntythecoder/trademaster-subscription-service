package com.trademaster.subscription.event;

import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription Event Factory
 * MANDATORY: Single Responsibility - Event creation only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #4 - Factory pattern
 *
 * Factory for creating subscription event instances.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
public final class SubscriptionEventFactory {

    private SubscriptionEventFactory() {
        throw new UnsupportedOperationException("Factory class cannot be instantiated");
    }

    private static final String SERVICE_SOURCE = "subscription-service";
    private static final String EVENT_VERSION = "1.0";

    public static BaseSubscriptionEvent subscriptionCreated(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.SUBSCRIPTION_CREATED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.PENDING)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "tier", tier.toString(),
                "createdAt", LocalDateTime.now().toString()
            ))
            .build();
    }

    public static BaseSubscriptionEvent subscriptionActivated(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, UUID paymentTransactionId,
            String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.SUBSCRIPTION_ACTIVATED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.ACTIVE)
            .previousStatus(SubscriptionStatus.PENDING)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
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

    public static BaseSubscriptionEvent subscriptionUpgraded(
            UUID subscriptionId, UUID userId,
            SubscriptionTier newTier, SubscriptionTier oldTier,
            String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.SUBSCRIPTION_UPGRADED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(newTier)
            .status(SubscriptionStatus.ACTIVE)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
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

    public static BaseSubscriptionEvent subscriptionCancelled(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, boolean immediate,
            String reason, String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.SUBSCRIPTION_CANCELLED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.CANCELLED)
            .previousStatus(SubscriptionStatus.ACTIVE)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
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

    public static BaseSubscriptionEvent usageLimitExceeded(
            UUID subscriptionId, UUID userId,
            String featureName, Long currentUsage,
            Long limit, String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.USAGE_LIMIT_EXCEEDED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
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

    public static BaseSubscriptionEvent trialStarted(
            UUID subscriptionId, UUID userId,
            LocalDateTime trialEndDate, String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.TRIAL_STARTED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.TRIAL)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
            .correlationId(correlationId)
            .payload(Map.of(
                "subscriptionId", subscriptionId.toString(),
                "userId", userId.toString(),
                "trialStartedAt", LocalDateTime.now().toString(),
                "trialEndsAt", trialEndDate.toString()
            ))
            .build();
    }

    public static BaseSubscriptionEvent paymentFailed(
            UUID subscriptionId, UUID userId,
            SubscriptionTier tier, int failedAttempts,
            String errorMessage, String correlationId) {

        return BaseSubscriptionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(SubscriptionEventTypes.SUBSCRIPTION_PAYMENT_FAILED)
            .timestamp(LocalDateTime.now())
            .subscriptionId(subscriptionId)
            .userId(userId)
            .tier(tier)
            .status(SubscriptionStatus.PAYMENT_FAILED)
            .source(SERVICE_SOURCE)
            .version(EVENT_VERSION)
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
