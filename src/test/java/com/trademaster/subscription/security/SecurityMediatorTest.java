package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Security Mediator Unit Tests
 *
 * MANDATORY: Zero Trust Security Pattern - Rule #6
 * MANDATORY: Mediator Pattern testing - Coordinates authentication, authorization, risk assessment
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Mediator Tests")
class SecurityMediatorTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AuditService auditService;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @InjectMocks
    private SecurityMediator securityMediator;

    private SecurityContext securityContext;
    private String correlationId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        correlationId = "test-correlation-123";

        securityContext = SecurityContext.builder()
            .userId(testUserId)
            .sessionId("session-456")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .requestPath("/api/subscriptions")
            .build();
    }

    @Nested
    @DisplayName("Successful Security Chain Tests")
    class SuccessfulSecurityChainTests {

        @Test
        @DisplayName("Should successfully mediate access with LOW risk")
        void shouldSuccessfullyMediateAccessWithLowRisk() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            RiskResult riskResult = new RiskResult(RiskResult.RiskLevel.LOW, 0.2, "Low risk user");

            when(authenticationService.authenticate(securityContext))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(authResult, securityContext))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(authzResult, securityContext))
                .thenReturn(Result.success(riskResult));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isSuccess()).isTrue();
            SecureContext secureContext = result.getValue();
            assertThat(secureContext.userId()).isEqualTo(testUserId);
            assertThat(secureContext.sessionId()).isEqualTo("session-456");
            assertThat(secureContext.correlationId()).isEqualTo(correlationId);
            assertThat(secureContext.riskResult()).isEqualTo(riskResult);

            verify(authenticationService).authenticate(securityContext);
            verify(authorizationService).authorize(authResult, securityContext);
            verify(riskAssessmentService).assessRisk(authzResult, securityContext);
            verify(auditService).logSecureAccess(securityContext, correlationId);
        }

        @Test
        @DisplayName("Should successfully mediate access with MEDIUM risk")
        void shouldSuccessfullyMediateAccessWithMediumRisk() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            RiskResult riskResult = new RiskResult(RiskResult.RiskLevel.MEDIUM, 0.5, "Medium risk user");

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(any(), any()))
                .thenReturn(Result.success(riskResult));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isSuccess()).isTrue();
            SecureContext secureContext = result.getValue();
            assertThat(secureContext.riskResult().level()).isEqualTo(RiskResult.RiskLevel.MEDIUM);
            verify(auditService).logSecureAccess(securityContext, correlationId);
        }
    }

    @Nested
    @DisplayName("Authentication Failure Tests")
    class AuthenticationFailureTests {

        @Test
        @DisplayName("Should handle authentication failure")
        void shouldHandleAuthenticationFailure() {
            // Given
            String authError = "Invalid authentication token";
            when(authenticationService.authenticate(securityContext))
                .thenReturn(Result.failure(authError));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            SecurityError error = result.getError();
            assertThat(error.message()).contains("Authentication failed");
            assertThat(error.message()).contains(authError);

            verify(authenticationService).authenticate(securityContext);
            verify(authorizationService, never()).authorize(any(), any());
            verify(riskAssessmentService, never()).assessRisk(any(), any());
            verify(auditService).logSecurityFailure(eq(securityContext), any(), eq(correlationId));
        }

        @Test
        @DisplayName("Should map authentication error correctly")
        void shouldMapAuthenticationErrorCorrectly() {
            // Given
            when(authenticationService.authenticate(any()))
                .thenReturn(Result.failure("authentication token expired"));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).startsWith("Authentication failed:");
        }
    }

    @Nested
    @DisplayName("Authorization Failure Tests")
    class AuthorizationFailureTests {

        @Test
        @DisplayName("Should handle authorization failure")
        void shouldHandleAuthorizationFailure() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            String authzError = "Authorization failed: User lacks required role";

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(authResult, securityContext))
                .thenReturn(Result.failure(authzError));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            SecurityError error = result.getError();
            assertThat(error.message()).contains("Authorization denied");
            assertThat(error.message()).contains("User lacks required role");

            verify(authenticationService).authenticate(securityContext);
            verify(authorizationService).authorize(authResult, securityContext);
            verify(riskAssessmentService, never()).assessRisk(any(), any());
            verify(auditService).logSecurityFailure(eq(securityContext), any(), eq(correlationId));
        }

        @Test
        @DisplayName("Should map authorization error correctly")
        void shouldMapAuthorizationErrorCorrectly() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.failure("Authorization failed: Required role not found"));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).startsWith("Authorization denied:");
        }
    }

    @Nested
    @DisplayName("Risk Assessment Tests")
    class RiskAssessmentTests {

        @Test
        @DisplayName("Should handle HIGH risk assessment denial")
        void shouldHandleHighRiskAssessmentDenial() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            RiskResult riskResult = new RiskResult(RiskResult.RiskLevel.HIGH, 0.8, "High risk detected");

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(authzResult, securityContext))
                .thenReturn(Result.success(riskResult));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            SecurityError error = result.getError();
            assertThat(error.message()).contains("High risk access denied");
            assertThat(error.message()).contains(testUserId.toString());

            verify(riskAssessmentService).assessRisk(authzResult, securityContext);
            verify(auditService).logSecurityFailure(eq(securityContext), any(), eq(correlationId));
        }

        @Test
        @DisplayName("Should handle CRITICAL risk assessment denial")
        void shouldHandleCriticalRiskAssessmentDenial() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            RiskResult riskResult = new RiskResult(RiskResult.RiskLevel.CRITICAL, 0.95, "Critical risk detected");

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(any(), any()))
                .thenReturn(Result.success(riskResult));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).contains("High risk access denied");
        }

        @Test
        @DisplayName("Should handle risk assessment service failure")
        void shouldHandleRiskAssessmentServiceFailure() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            String riskError = "Risk assessment failed: Service unavailable";

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(any(), any()))
                .thenReturn(Result.failure(riskError));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            SecurityError error = result.getError();
            assertThat(error.message()).contains("Risk assessment failed");
            assertThat(error.message()).contains("Service unavailable");
        }

        @Test
        @DisplayName("Should map risk assessment error correctly")
        void shouldMapRiskAssessmentErrorCorrectly() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(any(), any()))
                .thenReturn(Result.failure("Risk assessment failed: Calculation error"));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).startsWith("Risk assessment failed:");
        }
    }

    @Nested
    @DisplayName("Error Mapping Tests")
    class ErrorMappingTests {

        @Test
        @DisplayName("Should map generic error correctly")
        void shouldMapGenericErrorCorrectly() {
            // Given
            when(authenticationService.authenticate(any()))
                .thenReturn(Result.failure("unexpected system error"));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).startsWith("Security validation failed:");
        }
    }

    @Nested
    @DisplayName("Audit Logging Tests")
    class AuditLoggingTests {

        @Test
        @DisplayName("Should log successful access with audit service")
        void shouldLogSuccessfulAccessWithAuditService() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            RiskResult riskResult = new RiskResult(RiskResult.RiskLevel.LOW, 0.2, "Low risk");

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(any(), any()))
                .thenReturn(Result.success(riskResult));

            // When
            securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            verify(auditService).logSecureAccess(securityContext, correlationId);
            verify(auditService, never()).logSecurityFailure(any(), any(), any());
        }

        @Test
        @DisplayName("Should log failed access with audit service")
        void shouldLogFailedAccessWithAuditService() {
            // Given
            when(authenticationService.authenticate(any()))
                .thenReturn(Result.failure("authentication failed"));

            // When
            securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            verify(auditService).logSecurityFailure(eq(securityContext), any(SecurityError.class),
                eq(correlationId));
            verify(auditService, never()).logSecureAccess(any(), any());
        }
    }

    @Nested
    @DisplayName("Complete Security Chain Tests")
    class CompleteSecurityChainTests {

        @Test
        @DisplayName("Should execute complete security chain in order")
        void shouldExecuteCompleteSecurityChainInOrder() {
            // Given
            AuthenticationService.AuthResult authResult =
                new AuthenticationService.AuthResult(testUserId, "AUTHENTICATED", System.currentTimeMillis());
            AuthorizationService.AuthzResult authzResult =
                new AuthorizationService.AuthzResult(testUserId, "AUTHORIZED", System.currentTimeMillis());
            RiskResult riskResult = new RiskResult(RiskResult.RiskLevel.LOW, 0.2, "Low risk");

            when(authenticationService.authenticate(any()))
                .thenReturn(Result.success(authResult));
            when(authorizationService.authorize(any(), any()))
                .thenReturn(Result.success(authzResult));
            when(riskAssessmentService.assessRisk(any(), any()))
                .thenReturn(Result.success(riskResult));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isSuccess()).isTrue();

            // Verify execution order using InOrder
            var inOrder = inOrder(authenticationService, authorizationService,
                riskAssessmentService, auditService);
            inOrder.verify(authenticationService).authenticate(securityContext);
            inOrder.verify(authorizationService).authorize(authResult, securityContext);
            inOrder.verify(riskAssessmentService).assessRisk(authzResult, securityContext);
            inOrder.verify(auditService).logSecureAccess(securityContext, correlationId);
        }

        @Test
        @DisplayName("Should short-circuit on first failure")
        void shouldShortCircuitOnFirstFailure() {
            // Given
            when(authenticationService.authenticate(any()))
                .thenReturn(Result.failure("authentication failed"));

            // When
            Result<SecureContext, SecurityError> result =
                securityMediator.mediateAccess(securityContext, correlationId);

            // Then
            assertThat(result.isFailure()).isTrue();

            // Verify short-circuiting
            verify(authenticationService).authenticate(securityContext);
            verify(authorizationService, never()).authorize(any(), any());
            verify(riskAssessmentService, never()).assessRisk(any(), any());
        }
    }
}
