package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.base.BaseSubscriptionService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription State Manager
 *
 * MANDATORY: Single Responsibility - Rule #2
 * MANDATORY: Focused on subscription queries, status checks, and history management
 * MANDATORY: Functional Programming - Rule #3 (no if-else, pattern matching)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class SubscriptionStateManager extends BaseSubscriptionService {

    private static final List<SubscriptionStatus> ACTIVE_STATUSES = List.of(
        SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIAL, SubscriptionStatus.EXPIRED
    );

    public SubscriptionStateManager(
            SubscriptionRepository subscriptionRepository,
            SubscriptionHistoryRepository historyRepository,
            SubscriptionMetricsService metricsService,
            StructuredLoggingService loggingService,
            ApplicationEventPublisher eventPublisher,
            CircuitBreaker databaseCircuitBreaker,
            Retry databaseRetry) {
        super(subscriptionRepository, historyRepository, metricsService,
              loggingService, eventPublisher, databaseCircuitBreaker, databaseRetry);
    }

    /**
     * Find subscription by ID
     * MANDATORY: Functional Programming - CompletableFuture with virtual threads
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Optional<Subscription>, String>> findById(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> subscriptionRepository.findById(subscriptionId))
                    .mapError(exception -> "Failed to find subscription: " + exception.getMessage())),
            getVirtualThreadExecutor()
        );
    }

    /**
     * Get active subscription for user
     * MANDATORY: Functional Programming - Stream-based query processing
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Optional<Subscription>, String>> getActiveSubscription(UUID userId) {
        return CompletableFuture.supplyAsync(() ->
            Result.tryExecute(() -> subscriptionRepository.findActiveByUserId(userId, ACTIVE_STATUSES))
                .mapError(exception -> "Failed to retrieve active subscription: " + exception.getMessage()),
            getVirtualThreadExecutor()
        );
    }

    /**
     * Get user subscriptions with pagination
     * MANDATORY: Functional Programming - Stream processing for pagination
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<Subscription>, String>> getUserSubscriptions(
            UUID userId, Pageable pageable) {

        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> {
                    List<Subscription> userSubscriptions =
                        subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
                    return createPageFromList(userSubscriptions, pageable);
                }).mapError(exception -> "Failed to get user subscriptions: " + exception.getMessage())),
            getVirtualThreadExecutor()
        );
    }

    /**
     * Get subscriptions by status with pagination
     * MANDATORY: Functional Programming - Stream processing
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<Page<Subscription>, String>> getSubscriptionsByStatus(
            SubscriptionStatus status, Pageable pageable) {

        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> {
                    List<Subscription> statusSubscriptions = subscriptionRepository.findByStatus(status);
                    return createPageFromList(statusSubscriptions, pageable);
                }).mapError(exception -> "Failed to get subscriptions by status: " + exception.getMessage())),
            getVirtualThreadExecutor()
        );
    }

    /**
     * Get subscription history
     * MANDATORY: Functional Programming - Railway pattern
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<List<SubscriptionHistory>, String>> getSubscriptionHistory(
            UUID subscriptionId, Pageable pageable) {

        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> {
                    // MANDATORY: Functional Programming - Rule #3 (NO if-else)
                    Optional.of(subscriptionRepository.existsById(subscriptionId))
                        .filter(exists -> exists)
                        .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

                    return historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
                }).mapError(exception -> "Failed to get subscription history: " + exception.getMessage())),
            getVirtualThreadExecutor()
        );
    }

    /**
     * Check subscription health
     * MANDATORY: Pattern Matching - Rule #14
     */
    @Transactional(readOnly = true)
    public CompletableFuture<Result<String, String>> checkSubscriptionHealth(UUID subscriptionId) {
        return CompletableFuture.supplyAsync(() ->
            executeWithResilience(() ->
                Result.tryExecute(() -> {
                    Subscription subscription = subscriptionRepository.findById(subscriptionId)
                        .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

                    // MANDATORY: Pattern Matching - Rule #14 (NO if-else)
                    return switch (subscription.getStatus()) {
                        case ACTIVE, TRIAL -> "healthy";
                        case SUSPENDED, EXPIRED, CANCELLED -> "unhealthy";
                        default -> "unhealthy";
                    };
                }).mapError(exception -> "Failed to check subscription health: " + exception.getMessage())),
            getVirtualThreadExecutor()
        );
    }

    // Helper methods

    /**
     * Create page from list with pagination
     * MANDATORY: Functional Programming - Stream processing
     */
    private Page<Subscription> createPageFromList(List<Subscription> subscriptions, Pageable pageable) {
        int start = Math.toIntExact(pageable.getOffset());
        int end = Math.min(start + pageable.getPageSize(), subscriptions.size());

        List<Subscription> pageContent = start < subscriptions.size()
            ? subscriptions.subList(start, end)
            : List.of();

        return new PageImpl<>(pageContent, pageable, subscriptions.size());
    }
}
