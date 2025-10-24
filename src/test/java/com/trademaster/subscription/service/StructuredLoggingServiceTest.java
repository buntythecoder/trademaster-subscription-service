package com.trademaster.subscription.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Structured Logging Service Unit Tests
 *
 * MANDATORY: Facade Pattern testing - Verifies delegation to specialized logging services
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Structured Logging Service Tests")
class StructuredLoggingServiceTest {

    @Mock
    private ContextManager contextManager;

    @Mock
    private BusinessAuditLogger businessAuditLogger;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Mock
    private PerformanceLogger performanceLogger;

    @InjectMocks
    private StructuredLoggingService loggingService;

    @Nested
    @DisplayName("Context Management Delegation Tests")
    class ContextManagementTests {

        @Test
        @DisplayName("Should delegate setCorrelationId to ContextManager")
        void shouldDelegateSetCorrelationId() {
            // Given
            String correlationId = "test-correlation-123";

            // When
            loggingService.setCorrelationId(correlationId);

            // Then
            verify(contextManager).setCorrelationId(correlationId);
            verifyNoMoreInteractions(contextManager);
        }

        @Test
        @DisplayName("Should delegate setUserContext to ContextManager")
        void shouldDelegateSetUserContext() {
            // Given
            String userId = "user-123";
            String sessionId = "session-456";
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";

            // When
            loggingService.setUserContext(userId, sessionId, ipAddress, userAgent);

            // Then
            verify(contextManager).setUserContext(userId, sessionId, ipAddress, userAgent);
        }

        @Test
        @DisplayName("Should delegate setBusinessContext to ContextManager")
        void shouldDelegateSetBusinessContext() {
            // Given
            String subscriptionId = "sub-789";
            String transactionId = "txn-012";

            // When
            loggingService.setBusinessContext(subscriptionId, transactionId);

            // Then
            verify(contextManager).setBusinessContext(subscriptionId, transactionId);
        }

        @Test
        @DisplayName("Should delegate clearContext to ContextManager")
        void shouldDelegateClearContext() {
            // When
            loggingService.clearContext();

            // Then
            verify(contextManager).clearContext();
        }

        @Test
        @DisplayName("Should delegate clearContext with key to ContextManager")
        void shouldDelegateClearContextWithKey() {
            // Given
            String key = "test-key";

            // When
            loggingService.clearContext(key);

            // Then
            verify(contextManager).clearContext(key);
        }
    }

    @Nested
    @DisplayName("Business Audit Logging Delegation Tests")
    class BusinessAuditLoggingTests {

        @Test
        @DisplayName("Should delegate logSubscriptionEvent to BusinessAuditLogger")
        void shouldDelegateLogSubscriptionEvent() {
            // Given
            String operation = "SUBSCRIPTION_CREATED";
            String subscriptionId = "sub-123";
            String userId = "user-456";
            String tier = "PRO";
            String status = "ACTIVE";
            String billingCycle = "MONTHLY";
            String amount = "2999";
            String currency = "INR";

            // When
            loggingService.logSubscriptionEvent(operation, subscriptionId, userId, tier,
                status, billingCycle, amount, currency);

            // Then
            verify(businessAuditLogger).logSubscriptionEvent(operation, subscriptionId, userId,
                tier, status, billingCycle, amount, currency);
        }

        @Test
        @DisplayName("Should delegate logBillingEvent to BusinessAuditLogger")
        void shouldDelegateLogBillingEvent() {
            // Given
            String operation = "PAYMENT_PROCESSED";
            String subscriptionId = "sub-123";
            String userId = "user-456";
            String tier = "PRO";
            String amount = "2999";
            String currency = "INR";
            String status = "SUCCESS";
            String paymentMethod = "CREDIT_CARD";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("transactionId", "txn-789");

            // When
            loggingService.logBillingEvent(operation, subscriptionId, userId, tier, amount,
                currency, status, paymentMethod, metadata);

            // Then
            verify(businessAuditLogger).logBillingEvent(operation, subscriptionId, userId, tier,
                amount, currency, status, paymentMethod, metadata);
        }

        @Test
        @DisplayName("Should delegate logTierChangeEvent to BusinessAuditLogger")
        void shouldDelegateLogTierChangeEvent() {
            // Given
            String operation = "TIER_UPGRADED";
            String subscriptionId = "sub-123";
            String userId = "user-456";
            String oldTier = "FREE";
            String newTier = "PRO";
            String reason = "User upgrade";
            String oldAmount = "0";
            String newAmount = "2999";

            // When
            loggingService.logTierChangeEvent(operation, subscriptionId, userId, oldTier,
                newTier, reason, oldAmount, newAmount);

            // Then
            verify(businessAuditLogger).logTierChangeEvent(operation, subscriptionId, userId,
                oldTier, newTier, reason, oldAmount, newAmount);
        }

