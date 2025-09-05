package com.trademaster.subscription.service.factory;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.service.SubscriptionNotificationService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory Pattern Implementation for Subscription Events
 * 
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Factory Pattern - Advanced Design Pattern Rule #4
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Component
public class SubscriptionEventFactory {

    /**
     * Create subscription event based on event type using pattern matching
     */
    public SubscriptionNotificationService.SubscriptionEvent createSubscriptionEvent(
            EventCreationContext context) {
        
        return switch (context.eventType()) {
            case SUBSCRIPTION_CREATED -> createSubscriptionCreatedEvent(context);
            case SUBSCRIPTION_ACTIVATED -> createSubscriptionActivatedEvent(context);
            case SUBSCRIPTION_EXPIRED -> createSubscriptionExpiredEvent(context);
            case SUBSCRIPTION_SUSPENDED -> createSubscriptionSuspendedEvent(context);
            default -> throw new IllegalArgumentException("Unknown event type: " + context.eventType());
        };
    }
    
    /**
     * Create subscription upgrade event with previous tier information
     */
    public SubscriptionNotificationService.SubscriptionUpgradeEvent createSubscriptionUpgradeEvent(
            UpgradeEventContext context) {
        
        return new SubscriptionNotificationService.SubscriptionUpgradeEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            context.previousTier(),
            context.subscription().getTier(),
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    /**
     * Create subscription cancellation event with reason
     */
    public SubscriptionNotificationService.SubscriptionCancellationEvent createSubscriptionCancellationEvent(
            CancellationEventContext context) {
        
        return new SubscriptionNotificationService.SubscriptionCancellationEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            context.cancellationReason(),
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    /**
     * Create subscription billing event with transaction information
     */
    public SubscriptionNotificationService.SubscriptionBillingEvent createSubscriptionBillingEvent(
            BillingEventContext context) {
        
        return new SubscriptionNotificationService.SubscriptionBillingEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            context.transactionId(),
            context.billingAmount(),
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    // Private factory methods using functional patterns
    
    private SubscriptionNotificationService.SubscriptionEvent createSubscriptionCreatedEvent(
            EventCreationContext context) {
        
        return new SubscriptionNotificationService.SubscriptionEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            SubscriptionNotificationService.SubscriptionEventType.SUBSCRIPTION_CREATED,
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    private SubscriptionNotificationService.SubscriptionEvent createSubscriptionActivatedEvent(
            EventCreationContext context) {
        
        return new SubscriptionNotificationService.SubscriptionEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            SubscriptionNotificationService.SubscriptionEventType.SUBSCRIPTION_ACTIVATED,
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    private SubscriptionNotificationService.SubscriptionEvent createSubscriptionExpiredEvent(
            EventCreationContext context) {
        
        return new SubscriptionNotificationService.SubscriptionEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            SubscriptionNotificationService.SubscriptionEventType.SUBSCRIPTION_EXPIRED,
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    private SubscriptionNotificationService.SubscriptionEvent createSubscriptionSuspendedEvent(
            EventCreationContext context) {
        
        return new SubscriptionNotificationService.SubscriptionEvent(
            context.subscription().getId(),
            context.subscription().getUserId(),
            SubscriptionNotificationService.SubscriptionEventType.SUBSCRIPTION_SUSPENDED,
            LocalDateTime.now(),
            context.correlationId()
        );
    }
    
    // Context Records for Factory Operations
    public record EventCreationContext(
        Subscription subscription,
        SubscriptionNotificationService.SubscriptionEventType eventType,
        String correlationId
    ) {}
    
    public record UpgradeEventContext(
        Subscription subscription,
        SubscriptionTier previousTier,
        String correlationId
    ) {}
    
    public record CancellationEventContext(
        Subscription subscription,
        String cancellationReason,
        String correlationId
    ) {}
    
    public record BillingEventContext(
        Subscription subscription,
        UUID transactionId,
        BigDecimal billingAmount,
        String correlationId
    ) {}
}