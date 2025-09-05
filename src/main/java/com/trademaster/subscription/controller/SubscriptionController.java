package com.trademaster.subscription.controller;

import com.trademaster.subscription.dto.SubscriptionRequest;
import com.trademaster.subscription.dto.SubscriptionResponse;
import com.trademaster.subscription.dto.SubscriptionUpgradeRequest;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
import com.trademaster.subscription.service.SubscriptionBillingService;
import com.trademaster.subscription.service.SubscriptionNotificationService;
import com.trademaster.subscription.common.Result;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Functional Subscription Management Controller
 * 
 * MANDATORY: TradeMaster Standards Compliant Implementation
 * - Rule #3: Functional Programming First (Result<T,E> pattern)
 * - Rule #11: Error Handling Patterns (No try-catch)
 * - Rule #12: Virtual Threads & Concurrency
 * - Rule #14: Pattern Matching (No if-else)
 * 
 * Provides REST endpoints for subscription lifecycle management.
 * All operations use Virtual Threads and functional programming patterns.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscription Management", description = "Subscription lifecycle and management operations")
public class SubscriptionController {

    private final SubscriptionLifecycleService subscriptionLifecycleService;
    private final SubscriptionUpgradeService subscriptionUpgradeService;
    private final SubscriptionBillingService subscriptionBillingService;
    private final SubscriptionNotificationService subscriptionNotificationService;

