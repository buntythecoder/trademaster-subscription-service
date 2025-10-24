package com.trademaster.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Structured Logging Service - Facade Pattern
 *
 * MANDATORY: Facade Pattern - Rule #4 (Design Patterns)
 * MANDATORY: Single Responsibility - Delegates to specialized logging services
 * MANDATORY: Interface Segregation - Maintains backward compatibility
 *
 * This facade delegates to specialized logging services:
 * - ContextManager: MDC context management
 * - BusinessAuditLogger: Business event audit logging
 * - SecurityAuditLogger: Security event audit logging
 * - PerformanceLogger: Performance and application logging
 *
 * Provides structured JSON logging for ELK stack integration as per TradeMaster standards.
 * Implements context preservation for Virtual Thread operations.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructuredLoggingService {

    private final ContextManager contextManager;
    private final BusinessAuditLogger businessAuditLogger;
    private final SecurityAuditLogger securityAuditLogger;
    private final PerformanceLogger performanceLogger;

    // Context Management - Delegates to ContextManager

    public void setCorrelationId(String correlationId) {
        contextManager.setCorrelationId(correlationId);
    }

    public void setUserContext(String userId, String sessionId, String ipAddress, String userAgent) {
        contextManager.setUserContext(userId, sessionId, ipAddress, userAgent);
    }

    public void setBusinessContext(String subscriptionId, String transactionId) {
        contextManager.setBusinessContext(subscriptionId, transactionId);
    }

    public void clearContext() {
        contextManager.clearContext();
    }

    public void clearContext(String key) {
        contextManager.clearContext(key);
    }

    // Business Audit Logs - Delegates to BusinessAuditLogger

    public void logSubscriptionEvent(String operation, String subscriptionId, String userId,
                                   String tier, String status, String billingCycle,
                                   String amount, String currency) {
        businessAuditLogger.logSubscriptionEvent(operation, subscriptionId, userId, tier,
            status, billingCycle, amount, currency);
    }

    public void logBillingEvent(String operation, String subscriptionId, String userId,
                               String tier, String amount, String currency, String status,
                               String paymentMethod, Map<String, Object> metadata) {
        businessAuditLogger.logBillingEvent(operation, subscriptionId, userId, tier,
            amount, currency, status, paymentMethod, metadata);
    }

    public void logTierChangeEvent(String operation, String subscriptionId, String userId,
                                  String oldTier, String newTier, String reason,
                                  String oldAmount, String newAmount) {
        businessAuditLogger.logTierChangeEvent(operation, subscriptionId, userId, oldTier,
            newTier, reason, oldAmount, newAmount);
    }

    public void logUsageEvent(String operation, String userId, String subscriptionId,
                             String feature, String currentUsage, String usageLimit,
                             String status, Map<String, Object> details) {
        businessAuditLogger.logUsageEvent(operation, userId, subscriptionId, feature,
            currentUsage, usageLimit, status, details);
    }

    public void logTrialEvent(String operation, String subscriptionId, String userId,
                             String tier, String trialDays, String status) {
        businessAuditLogger.logTrialEvent(operation, subscriptionId, userId, tier,
            trialDays, status);
    }

    public void logPromoCodeEvent(String operation, String subscriptionId, String userId,
                                 String promoCode, String discount, String status) {
        businessAuditLogger.logPromoCodeEvent(operation, subscriptionId, userId, promoCode,
            discount, status);
    }

    // Security Audit Logs - Delegates to SecurityAuditLogger

    public void logSecurityIncident(String incidentType, String severity, String userId,
                                  String ipAddress, String userAgent, Map<String, Object> details) {
        securityAuditLogger.logSecurityIncident(incidentType, severity, userId, ipAddress,
            userAgent, details);
    }

    public void logRateLimitViolation(String endpoint, String userId, String ipAddress,
                                    String userAgent, String violationType) {
        securityAuditLogger.logRateLimitViolation(endpoint, userId, ipAddress, userAgent,
            violationType);
    }

    public void logUnauthorizedAccess(String resource, String userId, String ipAddress,
                                     String userAgent, String attemptedAction) {
        securityAuditLogger.logUnauthorizedAccess(resource, userId, ipAddress, userAgent,
            attemptedAction);
    }

    // Performance and Application Logs - Delegates to PerformanceLogger

    public void logPerformanceMetric(String operation, String component, long durationMs,
                                   String status, Map<String, Object> additionalMetrics) {
        performanceLogger.logPerformanceMetric(operation, component, durationMs, status,
            additionalMetrics);
    }

    public void logDatabasePerformance(String query, long executionTimeMs, String status,
                                     int recordsAffected) {
        performanceLogger.logDatabasePerformance(query, executionTimeMs, status, recordsAffected);
    }

    public void logServicePerformance(String service, String operation, long responseTimeMs,
                                    String status, String responseCode) {
        performanceLogger.logServicePerformance(service, operation, responseTimeMs, status,
            responseCode);
    }

    public void logInfo(String operation, String message, Map<String, Object> context) {
        performanceLogger.logInfo(operation, message, context);
    }

    public void logError(String operation, String errorMessage, String errorCode,
                        Throwable throwable, Map<String, Object> context) {
        performanceLogger.logError(operation, errorMessage, errorCode, throwable, context);
    }
}
