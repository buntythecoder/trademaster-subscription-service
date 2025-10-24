package com.trademaster.subscription.controller;

import com.trademaster.subscription.controller.base.BaseSubscriptionController;
import com.trademaster.subscription.dto.SubscriptionRequest;
import com.trademaster.subscription.dto.SubscriptionResponse;
import com.trademaster.subscription.security.SecurityContext;
import com.trademaster.subscription.security.SecurityFacade;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Management Controller
 * MANDATORY: Single Responsibility - Lifecycle management operations only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #6 - Zero Trust Security (SecurityFacade + SecurityMediator)
 *
 * Handles subscription lifecycle operations (create, activate, suspend) with full security.
 *
 * @author TradeMaster Development Team
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Slf4j
@Tag(name = "Subscription Management", description = "Subscription lifecycle management operations")
public class SubscriptionManagementController extends BaseSubscriptionController {

    private final SecurityFacade securityFacade;
    private final SubscriptionLifecycleService lifecycleService;

    public SubscriptionManagementController(
            SecurityFacade securityFacade,
            SubscriptionLifecycleService lifecycleService) {
        this.securityFacade = securityFacade;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping
    @Timed(value = "subscription.create")
    @Operation(
        summary = "Create new subscription",
        description = "Creates a new subscription for a user with specified tier and billing cycle"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Subscription created successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid subscription data"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "409", description = "User already has active subscription")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request,
            HttpServletRequest httpRequest) {

        log.info("Creating subscription for user: {}, tier: {}", request.userId(), request.tier());

        // Build security context from HTTP request
        SecurityContext securityContext = buildSecurityContext(httpRequest, request.userId());

        // Secure access through SecurityFacade
        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.createSubscription(
                request.userId(),
                request.tier(),
                request.billingCycle(),
                request.isStartTrial()
            )
        ).thenApply(result -> result.match(
            subscription -> ResponseEntity.status(HttpStatus.CREATED)
                .body(SubscriptionResponse.fromSubscription(subscription)),
            securityError -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SubscriptionResponse.error(securityError.message()))
        ));
    }

    @PostMapping("/{subscriptionId}/activate")
    @Timed(value = "subscription.activate")
    @Operation(
        summary = "Activate subscription",
        description = "Activates a subscription after successful payment"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription activated successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot activate subscription in current state")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> activateSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Parameter(description = "Payment transaction ID") @RequestParam UUID paymentTransactionId,
            HttpServletRequest httpRequest) {

        log.info("Activating subscription: {} with payment: {}", subscriptionId, paymentTransactionId);

        // Build security context from HTTP request (subscriptionId as userId proxy)
        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        // Secure access through SecurityFacade
        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.activateSubscription(subscriptionId, paymentTransactionId)
        ).thenApply(result -> result.match(
            subscription -> ResponseEntity.ok()
                .body(SubscriptionResponse.fromSubscription(subscription)),
            securityError -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SubscriptionResponse.error(securityError.message()))
        ));
    }

    @PostMapping("/{subscriptionId}/suspend")
    @Timed(value = "subscription.suspend")
    @Operation(
        summary = "Suspend subscription",
        description = "Suspends a subscription due to payment issues or policy violations"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription suspended successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot suspend subscription in current state")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> suspendSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Parameter(description = "Suspension reason") @RequestParam String reason,
            HttpServletRequest httpRequest) {

        log.info("Suspending subscription: {} for reason: {}", subscriptionId, reason);

        // Build security context from HTTP request (subscriptionId as userId proxy)
        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        // Secure access through SecurityFacade
        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.suspendSubscription(subscriptionId, reason)
        ).thenApply(result -> result.match(
            subscription -> ResponseEntity.ok()
                .body(SubscriptionResponse.fromSubscription(subscription)),
            securityError -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SubscriptionResponse.error(securityError.message()))
        ));
    }

    /**
     * Build SecurityContext from HTTP request
     * MANDATORY: Rule #6 - Zero Trust Security Context
     */
    private SecurityContext buildSecurityContext(HttpServletRequest httpRequest, UUID userId) {
        return SecurityContext.builder()
            .userId(userId)
            .sessionId(httpRequest.getSession().getId())
            .ipAddress(httpRequest.getRemoteAddr())
            .userAgent(httpRequest.getHeader("User-Agent"))
            .requestPath(httpRequest.getRequestURI())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