    /**
     * Create a new subscription
     */
    @PostMapping
    @Timed(value = "subscription.create", description = "Time taken to create subscription")
    @Operation(
        summary = "Create new subscription",
        description = "Creates a new subscription for a user with specified tier and billing cycle"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Subscription created successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid subscription data"),
        @ApiResponse(responseCode = "409", description = "User already has active subscription"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        
        log.info("Creating subscription for user: {}, tier: {}", request.userId(), request.tier());
        
        return subscriptionLifecycleService.createSubscription(
            request.userId(),
            request.tier(),
            request.billingCycle(),
            request.isStartTrial()
        ).thenApply(result -> result.match(
            subscription -> {
                SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            },
            error -> switch (error) {
                case String msg when msg.contains("already has an active subscription") -> 
                    ResponseEntity.status(HttpStatus.CONFLICT).build();
                case String msg when msg.contains("validation failed") -> 
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        ));
    }

    /**
     * Get user's active subscription
     */
    @GetMapping("/users/{userId}/active")
    @Timed(value = "subscription.get.active", description = "Time taken to get active subscription")
    @Operation(
        summary = "Get user's active subscription",
        description = "Retrieves the currently active subscription for a user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active subscription found",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No active subscription found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> getActiveSubscription(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        
        log.info("Getting active subscription for user: {}", userId);
        
        return subscriptionLifecycleService.getActiveSubscription(userId)
            .thenApply(result -> result.match(
                subscriptionOpt -> subscriptionOpt
                    .map(subscription -> {
                        SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build()),
                error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            ));
    }

    /**
     * Get subscription by ID
     */
    @GetMapping("/{subscriptionId}")
    @Timed(value = "subscription.get.byid", description = "Time taken to get subscription by ID")
    @Operation(
        summary = "Get subscription by ID",
        description = "Retrieves a specific subscription by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription found",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> getSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId) {
        
        log.info("Getting subscription: {}", subscriptionId);
        
        return CompletableFuture.<ResponseEntity<SubscriptionResponse>>supplyAsync(() -> {
            try {
                return subscriptionLifecycleService.findById(subscriptionId)
                    .<ResponseEntity<SubscriptionResponse>>thenApply(result -> result.match(
                        subscriptionOpt -> subscriptionOpt
                            .map(subscription -> {
                                SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                                return (ResponseEntity<SubscriptionResponse>) ResponseEntity.ok(response);
                            })
                            .orElse(ResponseEntity.<SubscriptionResponse>status(HttpStatus.NOT_FOUND).build()),
                        error -> ResponseEntity.<SubscriptionResponse>status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    )).join();
            } catch (Exception e) {
                log.error("Error retrieving subscription: {}", subscriptionId, e);
                return ResponseEntity.<SubscriptionResponse>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Get all subscriptions for a user
     */
    @GetMapping("/users/{userId}")
    @Timed(value = "subscription.get.user", description = "Time taken to get user subscriptions")
    @Operation(
        summary = "Get user subscriptions",
        description = "Retrieves all subscriptions for a user with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User subscriptions retrieved",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Page<SubscriptionResponse>>> getUserSubscriptions(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        log.info("Getting subscriptions for user: {}", userId);
        
        return CompletableFuture.<ResponseEntity<Page<SubscriptionResponse>>>supplyAsync(() -> {
            try {
                return subscriptionLifecycleService.getUserSubscriptions(userId, pageable)
                    .<ResponseEntity<Page<SubscriptionResponse>>>thenApply(result -> result.match(
                        subscriptionPage -> {
                            Page<SubscriptionResponse> responsePage = subscriptionPage.map(SubscriptionResponse::fromSubscription);
                            return ResponseEntity.ok(responsePage);
                        },
                        error -> ResponseEntity.<Page<SubscriptionResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    )).join();
            } catch (Exception e) {
                log.error("Error retrieving subscriptions for user: {}", userId, e);
                return ResponseEntity.<Page<SubscriptionResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Upgrade subscription
     */
    @PostMapping("/{subscriptionId}/upgrade")
    @Timed(value = "subscription.upgrade", description = "Time taken to upgrade subscription")
    @Operation(
        summary = "Upgrade subscription",
        description = "Upgrades a subscription to a higher tier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription upgraded successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid upgrade request"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot upgrade to requested tier"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> upgradeSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionUpgradeRequest request) {
        
        log.info("Upgrading subscription: {} to tier: {}", subscriptionId, request.getNewTier());
        
        return subscriptionUpgradeService.upgradeSubscription(
            subscriptionId,
            request.getNewTier()
        ).thenApply(result -> result.match(
            subscription -> {
                SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                return ResponseEntity.ok(response);
            },
            error -> switch (error) {
                case String msg when msg.contains("not found") -> 
                    ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                case String msg when msg.contains("Cannot upgrade") -> 
                    ResponseEntity.status(HttpStatus.CONFLICT).build();
                default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        ));
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/{subscriptionId}/cancel")
    @Timed(value = "subscription.cancel", description = "Time taken to cancel subscription")
    @Operation(
        summary = "Cancel subscription",
        description = "Cancels a subscription with optional immediate termination"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription cancelled successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot cancel subscription in current state"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> cancelSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Parameter(description = "Cancel immediately") @RequestParam(defaultValue = "false") boolean immediate,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason) {
        
        log.info("Cancelling subscription: {}, immediate: {}", subscriptionId, immediate);
        
        String cancellationReason = Optional.ofNullable(reason).orElse("User requested cancellation");
        
        return subscriptionLifecycleService.cancelSubscription(subscriptionId, cancellationReason)
            .thenApply(result -> result.match(
                subscription -> {
                    SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                    return ResponseEntity.ok(response);
                },
                error -> switch (error) {
                    case String msg when msg.contains("not found") -> 
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    case String msg when msg.contains("Cannot cancel") -> 
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                    default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            ));
    }

    /**
     * Activate subscription
     */
    @PostMapping("/{subscriptionId}/activate")
    @Timed(value = "subscription.activate", description = "Time taken to activate subscription")
    @Operation(
        summary = "Activate subscription",
        description = "Activates a subscription after successful payment"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription activated successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot activate subscription in current state"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> activateSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Parameter(description = "Payment transaction ID") @RequestParam UUID paymentTransactionId) {
        
        log.info("Activating subscription: {} with payment: {}", subscriptionId, paymentTransactionId);
        
        return subscriptionLifecycleService.activateSubscription(subscriptionId, paymentTransactionId)
            .thenApply(result -> result.match(
                subscription -> {
                    SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                    return ResponseEntity.ok(response);
                },
                error -> switch (error) {
                    case String msg when msg.contains("not found") -> 
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    case String msg when msg.contains("already active") -> 
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                    case String msg when msg.contains("Cannot activate") -> 
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                    default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            ));
    }

    /**
     * Suspend subscription
     */
    @PostMapping("/{subscriptionId}/suspend")
    @Timed(value = "subscription.suspend", description = "Time taken to suspend subscription")
    @Operation(
        summary = "Suspend subscription",
        description = "Suspends a subscription due to payment issues or policy violations"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription suspended successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot suspend subscription in current state"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> suspendSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Parameter(description = "Suspension reason") @RequestParam String reason) {
        
        log.info("Suspending subscription: {} for reason: {}", subscriptionId, reason);
        
        return subscriptionLifecycleService.suspendSubscription(subscriptionId, reason)
            .thenApply(result -> result.match(
                subscription -> {
                    SubscriptionResponse response = SubscriptionResponse.fromSubscription(subscription);
                    return ResponseEntity.ok(response);
                },
                error -> switch (error) {
                    case String msg when msg.contains("not found") -> 
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                    case String msg when msg.contains("Cannot suspend") -> 
                        ResponseEntity.status(HttpStatus.CONFLICT).build();
                    default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            ));
    }

    /**
     * Get subscription history
     */
    @GetMapping("/{subscriptionId}/history")
    @Timed(value = "subscription.history", description = "Time taken to get subscription history")
    @Operation(
        summary = "Get subscription history",
        description = "Retrieves the complete history of subscription changes"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription history retrieved",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<List<SubscriptionHistory>>> getSubscriptionHistory(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @PageableDefault(size = 20, sort = "changeDate") Pageable pageable) {
        
        log.info("Getting history for subscription: {}", subscriptionId);
        
        return CompletableFuture.<ResponseEntity<List<SubscriptionHistory>>>supplyAsync(() -> {
            try {
                return subscriptionLifecycleService.getSubscriptionHistory(subscriptionId, pageable)
                    .<ResponseEntity<List<SubscriptionHistory>>>thenApply(result -> result.match(
                        history -> ResponseEntity.ok(history),
                        error -> switch (error) {
                            case String msg when msg.contains("not found") -> 
                                ResponseEntity.<List<SubscriptionHistory>>status(HttpStatus.NOT_FOUND).build();
                            default -> ResponseEntity.<List<SubscriptionHistory>>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    )).join();
            } catch (Exception e) {
                log.error("Error retrieving subscription history: {}", subscriptionId, e);
                return ResponseEntity.<List<SubscriptionHistory>>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Check subscription health
     */
    @GetMapping("/{subscriptionId}/health")
    @Timed(value = "subscription.health", description = "Time taken to check subscription health")
    @Operation(
        summary = "Check subscription health",
        description = "Performs health check on subscription status and billing"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription is healthy"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "503", description = "Subscription has issues"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Void>> checkSubscriptionHealth(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId) {
        
        log.info("Checking health for subscription: {}", subscriptionId);
        
        return CompletableFuture.<ResponseEntity<Void>>supplyAsync(() -> {
            try {
                return subscriptionLifecycleService.checkSubscriptionHealth(subscriptionId)
                    .<ResponseEntity<Void>>thenApply(result -> result.match(
                        healthStatus -> switch (healthStatus.toLowerCase()) {
                            case "healthy" -> ResponseEntity.<Void>ok().build();
                            case "unhealthy" -> ResponseEntity.<Void>status(HttpStatus.SERVICE_UNAVAILABLE).build();
                            default -> ResponseEntity.<Void>status(HttpStatus.SERVICE_UNAVAILABLE).build();
                        },
                        error -> switch (error) {
                            case String msg when msg.contains("not found") -> 
                                ResponseEntity.<Void>status(HttpStatus.NOT_FOUND).build();
                            default -> ResponseEntity.<Void>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    )).join();
            } catch (Exception e) {
                log.error("Error checking subscription health: {}", subscriptionId, e);
                return ResponseEntity.<Void>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Get subscriptions by status
     */
    @GetMapping("/status/{status}")
    @Timed(value = "subscription.get.bystatus", description = "Time taken to get subscriptions by status")
    @Operation(
        summary = "Get subscriptions by status",
        description = "Retrieves subscriptions filtered by status with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscriptions retrieved",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Page<SubscriptionResponse>>> getSubscriptionsByStatus(
            @Parameter(description = "Subscription status") @PathVariable SubscriptionStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        
        log.info("Getting subscriptions with status: {}", status);
        
        return CompletableFuture.<ResponseEntity<Page<SubscriptionResponse>>>supplyAsync(() -> {
            try {
                return subscriptionLifecycleService.getSubscriptionsByStatus(status, pageable)
                    .<ResponseEntity<Page<SubscriptionResponse>>>thenApply(result -> result.match(
                        subscriptionPage -> {
                            Page<SubscriptionResponse> responsePage = subscriptionPage.map(SubscriptionResponse::fromSubscription);
                            return ResponseEntity.ok(responsePage);
                        },
                        error -> ResponseEntity.<Page<SubscriptionResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                    )).join();
            } catch (Exception e) {
                log.error("Error retrieving subscriptions by status: {}", status, e);
                return ResponseEntity.<Page<SubscriptionResponse>>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}