package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import com.trademaster.subscription.service.base.BaseUsageService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Usage Tracker
 * MANDATORY: Single Responsibility - Handles usage tracking operations only
 * MANDATORY: Functional Programming - Railway pattern
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class UsageTracker extends BaseUsageService {

    public UsageTracker(SubscriptionRepository subscriptionRepository,
                       UsageTrackingRepository usageTrackingRepository,
                       SubscriptionMetricsService metricsService,
                       StructuredLoggingService loggingService,
                       CircuitBreaker databaseCircuitBreaker,
                       Retry databaseRetry) {
        super(subscriptionRepository, usageTrackingRepository, metricsService,
              loggingService, databaseCircuitBreaker, databaseRetry);
    }

    @Transactional(readOnly = true)
    public CompletableFuture<Result<Boolean, String>> canUseFeature(UUID subId, String feature) {
        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return initCheckCtx(subId, feature, corrId)
                .flatMap(this::findSub)
                .flatMap(this::checkLimit)
                .map(CheckCtx::canUse)
                .onSuccess(can -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "can_use_feature");
                    log.debug("Usage check: sub={}, feature={}, can={}", subId, feature, can);
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "can_use_feature_failed");
                    log.error("Usage check failed: sub={}, error={}", subId, e);
                });
        }, getVirtualThreadExecutor());
    }

    @Transactional
    public CompletableFuture<Result<UsageTracking, String>> incrementUsage(
            UUID subId, String feature, int inc) {

        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return initIncCtx(subId, feature, inc, corrId)
                .flatMap(this::findSubForInc)
                .flatMap(this::validateInc)
                .flatMap(this::applyInc)
                .flatMap(this::save)
                .onSuccess(u -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "increment_usage");
                    log.debug("Usage incremented: sub={}, feature={}, inc={}", subId, feature, inc);
                })
                .onFailure(e -> {
                    metricsService.recordSubscriptionProcessingTime(timer, "increment_usage_failed");
                    log.error("Increment failed: sub={}, error={}", subId, e);
                });
        }, getVirtualThreadExecutor());
    }

    @Transactional
    public CompletableFuture<Result<List<UsageTracking>, String>> resetUsage(UUID subId) {
        return CompletableFuture.supplyAsync(() -> {
            String corrId = UUID.randomUUID().toString();
            var timer = metricsService.startSubscriptionProcessingTimer();

            return executeWithResilience(() ->
                Result.tryExecute(() -> {
                    List<UsageTracking> records = usageTrackingRepository.findBySubscriptionId(subId);
                    List<UsageTracking> reset = records.stream()
                        .map(u -> {
                            u.setUsageCount(0L);
                            u.setLastResetDate(LocalDateTime.now());
                            return u;
                        })
                        .toList();
                    return usageTrackingRepository.saveAll(reset);
                }).mapError(ex -> "Failed to reset: " + ex.getMessage())
            ).onSuccess(r -> {
                metricsService.recordSubscriptionProcessingTime(timer, "reset_usage");
                log.info("Reset {} features for sub: {}", r.size(), subId);
            }).onFailure(e -> {
                metricsService.recordSubscriptionProcessingTime(timer, "reset_usage_failed");
                log.error("Reset failed: sub={}, error={}", subId, e);
            });
        }, getVirtualThreadExecutor());
    }

    private Result<CheckCtx, String> initCheckCtx(UUID subId, String feature, String corrId) {
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(corrId);
            return new CheckCtx(corrId, subId, feature, null, false);
        }).mapError(ex -> "Failed to init check context: " + ex.getMessage());
    }

    private Result<IncCtx, String> initIncCtx(UUID subId, String feature, int inc, String corrId) {
        return Result.tryExecute(() -> {
            loggingService.setCorrelationId(corrId);
            return new IncCtx(corrId, subId, feature, inc, null, null);
        }).mapError(ex -> "Failed to init inc context: " + ex.getMessage());
    }

    private Result<CheckCtx, String> findSub(CheckCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.findById(ctx.subId()))
                .mapError(Exception::getMessage)
                .flatMap(opt -> opt.map(s -> Result.<CheckCtx, String>success(
                    new CheckCtx(ctx.corrId(), ctx.subId(), ctx.feature(), s, false)))
                    .orElse(Result.failure("Subscription not found: " + ctx.subId())))
        );
    }

    private Result<IncCtx, String> findSubForInc(IncCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> subscriptionRepository.findById(ctx.subId()))
                .mapError(Exception::getMessage)
                .flatMap(opt -> opt.map(s -> Result.<IncCtx, String>success(
                    new IncCtx(ctx.corrId(), ctx.subId(), ctx.feature(), ctx.inc(), s, null)))
                    .orElse(Result.failure("Subscription not found: " + ctx.subId())))
        );
    }

    private Result<CheckCtx, String> checkLimit(CheckCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> {
                UsageTracking u = usageTrackingRepository
                    .findBySubscriptionIdAndFeature(ctx.subId(), ctx.feature())
                    .orElse(createDefaultUsage(ctx.subId(), ctx.feature(), ctx.sub().getTier()));
                boolean can = !hasExceededLimit(u);
                return new CheckCtx(ctx.corrId(), ctx.subId(), ctx.feature(), ctx.sub(), can);
            }).mapError(ex -> "Failed to check limit: " + ex.getMessage())
        );
    }

    private Result<IncCtx, String> validateInc(IncCtx ctx) {
        return executeWithResilience(() -> {
            try {
                UsageTracking u = usageTrackingRepository
                    .findBySubscriptionIdAndFeature(ctx.subId(), ctx.feature())
                    .orElse(createDefaultUsage(ctx.subId(), ctx.feature(), ctx.sub().getTier()));

                return wouldExceedLimit(u, ctx.inc())
                    ? Result.<IncCtx, String>failure("Would exceed limit: " + ctx.feature())
                    : Result.success(new IncCtx(ctx.corrId(), ctx.subId(), ctx.feature(),
                                                ctx.inc(), ctx.sub(), u));
            } catch (Exception ex) {
                return Result.<IncCtx, String>failure("Validation failed: " + ex.getMessage());
            }
        });
    }

    private Result<IncCtx, String> applyInc(IncCtx ctx) {
        return Result.tryExecute(() -> {
            UsageTracking u = ctx.usage();
            u.setUsageCount(u.getUsageCount() + ctx.inc());
            u.setLastUsedDate(LocalDateTime.now());
            return new IncCtx(ctx.corrId(), ctx.subId(), ctx.feature(), ctx.inc(), ctx.sub(), u);
        }).mapError(ex -> "Failed to apply increment: " + ex.getMessage());
    }

    private Result<UsageTracking, String> save(IncCtx ctx) {
        return executeWithResilience(() ->
            Result.tryExecute(() -> usageTrackingRepository.save(ctx.usage()))
                .mapError(ex -> "Failed to save: " + ex.getMessage())
        );
    }

    private record CheckCtx(String corrId, UUID subId, String feature,
                           Subscription sub, boolean canUse) {}

    private record IncCtx(String corrId, UUID subId, String feature, int inc,
                         Subscription sub, UsageTracking usage) {}
}
