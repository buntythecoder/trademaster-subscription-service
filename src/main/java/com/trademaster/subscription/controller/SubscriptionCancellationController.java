package com.trademaster.subscription.controller;

import com.trademaster.subscription.controller.base.BaseSubscriptionController;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Subscription Cancellation Controller
 * MANDATORY: Single Responsibility - Cancellation operations only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #6 - Zero Trust Security (SecurityFacade + SecurityMediator)
 *
 * Handles subscription cancellation operations with full security.
 *
 * @author TradeMaster Development Team
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Slf4j
@Tag(name = "Subscription Cancellations", description = "Subscription cancellation operations")
public class SubscriptionCancellationController extends BaseSubscriptionController {

    private final SecurityFacade securityFacade;
    private final SubscriptionLifecycleService lifecycleService;

    public SubscriptionCancellationController(
            SecurityFacade securityFacade,
            SubscriptionLifecycleService lifecycleService) {
        this.securityFacade = securityFacade;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Timed(value = "subscription.cancel")
    @Operation(
        summary = "Cancel subscription",
        description = "Cancels a subscription with optional immediate termination"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription cancelled successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot cancel subscription in current state")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> cancelSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Parameter(description = "Cancel immediately") @RequestParam(defaultValue = "false") boolean immediate,
            @Parameter(description = "Cancellation reason") @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {

        log.info("Cancelling subscription: {}, immediate: {}", subscriptionId, immediate);
        String cancellationReason = Optional.ofNullable(reason).orElse("User requested cancellation");

        // Build security context from HTTP request (subscriptionId as userId proxy)
        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        // Secure access through SecurityFacade
        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> lifecycleService.cancelSubscription(subscriptionId, cancellationReason)
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
