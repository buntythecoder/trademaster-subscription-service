package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.service.interfaces.ISubscriptionLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Lifecycle Service - Facade Pattern
 *
 * MANDATORY: Facade Pattern - Rule #4 (Design Patterns)
 * MANDATORY: Single Responsibility - Delegates to specialized services
 * MANDATORY: Interface Segregation - Maintains backward compatibility
 *
 * This facade delegates to specialized services:
 * - SubscriptionCreator: Creation operations
 * - SubscriptionActivator: Activation operations
 * - SubscriptionCancellationService: Cancellation operations
 * - SubscriptionSuspender: Suspension operations
 * - SubscriptionResumer: Resumption operations
 * - SubscriptionStateManager: Queries and health checks
 *
 * @author TradeMaster Development Team
 * @version 3.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleService implements ISubscriptionLifecycleService {

    private final SubscriptionCreator subscriptionCreator;
    private final SubscriptionActivator subscriptionActivator;
    private final SubscriptionCancellationService subscriptionCancellationService;
    private final SubscriptionSuspender subscriptionSuspender;
    private final SubscriptionResumer subscriptionResumer;
    private final SubscriptionStateManager subscriptionStateManager;

    /**
     * Create subscription - Delegates to SubscriptionCreator
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> createSubscription(
            UUID userId, SubscriptionTier tier, BillingCycle billingCycle, boolean startTrial) {
        return subscriptionCreator.createSubscription(userId, tier, billingCycle, startTrial);
    }

    /**
     * Activate subscription - Delegates to SubscriptionActivator
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> activateSubscription(
            UUID subscriptionId, UUID paymentTransactionId) {
        return subscriptionActivator.activateSubscription(subscriptionId, paymentTransactionId);
    }

    /**
     * Cancel subscription - Delegates to SubscriptionCancellationService
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> cancelSubscription(
            UUID subscriptionId, String cancellationReason) {
        return subscriptionCancellationService.cancelSubscription(subscriptionId, cancellationReason);
    }

    /**
     * Suspend subscription - Delegates to SubscriptionSuspender
     */
    @Override
    @Transactional
    public CompletableFuture<Result<Subscription, String>> suspendSubscription(
            UUID subscriptionId, String reason) {
        return subscriptionSuspender.suspendSubscription(subscriptionId, reason);
    }

    /**
     * Resume subscription - Delegates to SubscriptionResumer
     */
    @Transactional
    public CompletableFuture<Result<Subscription, String>> resumeSubscription(UUID subscriptionId) {
        return subscriptionResumer.resumeSubscription(subscriptionId);
    }

    /**
     * Get active subscription - Delegates to SubscriptionStateManager
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Optional<Subscription>, String>> getActiveSubscription(UUID userId) {
        return subscriptionStateManager.getActiveSubscription(userId);
    }

    /**
     * Find subscription by ID - Delegates to SubscriptionStateManager
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Optional<Subscription>, String>> findById(UUID subscriptionId) {
        return subscriptionStateManager.findById(subscriptionId);
    }

    /**
     * Get user subscriptions - Delegates to SubscriptionStateManager
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<Subscription>, String>> getUserSubscriptions(
            UUID userId, Pageable pageable) {
        return subscriptionStateManager.getUserSubscriptions(userId, pageable);
    }

    /**
     * Get subscriptions by status - Delegates to SubscriptionStateManager
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<Subscription>, String>> getSubscriptionsByStatus(
            SubscriptionStatus status, Pageable pageable) {
        return subscriptionStateManager.getSubscriptionsByStatus(status, pageable);
    }

    /**
     * Get subscription history - Delegates to SubscriptionStateManager
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<SubscriptionHistory>, String>> getSubscriptionHistory(
            UUID subscriptionId, Pageable pageable) {
        return subscriptionStateManager.getSubscriptionHistory(subscriptionId, pageable);
    }

    /**
     * Check subscription health - Delegates to SubscriptionStateManager
     */
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Result<String, String>> checkSubscriptionHealth(UUID subscriptionId) {
        return subscriptionStateManager.checkSubscriptionHealth(subscriptionId);
    }
}
