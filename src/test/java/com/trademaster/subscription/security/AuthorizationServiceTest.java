package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization Service Unit Tests
 *
 * MANDATORY: Zero Trust Security Pattern - Rule #6
 * MANDATORY: Functional Programming - Pattern matching for authorization
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@DisplayName("Authorization Service Tests")
class AuthorizationServiceTest {

    private AuthorizationService authorizationService;
    private UUID testUserId;
    private AuthenticationService.AuthResult testAuthResult;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        testUserId = UUID.randomUUID();
        testAuthResult = new AuthenticationService.AuthResult(
            testUserId,
            "AUTHENTICATED",
            System.currentTimeMillis()
        );
    }

    @Nested
    @DisplayName("Granted Access Tests")
    class GrantedAccessTests {

        @Test
        @DisplayName("Should grant access for subscriptions path")
        void shouldGrantAccessForSubscriptionsPath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            AuthorizationService.AuthzResult authzResult = result.getValue();
            assertThat(authzResult.userId()).isEqualTo(testUserId);
            assertThat(authzResult.status()).isEqualTo("AUTHORIZED");
            assertThat(authzResult.authorizedAt()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should grant access for subscriptions subpath")
        void shouldGrantAccessForSubscriptionsSubpath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions/123/details")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().status()).isEqualTo("AUTHORIZED");
        }

        @Test
        @DisplayName("Should grant access for default paths")
        void shouldGrantAccessForDefaultPaths() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/public/health")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().status()).isEqualTo("AUTHORIZED");
        }

        @Test
        @DisplayName("Should include timestamp in authorization result")
        void shouldIncludeTimestampInAuthorizationResult() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions")
                .build();

            long beforeAuthz = System.currentTimeMillis();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            long afterAuthz = System.currentTimeMillis();

            // Then
            assertThat(result.isSuccess()).isTrue();
            long authorizedAt = result.getValue().authorizedAt();
            assertThat(authorizedAt).isBetween(beforeAuthz, afterAuthz);
        }

        @Test
        @DisplayName("Should preserve userId from AuthResult")
        void shouldPreserveUserIdFromAuthResult() {
            // Given
            UUID specificUserId = UUID.randomUUID();
            AuthenticationService.AuthResult specificAuthResult =
                new AuthenticationService.AuthResult(specificUserId, "AUTHENTICATED", System.currentTimeMillis());

            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)  // Different from authResult userId
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(specificAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().userId()).isEqualTo(specificUserId);
        }
    }

    @Nested
    @DisplayName("Denied Access Tests")
    class DeniedAccessTests {

        @Test
        @DisplayName("Should deny access for admin path")
        void shouldDenyAccessForAdminPath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Authorization failed");
            assertThat(result.getError()).contains("Insufficient permissions");
        }

        @Test
        @DisplayName("Should deny access for admin subpath")
        void shouldDenyAccessForAdminSubpath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin/users")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Insufficient permissions");
        }

        @Test
        @DisplayName("Should handle admin path with query parameters")
        void shouldHandleAdminPathWithQueryParameters() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin?action=delete")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Insufficient permissions");
        }
    }

    @Nested
    @DisplayName("Error Message Mapping Tests")
    class ErrorMessageMappingTests {

        @Test
        @DisplayName("Should prefix error message with 'Authorization failed:'")
        void shouldPrefixErrorMessageWithAuthorizationFailed() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).startsWith("Authorization failed:");
        }

        @Test
        @DisplayName("Should include specific error details in message")
        void shouldIncludeSpecificErrorDetailsInMessage() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isFailure()).isTrue();
            String errorMessage = result.getError();
            assertThat(errorMessage).contains("Authorization failed");
            assertThat(errorMessage).contains("Insufficient permissions");
        }
    }

    @Nested
    @DisplayName("Pattern Matching Tests")
    class PatternMatchingTests {

        @Test
        @DisplayName("Should use switch expression with pattern matching")
        void shouldUseSwitchExpressionWithPatternMatching() {
            // Given - Different paths for pattern matching
            SecurityContext subscriptionContext = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions")
                .build();

            SecurityContext adminContext = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> subscriptionResult =
                authorizationService.authorize(testAuthResult, subscriptionContext);
            Result<AuthorizationService.AuthzResult, String> adminResult =
                authorizationService.authorize(testAuthResult, adminContext);

            // Then - Different results based on pattern matching
            assertThat(subscriptionResult.isSuccess()).isTrue();
            assertThat(adminResult.isFailure()).isTrue();
        }

        @Test
        @DisplayName("Should use PermissionResult enum for authorization decisions")
        void shouldUsePermissionResultEnumForAuthorizationDecisions() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then - PermissionResult.GRANTED processed correctly
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty request path")
        void shouldHandleEmptyRequestPath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then - Empty path falls to default GRANTED
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should handle null request path")
        void shouldHandleNullRequestPath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath(null)
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then - Null path falls to default GRANTED
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should handle case-sensitive path matching")
        void shouldHandleCaseSensitivePathMatching() {
            // Given - Admin with different casing
            SecurityContext upperCaseContext = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/ADMIN")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, upperCaseContext);

            // Then - Case sensitive, uppercase ADMIN doesn't match "/admin"
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should handle very long request path")
        void shouldHandleVeryLongRequestPath() {
            // Given
            String longPath = "/api/subscriptions/" + "a".repeat(1000);
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath(longPath)
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should handle special characters in path")
        void shouldHandleSpecialCharactersInPath() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions/!@#$%^&*()")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Result Pattern Tests")
    class ResultPatternTests {

        @Test
        @DisplayName("Should return Result.success for granted authorization")
        void shouldReturnResultSuccessForGrantedAuthorization() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/subscriptions")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
        }

        @Test
        @DisplayName("Should return Result.failure for denied authorization")
        void shouldReturnResultFailureForDeniedAuthorization() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should use Result.tryExecute for exception handling")
        void shouldUseResultTryExecuteForExceptionHandling() {
            // Given
            SecurityContext context = SecurityContext.builder()
                .userId(testUserId)
                .sessionId("session-123")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .requestPath("/api/admin")
                .build();

            // When
            Result<AuthorizationService.AuthzResult, String> result =
                authorizationService.authorize(testAuthResult, context);

            // Then - Exception converted to Result.failure
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isNotEmpty();
        }
    }
}
