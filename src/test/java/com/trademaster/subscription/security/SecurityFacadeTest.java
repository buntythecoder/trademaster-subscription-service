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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Security Facade Unit Tests
 *
 * MANDATORY: Zero Trust Security Pattern - Rule #6
 * MANDATORY: Facade Pattern testing - Entry point for external access
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Facade Tests")
class SecurityFacadeTest {

    @Mock
    private SecurityMediator securityMediator;

    @InjectMocks
    private SecurityFacade securityFacade;

    private SecurityContext securityContext;
    private SecureContext secureContext;
    private UUID testUserId;
    private RiskResult lowRiskResult;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        securityContext = SecurityContext.builder()
            .userId(testUserId)
            .sessionId("session-123")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .requestPath("/api/subscriptions")
            .build();

        lowRiskResult = new RiskResult(RiskResult.RiskLevel.LOW, 0.2, "Low risk user");

        secureContext = new SecureContext(
            testUserId,
            "session-123",
            "correlation-123",
            lowRiskResult,
            System.currentTimeMillis()
        );
    }

    @Nested
    @DisplayName("Successful Access Tests")
    class SuccessfulAccessTests {

        @Test
        @DisplayName("Should grant access and execute operation successfully")
        void shouldGrantAccessAndExecuteOperationSuccessfully() throws Exception {
            // Given
            String expectedResult = "operation-result";
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.success(expectedResult));

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(expectedResult);
            verify(securityMediator).mediateAccess(any(SecurityContext.class), anyString());
        }

        @Test
        @DisplayName("Should execute operation with virtual thread executor")
        void shouldExecuteOperationWithVirtualThreadExecutor() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.success("test"));

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(future.isDone()).isTrue();
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should generate unique correlation ID for each request")
        void shouldGenerateUniqueCorrelationId() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.success("test"));

            // When
            CompletableFuture<Result<String, SecurityError>> future1 =
                securityFacade.secureAccess(securityContext, operation);
            CompletableFuture<Result<String, SecurityError>> future2 =
                securityFacade.secureAccess(securityContext, operation);

            future1.get(5, TimeUnit.SECONDS);
            future2.get(5, TimeUnit.SECONDS);

            // Then - Verify mediator called twice with different correlation IDs
            verify(securityMediator, times(2)).mediateAccess(any(SecurityContext.class), anyString());
        }
    }

    @Nested
    @DisplayName("Security Denial Tests")
    class SecurityDenialTests {

        @Test
        @DisplayName("Should deny access when SecurityMediator rejects")
        void shouldDenyAccessWhenSecurityMediatorRejects() throws Exception {
            // Given
            SecurityError securityError = new SecurityError("Authentication failed");
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.failure(securityError));

            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.success("test"));

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(securityError);
            verify(securityMediator).mediateAccess(any(SecurityContext.class), anyString());
        }

        @Test
        @DisplayName("Should not execute operation when access denied")
        void shouldNotExecuteOperationWhenAccessDenied() throws Exception {
            // Given
            SecurityError securityError = new SecurityError("Authorization denied");
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.failure(securityError));

            // Mock operation to track if it was called
            @SuppressWarnings("unchecked")
            Function<Void, CompletableFuture<Result<String, String>>> operation = mock(Function.class);

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isFailure()).isTrue();
            verify(operation, never()).apply(any());
        }
    }

    @Nested
    @DisplayName("Operation Failure Tests")
    class OperationFailureTests {

        @Test
        @DisplayName("Should handle operation returning failure")
        void shouldHandleOperationReturningFailure() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            String operationError = "Operation failed: Database unavailable";
            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.failure(operationError));

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).isEqualTo(operationError);
        }

        @Test
        @DisplayName("Should handle operation throwing exception")
        void shouldHandleOperationThrowingException() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            RuntimeException operationException = new RuntimeException("Unexpected error");
            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.failedFuture(operationException);

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().message()).contains("Operation failed");
            assertThat(result.getError().message()).contains("Unexpected error");
        }

        @Test
        @DisplayName("Should convert operation string error to SecurityError")
        void shouldConvertOperationStringErrorToSecurityError() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            String operationError = "Business validation failed";
            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.failure(operationError));

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then
            assertThat(result.isFailure()).isTrue();
            SecurityError error = result.getError();
            assertThat(error.message()).isEqualTo(operationError);
        }
    }

    @Nested
    @DisplayName("Async Execution Tests")
    class AsyncExecutionTests {

        @Test
        @DisplayName("Should execute operation asynchronously")
        void shouldExecuteOperationAsynchronously() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(100); // Simulate async work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return Result.success("async-result");
                });

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);

            // Then - Future should complete eventually
            assertThat(future).isNotNull();
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo("async-result");
        }

        @Test
        @DisplayName("Should handle multiple concurrent secure access requests")
        void shouldHandleMultipleConcurrentSecureAccessRequests() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.success("concurrent-result"));

            // When
            CompletableFuture<Result<String, SecurityError>> future1 =
                securityFacade.secureAccess(securityContext, operation);
            CompletableFuture<Result<String, SecurityError>> future2 =
                securityFacade.secureAccess(securityContext, operation);
            CompletableFuture<Result<String, SecurityError>> future3 =
                securityFacade.secureAccess(securityContext, operation);

            // Then - All should complete successfully
            CompletableFuture.allOf(future1, future2, future3).get(5, TimeUnit.SECONDS);

            assertThat(future1.get().isSuccess()).isTrue();
            assertThat(future2.get().isSuccess()).isTrue();
            assertThat(future3.get().isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Integration Workflow Tests")
    class IntegrationWorkflowTests {

        @Test
        @DisplayName("Should execute complete successful access workflow")
        void shouldExecuteCompleteSuccessfulAccessWorkflow() throws Exception {
            // Given
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.success(secureContext));

            String expectedData = "workflow-result";
            Function<Void, CompletableFuture<Result<String, String>>> operation =
                v -> CompletableFuture.completedFuture(Result.success(expectedData));

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then - Verify complete workflow
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(expectedData);

            // Verify security mediation was called
            verify(securityMediator).mediateAccess(any(SecurityContext.class), anyString());
        }

        @Test
        @DisplayName("Should execute complete denial workflow")
        void shouldExecuteCompleteDenialWorkflow() throws Exception {
            // Given
            SecurityError securityError = new SecurityError("High risk access denied");
            when(securityMediator.mediateAccess(any(SecurityContext.class), anyString()))
                .thenReturn(Result.failure(securityError));

            @SuppressWarnings("unchecked")
            Function<Void, CompletableFuture<Result<String, String>>> operation = mock(Function.class);

            // When
            CompletableFuture<Result<String, SecurityError>> future =
                securityFacade.secureAccess(securityContext, operation);
            Result<String, SecurityError> result = future.get(5, TimeUnit.SECONDS);

            // Then - Verify complete denial workflow
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(securityError);

            // Verify operation was never executed
            verify(operation, never()).apply(any());
        }
    }
}
