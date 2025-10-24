package com.trademaster.subscription.controller;

import com.trademaster.subscription.controller.base.BaseSubscriptionController;
import com.trademaster.subscription.dto.SubscriptionResponse;
import com.trademaster.subscription.dto.SubscriptionUpgradeRequest;
import com.trademaster.subscription.security.SecurityContext;
import com.trademaster.subscription.security.SecurityFacade;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
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
 * Subscription Upgrade Controller
 * MANDATORY: Single Responsibility - Upgrade operations only
 * MANDATORY: Rule #5 - <200 lines per class
 * MANDATORY: Rule #6 - Zero Trust Security (SecurityFacade + SecurityMediator)
 *
 * Handles subscription upgrade operations with full security.
 *
 * @author TradeMaster Development Team
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Slf4j
@Tag(name = "Subscription Upgrades", description = "Subscription tier upgrade operations")
public class SubscriptionUpgradeController extends BaseSubscriptionController {

    private final SecurityFacade securityFacade;
    private final SubscriptionUpgradeService upgradeService;

    public SubscriptionUpgradeController(
            SecurityFacade securityFacade,
            SubscriptionUpgradeService upgradeService) {
        this.securityFacade = securityFacade;
        this.upgradeService = upgradeService;
    }

    @PostMapping("/{subscriptionId}/upgrade")
    @Timed(value = "subscription.upgrade")
    @Operation(
        summary = "Upgrade subscription",
        description = "Upgrades a subscription to a higher tier"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription upgraded successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid upgrade request"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "403", description = "Authorization denied or high risk"),
        @ApiResponse(responseCode = "404", description = "Subscription not found"),
        @ApiResponse(responseCode = "409", description = "Cannot upgrade to requested tier")
    })
    public CompletableFuture<ResponseEntity<SubscriptionResponse>> upgradeSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionUpgradeRequest request,
            HttpServletRequest httpRequest) {

        log.info("Upgrading subscription: {} to tier: {}", subscriptionId, request.getNewTier());

        // Build security context from HTTP request (subscriptionId as userId proxy)
        SecurityContext securityContext = buildSecurityContext(httpRequest, subscriptionId);

        // Secure access through SecurityFacade
        return securityFacade.secureAccess(
            securityContext,
            secureCtx -> upgradeService.upgradeSubscription(subscriptionId, request.getNewTier())
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