        @Test
        @DisplayName("Should delegate logUsageEvent to BusinessAuditLogger")
        void shouldDelegateLogUsageEvent() {
            // Given
            String operation = "USAGE_CHECKED";
            String userId = "user-456";
            String subscriptionId = "sub-123";
            String feature = "api_calls";
            String currentUsage = "5000";
            String usageLimit = "10000";
            String status = "WITHIN_LIMIT";
            Map<String, Object> details = new HashMap<>();
            details.put("remainingUsage", 5000);

            // When
            loggingService.logUsageEvent(operation, userId, subscriptionId, feature,
                currentUsage, usageLimit, status, details);

            // Then
            verify(businessAuditLogger).logUsageEvent(operation, userId, subscriptionId,
                feature, currentUsage, usageLimit, status, details);
        }

        @Test
        @DisplayName("Should delegate logTrialEvent to BusinessAuditLogger")
        void shouldDelegateLogTrialEvent() {
            // Given
            String operation = "TRIAL_STARTED";
            String subscriptionId = "sub-123";
            String userId = "user-456";
            String tier = "PRO";
            String trialDays = "14";
            String status = "ACTIVE";

            // When
            loggingService.logTrialEvent(operation, subscriptionId, userId, tier,
                trialDays, status);

            // Then
            verify(businessAuditLogger).logTrialEvent(operation, subscriptionId, userId,
                tier, trialDays, status);
        }

        @Test
        @DisplayName("Should delegate logPromoCodeEvent to BusinessAuditLogger")
        void shouldDelegateLogPromoCodeEvent() {
            // Given
            String operation = "PROMO_APPLIED";
            String subscriptionId = "sub-123";
            String userId = "user-456";
            String promoCode = "SAVE20";
            String discount = "20";
            String status = "APPLIED";

            // When
            loggingService.logPromoCodeEvent(operation, subscriptionId, userId, promoCode,
                discount, status);

            // Then
            verify(businessAuditLogger).logPromoCodeEvent(operation, subscriptionId, userId,
                promoCode, discount, status);
        }
    }

    @Nested
    @DisplayName("Security Audit Logging Delegation Tests")
    class SecurityAuditLoggingTests {

        @Test
        @DisplayName("Should delegate logSecurityIncident to SecurityAuditLogger")
        void shouldDelegateLogSecurityIncident() {
            // Given
            String incidentType = "SUSPICIOUS_ACTIVITY";
            String severity = "HIGH";
            String userId = "user-456";
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            Map<String, Object> details = new HashMap<>();
            details.put("attemptCount", 5);

            // When
            loggingService.logSecurityIncident(incidentType, severity, userId, ipAddress,
                userAgent, details);

            // Then
            verify(securityAuditLogger).logSecurityIncident(incidentType, severity, userId,
                ipAddress, userAgent, details);
        }

        @Test
        @DisplayName("Should delegate logRateLimitViolation to SecurityAuditLogger")
        void shouldDelegateLogRateLimitViolation() {
            // Given
            String endpoint = "/api/subscriptions";
            String userId = "user-456";
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            String violationType = "EXCESSIVE_REQUESTS";

            // When
            loggingService.logRateLimitViolation(endpoint, userId, ipAddress, userAgent,
                violationType);

            // Then
            verify(securityAuditLogger).logRateLimitViolation(endpoint, userId, ipAddress,
                userAgent, violationType);
        }

        @Test
        @DisplayName("Should delegate logUnauthorizedAccess to SecurityAuditLogger")
        void shouldDelegateLogUnauthorizedAccess() {
            // Given
            String resource = "/admin/dashboard";
            String userId = "user-456";
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            String attemptedAction = "VIEW";

            // When
            loggingService.logUnauthorizedAccess(resource, userId, ipAddress, userAgent,
                attemptedAction);

            // Then
            verify(securityAuditLogger).logUnauthorizedAccess(resource, userId, ipAddress,
                userAgent, attemptedAction);
        }
    }

    @Nested
    @DisplayName("Performance and Application Logging Delegation Tests")
    class PerformanceLoggingTests {

        @Test
        @DisplayName("Should delegate logPerformanceMetric to PerformanceLogger")
        void shouldDelegateLogPerformanceMetric() {
            // Given
            String operation = "SUBSCRIPTION_PROCESSING";
            String component = "SubscriptionService";
            long durationMs = 150L;
            String status = "SUCCESS";
            Map<String, Object> additionalMetrics = new HashMap<>();
            additionalMetrics.put("cacheHits", 5);

            // When
            loggingService.logPerformanceMetric(operation, component, durationMs, status,
                additionalMetrics);

            // Then
            verify(performanceLogger).logPerformanceMetric(operation, component, durationMs,
                status, additionalMetrics);
        }

