package com.trademaster.subscription.controller.base;

import com.trademaster.subscription.dto.SubscriptionResponse;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Base Subscription Controller
 * MANDATORY: DRY Principle - Shared controller infrastructure
 * MANDATORY: Single Responsibility - Common response handling only
 *
 * Provides common response handling and error mapping for subscription controllers.
 *
 * @author TradeMaster Development Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class BaseSubscriptionController {

    /**
     * Convert subscription to response entity
     */
    protected ResponseEntity<SubscriptionResponse> toResponse(Subscription subscription) {
        return ResponseEntity.ok(SubscriptionResponse.fromSubscription(subscription));
    }

    /**
     * Convert optional subscription to response entity
     */
    protected ResponseEntity<SubscriptionResponse> toResponse(Optional<Subscription> subscriptionOpt) {
        return subscriptionOpt
            .map(this::toResponse)
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Handle result with standard error mapping
     */
    protected ResponseEntity<SubscriptionResponse> handleResult(
            Result<Subscription, String> result) {

        return result.match(
            this::toResponse,
            error -> mapErrorToResponse(error)
        );
    }

    /**
     * Handle optional result with standard error mapping
     */
    protected ResponseEntity<SubscriptionResponse> handleOptionalResult(
            Result<Optional<Subscription>, String> result) {

        return result.match(
            this::toResponse,
            error -> mapErrorToResponse(error)
        );
    }

    /**
     * Map error message to appropriate HTTP response
     */
    private ResponseEntity<SubscriptionResponse> mapErrorToResponse(String error) {
        return switch (error) {
            case String msg when msg.contains("already has an active subscription") ->
                ResponseEntity.status(HttpStatus.CONFLICT).build();
            case String msg when msg.contains("validation failed") ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            case String msg when msg.contains("not found") ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            case String msg when msg.contains("unauthorized") ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            default ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        };
    }

    /**
     * Get virtual thread executor for async operations
     */
    protected CompletableFuture<ResponseEntity<SubscriptionResponse>> executeAsync(
            CompletableFuture<Result<Subscription, String>> operation) {

        return operation.thenApply(this::handleResult);
    }

    /**
     * Get virtual thread executor for optional async operations
     */
    protected CompletableFuture<ResponseEntity<SubscriptionResponse>> executeAsyncOptional(
            CompletableFuture<Result<Optional<Subscription>, String>> operation) {

        return operation.thenApply(this::handleOptionalResult);
    }
}
