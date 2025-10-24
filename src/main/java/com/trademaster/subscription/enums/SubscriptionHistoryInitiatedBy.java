package com.trademaster.subscription.enums;

/**
 * Subscription History Initiated By
 * MANDATORY: Single Responsibility - Initiator enumeration only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Indicates who or what initiated a subscription change.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum SubscriptionHistoryInitiatedBy {
    USER("User Action"),
    SYSTEM("System Automated"),
    ADMIN("Administrator"),
    PAYMENT_GATEWAY("Payment Gateway"),
    SCHEDULED_TASK("Scheduled Task");

    private final String description;

    SubscriptionHistoryInitiatedBy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
