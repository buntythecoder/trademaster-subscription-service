package com.trademaster.subscription.controller;

import com.trademaster.subscription.controller.base.BaseSubscriptionController;
import com.trademaster.subscription.dto.SubscriptionResponse;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.SubscriptionStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Query Controller
 * MANDATORY: Single Responsibility - Read operations only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #6 - Zero Trust Security (SecurityFacade + SecurityMediator)
 *
 * Handles all subscription query operations (GET endpoints) with full security.
 *
 * @author TradeMaster Development Team
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Slf4j
@Tag(name = "Subscription Queries", description = "Subscription read operations")
public class SubscriptionQueryController extends BaseSubscriptionController {

    private final SecurityFacade securityFacade;
    private final SubscriptionLifecycleService lifecycleService;

    public SubscriptionQueryController(
            SecurityFacade securityFacade,
            SubscriptionLifecycleService lifecycleService) {
        this.securityFacade = securityFacade;
        this.lifecycleService = lifecycleService;
    }

    @GetMapping("/users/{userId}/active")
    @Timed(value = "subscription.get.active")
    @Operation(summary = "Get user's active subscription")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active subscription found",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "No active subscription")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> getActiveSubscription(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            HttpServletRequest httpRequest) {

        log.info("Getting active subscription for user: {}", userId);

        SecurityContext securityContext = buildSecurityContext(httpRequest, userId);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.getActiveSubscription(userId)
        ).thenApply(result -> result.match(
            maybeSubscription -> maybeSubscription
                .map(subscription -> ResponseEntity.ok().body(SubscriptionResponse.fromSubscription(subscription)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(SubscriptionResponse.error("Subscription not found"))),
            securityError -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SubscriptionResponse.error(securityError.message()))
        ));
    }

    @GetMapping("/{subscriptionId}")
    @Timed(value = "subscription.get.byid")
    @Operation(summary = "Get subscription by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription found"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> getSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            HttpServletRequest httpRequest) {

        log.info("Getting subscription: {}", subscriptionId);

        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.findById(subscriptionId)
        ).thenApply(result -> result.match(
            maybeSubscription -> maybeSubscription
                .map(subscription -> ResponseEntity.ok().body(SubscriptionResponse.fromSubscription(subscription)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(SubscriptionResponse.error("Subscription not found"))),
            securityError -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SubscriptionResponse.error(securityError.message()))
        ));
    }

    @GetMapping("/users/{userId}")
    @Timed(value = "subscription.get.user")
    @Operation(summary = "Get user subscriptions with pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User subscriptions retrieved"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk")
    })
    public CompletableFuture<ResponseEntity<Page<SubscriptionResponse>>> getUserSubscriptions(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {

        log.info("Getting subscriptions for user: {}", userId);

        SecurityContext securityContext = buildSecurityContext(httpRequest, userId);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.getUserSubscriptions(userId, pageable)
        ).thenApply(result -> result.match(
            page -> ResponseEntity.ok(page.map(SubscriptionResponse::fromSubscription)),
            securityError -> ResponseEntity.<Page<SubscriptionResponse>>status(HttpStatus.UNAUTHORIZED).build()
        ));
    }

    @GetMapping("/{subscriptionId}/history")
    @Timed(value = "subscription.history")
    @Operation(summary = "Get subscription history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History retrieved"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public CompletableFuture<ResponseEntity<List<SubscriptionHistory>>> getSubscriptionHistory(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @PageableDefault(size = 20, sort = "changeDate") Pageable pageable,
            HttpServletRequest httpRequest) {

        log.info("Getting history for subscription: {}", subscriptionId);

        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.getSubscriptionHistory(subscriptionId, pageable)
        ).thenApply(result -> result.match(
            history -> ResponseEntity.ok(history),
            securityError -> ResponseEntity.<List<SubscriptionHistory>>status(HttpStatus.UNAUTHORIZED).build()
        ));
    }

    @GetMapping("/{subscriptionId}/health")
    @Timed(value = "subscription.health")
    @Operation(summary = "Check subscription health")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription is healthy"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "503", description = "Subscription has issues")
    })
    public CompletableFuture<ResponseEntity<Void>> checkSubscriptionHealth(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            HttpServletRequest httpRequest) {

        log.info("Checking health for subscription: {}", subscriptionId);

        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.checkSubscriptionHealth(subscriptionId)
        ).thenApply(result -> result.match(
            status -> status.equalsIgnoreCase("healthy") ?
                ResponseEntity.<Void>ok().build() :
                ResponseEntity.<Void>status(HttpStatus.SERVICE_UNAVAILABLE).build(),
            securityError -> ResponseEntity.<Void>status(HttpStatus.UNAUTHORIZED).build()
        ));
    }

    @GetMapping("/status/{status}")
    @Timed(value = "subscription.get.bystatus")
    @Operation(summary = "Get subscriptions by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscriptions retrieved"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk")
    })
    public CompletableFuture<ResponseEntity<Page<SubscriptionResponse>>> getSubscriptionsByStatus(
            @Parameter(description = "Subscription status") @PathVariable SubscriptionStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {

        log.info("Getting subscriptions with status: {}", status);

        // Use a system UUID for status queries (not user-specific)
        SecurityContext securityContext = buildSecurityContext(httpRequest, UUID.randomUUID());

        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.getSubscriptionsByStatus(status, pageable)
        ).thenApply(result -> result.match(
            page -> ResponseEntity.ok(page.map(SubscriptionResponse::fromSubscription)),
            securityError -> ResponseEntity.<Page<SubscriptionResponse>>status(HttpStatus.UNAUTHORIZED).build()
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
