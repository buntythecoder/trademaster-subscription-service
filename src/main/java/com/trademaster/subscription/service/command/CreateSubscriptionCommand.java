package com.trademaster.subscription.service.command;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Command Pattern Implementation for Creating Subscriptions
 * 
 * MANDATORY: Command Pattern - Advanced Design Pattern Rule #4
 * MANDATORY: Single Responsibility - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@RequiredArgsConstructor
public class CreateSubscriptionCommand implements SubscriptionCommand<Subscription> {

    private final SubscriptionLifecycleService subscriptionLifecycleService;
    private final UUID userId;
    private final SubscriptionTier tier;
    private final BillingCycle billingCycle;
    private final boolean startTrial;

    @Override
    public CompletableFuture<Result<Subscription, String>> execute() {
        return validate()
            .match(
                success -> subscriptionLifecycleService.createSubscription(userId, tier, billingCycle, startTrial),
                failure -> CompletableFuture.completedFuture(Result.failure(failure))
            );
    }

    @Override
    public Result<Void, String> validate() {
        return switch (userId) {
            case null -> Result.failure("User ID cannot be null");
            case UUID id when tier == null -> Result.failure("Subscription tier cannot be null");
            case UUID id when billingCycle == null -> Result.failure("Billing cycle cannot be null");
            default -> Result.success(null);
        };
    }

    @Override
    public String getCommandName() {
        return "CreateSubscriptionCommand";
    }
}