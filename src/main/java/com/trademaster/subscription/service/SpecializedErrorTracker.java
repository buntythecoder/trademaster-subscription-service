package com.trademaster.subscription.service;

import com.trademaster.subscription.constants.ErrorTypeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Specialized Error Tracker
 * MANDATORY: Single Responsibility - Domain-specific error tracking only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Provides specialized error tracking methods for validation, security, and business errors.
 * Separated from ErrorTrackingService to maintain SRP and keep classes under 200 lines.
 *
 * @author TradeMaster Development Team
 */
@Service
@RequiredArgsConstructor
public class SpecializedErrorTracker {

    private final ErrorTrackingService errorTrackingService;

    /**
     * Track validation error with field details
     */
    public CompletableFuture<Void> trackValidationError(Map<String, Object> fieldErrors, String source) {
        return errorTrackingService.trackError(
            ErrorTypeConstants.VALIDATION_ERROR,
            "Request validation failed",
            null,
            Map.of(
                "fieldErrors", fieldErrors,
                "source", source,
                "errorCategory", "validation"
            )
        );
    }

    /**
     * Track security incident
     */
    public CompletableFuture<Void> trackSecurityIncident(String incidentType, String details,
                                                        String severity, String userId) {
        return errorTrackingService.trackError(
            ErrorTypeConstants.SECURITY_INCIDENT,
            details,
            null,
            Map.of(
                "incidentType", incidentType,
                "severity", severity,
                "affectedUserId", userId != null ? userId : "unknown",
                "errorCategory", "security"
            )
        );
    }

    /**
     * Track business logic error
     */
    public CompletableFuture<Void> trackBusinessError(String businessRule, String violation,
                                                     String entityId, String entityType) {
        return errorTrackingService.trackError(
            ErrorTypeConstants.BUSINESS_RULE_VIOLATION,
            violation,
            null,
            Map.of(
                "businessRule", businessRule,
                "entityId", entityId,
                "entityType", entityType,
                "errorCategory", "business"
            )
        );
    }
}
