package com.trademaster.subscription.dto;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Subscription Response DTO
 * 
 * MANDATORY: Immutable Record - TradeMaster Rule #9
 * MANDATORY: Functional Programming - TradeMaster Rule #3
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriptionResponse(
    UUID id,
    UUID userId,
    SubscriptionTier tier,
    SubscriptionStatus status,
    BillingCycle billingCycle,
    BigDecimal monthlyPrice,
    BigDecimal billingAmount,
    String currency,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime nextBillingDate,
    LocalDateTime trialEndDate,
    Boolean autoRenewal,
    String promotionCode,
    BigDecimal promotionDiscount,
    List<String> features,
    SubscriptionLimitsResponse limits,
    BigDecimal monthlySavings,
    Long daysRemainingInCycle,
    Boolean isActive,
    Boolean isInTrial,
    Boolean canUpgrade,
    Boolean canCancel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * Factory method to create response from subscription entity
     */
    public static SubscriptionResponse fromSubscription(Subscription subscription) {
        return new SubscriptionResponse(
            subscription.getId(),
            subscription.getUserId(),
            subscription.getTier(),
            subscription.getStatus(),
            subscription.getBillingCycle(),
            subscription.getMonthlyPrice(),
            subscription.getBillingAmount(),
            subscription.getCurrency(),
            subscription.getStartDate(),
            subscription.getEndDate(),
            subscription.getNextBillingDate(),
            subscription.getTrialEndDate(),
            subscription.getAutoRenewal(),
            subscription.getPromotionCode(),
            subscription.getPromotionDiscount(),
            getFeaturesForTier(subscription.getTier()),
            createLimitsResponse(subscription.getTier()),
            calculateMonthlySavings(subscription),
            calculateDaysRemainingInCycle(subscription),
            isActive(subscription),
            isInTrial(subscription),
            canUpgrade(subscription),
            canCancel(subscription),
            subscription.getCreatedAt(),
            subscription.getUpdatedAt()
        );
    }
    
    // Helper methods using pattern matching
    private static List<String> getFeaturesForTier(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> List.of("Basic Analytics", "Limited API Access", "Email Support");
            case PRO -> List.of("Advanced Analytics", "Full API Access", "Data Export", "Priority Support");
            case AI_PREMIUM -> List.of("All PRO Features", "AI Insights", "Custom Algorithms", "Real-time Alerts");
            case INSTITUTIONAL -> List.of("All Features", "Unlimited Usage", "Dedicated Support", "Custom Integrations");
        };
    }
    
    private static SubscriptionLimitsResponse createLimitsResponse(SubscriptionTier tier) {
        return switch (tier) {
            case FREE -> new SubscriptionLimitsResponse(5, 10, 1000, 3, 0, 0, 0, 30, 1);
            case PRO -> new SubscriptionLimitsResponse(25, 100, 10000, 10, 0, 3, 5, 90, 5);
            case AI_PREMIUM -> new SubscriptionLimitsResponse(100, 500, 50000, 50, 1000, 10, 20, 365, 25);
            case INSTITUTIONAL -> new SubscriptionLimitsResponse(-1, -1, -1, -1, -1, -1, -1, -1, -1);
        };
    }
    
    private static BigDecimal calculateMonthlySavings(Subscription subscription) {
        return switch (subscription.getBillingCycle()) {
            case MONTHLY -> BigDecimal.ZERO;
            case QUARTERLY -> subscription.getMonthlyPrice().multiply(new BigDecimal("3"))
                .subtract(subscription.getBillingAmount()).divide(new BigDecimal("3"));
            case ANNUAL -> subscription.getMonthlyPrice().multiply(new BigDecimal("12"))
                .subtract(subscription.getBillingAmount()).divide(new BigDecimal("12"));
        };
    }
    
    private static Long calculateDaysRemainingInCycle(Subscription subscription) {
        return switch (subscription.getNextBillingDate()) {
            case null -> 0L;
            default -> ChronoUnit.DAYS.between(LocalDateTime.now(), subscription.getNextBillingDate());
        };
    }
    
    private static Boolean isActive(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case ACTIVE, TRIAL -> true;
            default -> false;
        };
    }
    
    private static Boolean isInTrial(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case TRIAL -> subscription.getTrialEndDate() != null && 
                         subscription.getTrialEndDate().isAfter(LocalDateTime.now());
            default -> false;
        };
    }
    
    private static Boolean canUpgrade(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case ACTIVE, TRIAL -> switch (subscription.getTier()) {
                case FREE, PRO, AI_PREMIUM -> true;
                case INSTITUTIONAL -> false;
            };
            default -> false;
        };
    }
    
    private static Boolean canCancel(Subscription subscription) {
        return switch (subscription.getStatus()) {
            case ACTIVE, TRIAL, SUSPENDED -> true;
            default -> false;
        };
    }
    
    /**
     * Nested record for subscription limits
     */
    public record SubscriptionLimitsResponse(
        Integer maxWatchlists,
        Integer maxAlerts,
        Integer apiCallsPerDay,
        Integer maxPortfolios,
        Integer aiAnalysisPerMonth,
        Integer maxSubAccounts,
        Integer maxCustomIndicators,
        Integer dataRetentionDays,
        Integer maxWebSocketConnections
    ) {
        /**
         * Check if a feature is unlimited using pattern matching
         */
        public boolean isUnlimited(String feature) {
            return switch (feature.toLowerCase()) {
                case "watchlists" -> maxWatchlists == -1;
                case "alerts" -> maxAlerts == -1;
                case "api_calls" -> apiCallsPerDay == -1;
                case "portfolios" -> maxPortfolios == -1;
                case "ai_analysis" -> aiAnalysisPerMonth == -1;
                case "sub_accounts" -> maxSubAccounts == -1;
                case "custom_indicators" -> maxCustomIndicators == -1;
                case "data_retention" -> dataRetentionDays == -1;
                case "websocket_connections" -> maxWebSocketConnections == -1;
                default -> false;
            };
        }
    }
}