        @Test
        @DisplayName("Should delegate logDatabasePerformance to PerformanceLogger")
        void shouldDelegateLogDatabasePerformance() {
            // Given
            String query = "SELECT * FROM subscriptions WHERE userId = ?";
            long executionTimeMs = 50L;
            String status = "SUCCESS";
            int recordsAffected = 1;

            // When
            loggingService.logDatabasePerformance(query, executionTimeMs, status, recordsAffected);

            // Then
            verify(performanceLogger).logDatabasePerformance(query, executionTimeMs, status,
                recordsAffected);
        }

        @Test
        @DisplayName("Should delegate logServicePerformance to PerformanceLogger")
        void shouldDelegateLogServicePerformance() {
            // Given
            String service = "PaymentGateway";
            String operation = "PROCESS_PAYMENT";
            long responseTimeMs = 200L;
            String status = "SUCCESS";
            String responseCode = "200";

            // When
            loggingService.logServicePerformance(service, operation, responseTimeMs, status,
                responseCode);

            // Then
            verify(performanceLogger).logServicePerformance(service, operation, responseTimeMs,
                status, responseCode);
        }

        @Test
        @DisplayName("Should delegate logInfo to PerformanceLogger")
        void shouldDelegateLogInfo() {
            // Given
            String operation = "SYSTEM_STATUS";
            String message = "System health check completed";
            Map<String, Object> context = new HashMap<>();
            context.put("status", "healthy");

            // When
            loggingService.logInfo(operation, message, context);

            // Then
            verify(performanceLogger).logInfo(operation, message, context);
        }

        @Test
        @DisplayName("Should delegate logError to PerformanceLogger")
        void shouldDelegateLogError() {
            // Given
            String operation = "PAYMENT_PROCESSING";
            String errorMessage = "Payment gateway timeout";
            String errorCode = "GATEWAY_TIMEOUT";
            Throwable throwable = new RuntimeException("Timeout");
            Map<String, Object> context = new HashMap<>();
            context.put("attemptCount", 3);

            // When
            loggingService.logError(operation, errorMessage, errorCode, throwable, context);

            // Then
            verify(performanceLogger).logError(operation, errorMessage, errorCode, throwable,
                context);
        }
    }

    @Nested
    @DisplayName("Integration Workflow Tests")
    class IntegrationWorkflowTests {

        @Test
        @DisplayName("Should support complete subscription creation workflow")
        void shouldSupportCompleteSubscriptionCreationWorkflow() {
            // Given
            String correlationId = "test-123";
            String userId = "user-456";
            String sessionId = "session-789";
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";
            String subscriptionId = "sub-123";
            String transactionId = "txn-456";

            // When - Simulate complete workflow
            loggingService.setCorrelationId(correlationId);
            loggingService.setUserContext(userId, sessionId, ipAddress, userAgent);
            loggingService.setBusinessContext(subscriptionId, transactionId);
            loggingService.logSubscriptionEvent("SUBSCRIPTION_CREATED", subscriptionId, userId,
                "PRO", "ACTIVE", "MONTHLY", "2999", "INR");
            loggingService.clearContext();

            // Then - Verify all delegations
            verify(contextManager).setCorrelationId(correlationId);
            verify(contextManager).setUserContext(userId, sessionId, ipAddress, userAgent);
            verify(contextManager).setBusinessContext(subscriptionId, transactionId);
            verify(businessAuditLogger).logSubscriptionEvent(eq("SUBSCRIPTION_CREATED"),
                eq(subscriptionId), eq(userId), eq("PRO"), eq("ACTIVE"), eq("MONTHLY"),
                eq("2999"), eq("INR"));
            verify(contextManager).clearContext();
        }

        @Test
        @DisplayName("Should support complete payment workflow with error handling")
        void shouldSupportCompletePaymentWorkflowWithErrorHandling() {
            // Given
            String correlationId = "test-456";
            String subscriptionId = "sub-123";
            String userId = "user-456";
            Throwable error = new RuntimeException("Payment failed");

            // When - Simulate payment workflow with error
            loggingService.setCorrelationId(correlationId);
            loggingService.logBillingEvent("PAYMENT_ATTEMPTED", subscriptionId, userId, "PRO",
                "2999", "INR", "FAILED", "CREDIT_CARD", Map.of("errorCode", "DECLINED"));
            loggingService.logError("PAYMENT_PROCESSING", "Payment gateway error",
                "GATEWAY_ERROR", error, Map.of("subscriptionId", subscriptionId));
            loggingService.clearContext();

            // Then - Verify error logging workflow
            verify(contextManager).setCorrelationId(correlationId);
            verify(businessAuditLogger).logBillingEvent(eq("PAYMENT_ATTEMPTED"),
                eq(subscriptionId), eq(userId), eq("PRO"), eq("2999"), eq("INR"),
                eq("FAILED"), eq("CREDIT_CARD"), any());
            verify(performanceLogger).logError(eq("PAYMENT_PROCESSING"),
                eq("Payment gateway error"), eq("GATEWAY_ERROR"), eq(error), any());
            verify(contextManager).clearContext();
        }
    }
}
