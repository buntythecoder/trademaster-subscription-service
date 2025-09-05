package com.trademaster.subscription.integration;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Notification Service Integration Client
 * 
 * MANDATORY: Zero Trust Security - External Service Access
 * MANDATORY: Circuit Breaker Pattern for Resilience
 * MANDATORY: Functional Programming Patterns
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker notificationServiceCircuitBreaker;
    private final Retry notificationServiceRetry;
    
    @Value("${trademaster.services.notification.base-url:http://localhost:8084}")
    private String notificationServiceBaseUrl;
    
    /**
     * Send subscription activation notification
     */
    public CompletableFuture<Result<NotificationResponse, String>> sendSubscriptionActivated(
            UUID userId, String subscriptionTier, String planName) {
        
        NotificationRequest request = new NotificationRequest(
            userId,
            "SUBSCRIPTION_ACTIVATED",
            "Your TradeMaster subscription has been activated",
            "Congratulations! Your " + planName + " subscription is now active.",
            Map.of(
                "subscriptionTier", subscriptionTier,
                "planName", planName,
                "template", "subscription_activated"
            )
        );
        
        return sendNotification(request);
    }
    
    /**
     * Send subscription cancelled notification
     */
    public CompletableFuture<Result<NotificationResponse, String>> sendSubscriptionCancelled(
            UUID userId, String subscriptionTier, String cancellationReason) {
        
        NotificationRequest request = new NotificationRequest(
            userId,
            "SUBSCRIPTION_CANCELLED",
            "Your TradeMaster subscription has been cancelled",
            "Your subscription has been cancelled. " + 
            (cancellationReason != null ? "Reason: " + cancellationReason : ""),
            Map.of(
                "subscriptionTier", subscriptionTier,
                "cancellationReason", cancellationReason != null ? cancellationReason : "",
                "template", "subscription_cancelled"
            )
        );
        
        return sendNotification(request);
    }
    
    /**
     * Send billing failure notification
     */
    public CompletableFuture<Result<NotificationResponse, String>> sendBillingFailure(
            UUID userId, String subscriptionTier, int failedAttempts, String nextRetryDate) {
        
        NotificationRequest request = new NotificationRequest(
            userId,
            "BILLING_FAILED",
            "Payment failed for your TradeMaster subscription",
            "We couldn't process your payment. Please update your payment method to continue your subscription.",
            Map.of(
                "subscriptionTier", subscriptionTier,
                "failedAttempts", String.valueOf(failedAttempts),
                "nextRetryDate", nextRetryDate,
                "template", "billing_failure"
            )
        );
        
        return sendNotification(request);
    }
    
    /**
     * Send subscription upgrade notification
     */
    public CompletableFuture<Result<NotificationResponse, String>> sendSubscriptionUpgraded(
            UUID userId, String oldTier, String newTier) {
        
        NotificationRequest request = new NotificationRequest(
            userId,
            "SUBSCRIPTION_UPGRADED",
            "Your TradeMaster subscription has been upgraded",
            "Great news! Your subscription has been upgraded from " + oldTier + " to " + newTier + ".",
            Map.of(
                "oldTier", oldTier,
                "newTier", newTier,
                "template", "subscription_upgraded"
            )
        );
        
        return sendNotification(request);
    }
    
    /**
     * Send trial ending notification
     */
    public CompletableFuture<Result<NotificationResponse, String>> sendTrialEnding(
            UUID userId, String trialEndDate, int daysRemaining) {
        
        NotificationRequest request = new NotificationRequest(
            userId,
            "TRIAL_ENDING",
            "Your TradeMaster trial is ending soon",
            "Your free trial ends in " + daysRemaining + " days. Upgrade now to continue using all features.",
            Map.of(
                "trialEndDate", trialEndDate,
                "daysRemaining", String.valueOf(daysRemaining),
                "template", "trial_ending"
            )
        );
        
        return sendNotification(request);
    }
    
    /**
     * Send usage limit reached notification
     */
    public CompletableFuture<Result<NotificationResponse, String>> sendUsageLimitReached(
            UUID userId, String featureName, String subscriptionTier) {
        
        NotificationRequest request = new NotificationRequest(
            userId,
            "USAGE_LIMIT_REACHED",
            "Usage limit reached for " + featureName,
            "You've reached your monthly limit for " + featureName + ". Consider upgrading for unlimited access.",
            Map.of(
                "featureName", featureName,
                "subscriptionTier", subscriptionTier,
                "template", "usage_limit_reached"
            )
        );
        
        return sendNotification(request);
    }
    
    /**
     * Generic notification sender
     */
    private CompletableFuture<Result<NotificationResponse, String>> sendNotification(NotificationRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.info("Sending {} notification to user: {}", request.type(), request.userId());
                    
                    String url = notificationServiceBaseUrl + "/api/v1/notifications/send";
                    NotificationResponse response = restTemplate.postForObject(url, request, NotificationResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("Notification sending failed for user: {}, type: {}", request.userId(), request.type(), e);
                    return Result.<NotificationResponse, String>failure("Notification sending failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        Supplier<Result<T, String>> decoratedSupplier = Retry.decorateSupplier(notificationServiceRetry, operation);
        return notificationServiceCircuitBreaker.executeSupplier(decoratedSupplier);
    }
    
    // DTOs for Notification Service Integration
    
    public record NotificationRequest(
        UUID userId,
        String type,
        String title,
        String message,
        Map<String, String> metadata
    ) {}
    
    public record NotificationResponse(
        UUID notificationId,
        String status,
        String deliveryMethod
    ) {}
}