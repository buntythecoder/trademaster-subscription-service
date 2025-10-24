package com.trademaster.subscription.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit Service Unit Tests
 *
 * MANDATORY: Zero Trust Security Pattern - Rule #6
 * MANDATORY: Audit logging for ALL security events
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@DisplayName("Audit Service Tests")
class AuditServiceTest {

    private AuditService auditService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;
    private SecurityContext securityContext;
    private String correlationId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();

        // Setup test appender to capture log events
        logger = (Logger) LoggerFactory.getLogger(AuditService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Test data setup
        testUserId = UUID.randomUUID();
        correlationId = UUID.randomUUID().toString();

        securityContext = SecurityContext.builder()
            .userId(testUserId)
            .sessionId("session-123")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .requestPath("/api/subscriptions")
            .build();
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Nested
    @DisplayName("Secure Access Logging Tests")
    class SecureAccessLoggingTests {

        @Test
        @DisplayName("Should log secure access with INFO level")
        void shouldLogSecureAccessWithInfoLevel() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(1);

            ILoggingEvent logEvent = logsList.get(0);
            assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        }

        @Test
        @DisplayName("Should log secure access with SECURITY_ACCESS_GRANTED message")
        void shouldLogSecureAccessWithSecurityAccessGrantedMessage() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("SECURITY_ACCESS_GRANTED");
        }

        @Test
        @DisplayName("Should log secure access with userId")
        void shouldLogSecureAccessWithUserId() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains(testUserId.toString());
        }

        @Test
        @DisplayName("Should log secure access with sessionId")
        void shouldLogSecureAccessWithSessionId() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("session-123");
        }

        @Test
        @DisplayName("Should log secure access with IP address")
        void shouldLogSecureAccessWithIpAddress() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("192.168.1.1");
        }

        @Test
        @DisplayName("Should log secure access with request path")
        void shouldLogSecureAccessWithRequestPath() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("/api/subscriptions");
        }

        @Test
        @DisplayName("Should log secure access with correlation ID")
        void shouldLogSecureAccessWithCorrelationId() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains(correlationId);
        }

        @Test
        @DisplayName("Should log multiple secure access events independently")
        void shouldLogMultipleSecureAccessEventsIndependently() {
            // Given
            String correlationId1 = UUID.randomUUID().toString();
            String correlationId2 = UUID.randomUUID().toString();

            // When
            auditService.logSecureAccess(securityContext, correlationId1);
            auditService.logSecureAccess(securityContext, correlationId2);

            // Then
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(2);
            assertThat(logsList.get(0).getFormattedMessage()).contains(correlationId1);
            assertThat(logsList.get(1).getFormattedMessage()).contains(correlationId2);
        }
    }

    @Nested
    @DisplayName("Security Failure Logging Tests")
    class SecurityFailureLoggingTests {

        @Test
        @DisplayName("Should log security failure with WARN level")
        void shouldLogSecurityFailureWithWarnLevel() {
            // Given
            SecurityError securityError = new SecurityError("Authentication failed");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(1);

            ILoggingEvent logEvent = logsList.get(0);
            assertThat(logEvent.getLevel()).isEqualTo(Level.WARN);
        }

        @Test
        @DisplayName("Should log security failure with SECURITY_ACCESS_DENIED message")
        void shouldLogSecurityFailureWithSecurityAccessDeniedMessage() {
            // Given
            SecurityError securityError = new SecurityError("Authorization denied");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("SECURITY_ACCESS_DENIED");
        }

        @Test
        @DisplayName("Should log security failure with userId")
        void shouldLogSecurityFailureWithUserId() {
            // Given
            SecurityError securityError = new SecurityError("Access denied");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains(testUserId.toString());
        }

        @Test
        @DisplayName("Should log security failure with IP address")
        void shouldLogSecurityFailureWithIpAddress() {
            // Given
            SecurityError securityError = new SecurityError("Unauthorized");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("192.168.1.1");
        }

        @Test
        @DisplayName("Should log security failure with request path")
        void shouldLogSecurityFailureWithRequestPath() {
            // Given
            SecurityError securityError = new SecurityError("Forbidden");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("/api/subscriptions");
        }

        @Test
        @DisplayName("Should log security failure with error message")
        void shouldLogSecurityFailureWithErrorMessage() {
            // Given
            SecurityError securityError = new SecurityError("Invalid credentials");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("Invalid credentials");
        }

        @Test
        @DisplayName("Should log security failure with error type")
        void shouldLogSecurityFailureWithErrorType() {
            // Given
            SecurityError securityError = new SecurityError("Token expired");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("GENERAL");
        }

        @Test
        @DisplayName("Should log security failure with correlation ID")
        void shouldLogSecurityFailureWithCorrelationId() {
            // Given
            SecurityError securityError = new SecurityError("Session invalid");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains(correlationId);
        }

        @Test
        @DisplayName("Should log multiple security failures independently")
        void shouldLogMultipleSecurityFailuresIndependently() {
            // Given
            SecurityError error1 = new SecurityError("Authentication failed");
            SecurityError error2 = new SecurityError("Authorization denied");
            String correlationId1 = UUID.randomUUID().toString();
            String correlationId2 = UUID.randomUUID().toString();

            // When
            auditService.logSecurityFailure(securityContext, error1, correlationId1);
            auditService.logSecurityFailure(securityContext, error2, correlationId2);

            // Then
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(2);
            assertThat(logsList.get(0).getFormattedMessage()).contains("Authentication failed");
            assertThat(logsList.get(1).getFormattedMessage()).contains("Authorization denied");
        }
    }

    @Nested
    @DisplayName("High Risk Access Logging Tests")
    class HighRiskAccessLoggingTests {

        @Test
        @DisplayName("Should log high risk access with WARN level")
        void shouldLogHighRiskAccessWithWarnLevel() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.85,
                "Suspicious IP address"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(1);

            ILoggingEvent logEvent = logsList.get(0);
            assertThat(logEvent.getLevel()).isEqualTo(Level.WARN);
        }

        @Test
        @DisplayName("Should log high risk access with HIGH_RISK_ACCESS message")
        void shouldLogHighRiskAccessWithHighRiskAccessMessage() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.CRITICAL,
                0.95,
                "Multiple failed attempts"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("HIGH_RISK_ACCESS");
        }

        @Test
        @DisplayName("Should log high risk access with userId")
        void shouldLogHighRiskAccessWithUserId() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.80,
                "Unusual activity"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains(testUserId.toString());
        }

        @Test
        @DisplayName("Should log high risk access with IP address")
        void shouldLogHighRiskAccessWithIpAddress() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.88,
                "Blacklisted IP"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("192.168.1.1");
        }

        @Test
        @DisplayName("Should log high risk access with risk level")
        void shouldLogHighRiskAccessWithRiskLevel() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.CRITICAL,
                0.99,
                "Known attacker"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("CRITICAL");
        }

        @Test
        @DisplayName("Should log high risk access with risk score")
        void shouldLogHighRiskAccessWithRiskScore() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.75,
                "Abnormal behavior"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("0.75");
        }

        @Test
        @DisplayName("Should log high risk access with risk reason")
        void shouldLogHighRiskAccessWithRiskReason() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.82,
                "Rapid successive requests"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains("Rapid successive requests");
        }

        @Test
        @DisplayName("Should log high risk access with correlation ID")
        void shouldLogHighRiskAccessWithCorrelationId() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.87,
                "Unusual location"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            assertThat(logEvent.getFormattedMessage()).contains(correlationId);
        }

        @Test
        @DisplayName("Should log multiple high risk access events independently")
        void shouldLogMultipleHighRiskAccessEventsIndependently() {
            // Given
            RiskResult riskResult1 = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.80,
                "Suspicious pattern"
            );
            RiskResult riskResult2 = new RiskResult(
                RiskResult.RiskLevel.CRITICAL,
                0.95,
                "Known threat"
            );
            String correlationId1 = UUID.randomUUID().toString();
            String correlationId2 = UUID.randomUUID().toString();

            // When
            auditService.logHighRiskAccess(securityContext, riskResult1, correlationId1);
            auditService.logHighRiskAccess(securityContext, riskResult2, correlationId2);

            // Then
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(2);
            assertThat(logsList.get(0).getFormattedMessage()).contains("HIGH");
            assertThat(logsList.get(1).getFormattedMessage()).contains("CRITICAL");
        }
    }

    @Nested
    @DisplayName("Log Format Tests")
    class LogFormatTests {

        @Test
        @DisplayName("Should format secure access log with all required fields")
        void shouldFormatSecureAccessLogWithAllRequiredFields() {
            // When
            auditService.logSecureAccess(securityContext, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            String message = logEvent.getFormattedMessage();

            assertThat(message).contains("User:");
            assertThat(message).contains("Session:");
            assertThat(message).contains("IP:");
            assertThat(message).contains("Path:");
            assertThat(message).contains("Correlation:");
        }

        @Test
        @DisplayName("Should format security failure log with all required fields")
        void shouldFormatSecurityFailureLogWithAllRequiredFields() {
            // Given
            SecurityError securityError = new SecurityError("Test error");

            // When
            auditService.logSecurityFailure(securityContext, securityError, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            String message = logEvent.getFormattedMessage();

            assertThat(message).contains("User:");
            assertThat(message).contains("IP:");
            assertThat(message).contains("Path:");
            assertThat(message).contains("Error:");
            assertThat(message).contains("Type:");
            assertThat(message).contains("Correlation:");
        }

        @Test
        @DisplayName("Should format high risk access log with all required fields")
        void shouldFormatHighRiskAccessLogWithAllRequiredFields() {
            // Given
            RiskResult riskResult = new RiskResult(
                RiskResult.RiskLevel.HIGH,
                0.85,
                "Test reason"
            );

            // When
            auditService.logHighRiskAccess(securityContext, riskResult, correlationId);

            // Then
            ILoggingEvent logEvent = listAppender.list.get(0);
            String message = logEvent.getFormattedMessage();

            assertThat(message).contains("User:");
            assertThat(message).contains("IP:");
            assertThat(message).contains("Risk:");
            assertThat(message).contains("Score:");
            assertThat(message).contains("Reason:");
            assertThat(message).contains("Correlation:");
        }
    }
}
