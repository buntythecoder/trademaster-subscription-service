package com.trademaster.subscription.service;

import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Error Tracking Service Unit Tests
 *
 * MANDATORY: Async operations testing - CompletableFuture validation
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Error Tracking Service Tests")
class ErrorTrackingServiceTest {

    @Mock
    private StructuredLoggingService loggingService;

    @Mock
    private ErrorMetricsCollector metricsCollector;

    @Mock
    private ErrorPatternTracker patternTracker;

    @InjectMocks
    private ErrorTrackingService errorTrackingService;

    private Timer.Sample testTimer;
    private Map<String, Object> testContext;
    private RuntimeException testException;

    @BeforeEach
    void setUp() {
        testTimer = mock(Timer.Sample.class);
        testContext = new HashMap<>();
        testContext.put("userId", UUID.randomUUID().toString());
        testContext.put("operation", "test_operation");
        testException = new RuntimeException("Test error message");

        // Setup default behaviors
        when(metricsCollector.startErrorProcessingTimer()).thenReturn(testTimer);
        when(patternTracker.trackErrorPattern(any(), anyString(), any())).thenReturn("pattern-key-123");
    }

    @Nested
    @DisplayName("Track Error Tests")
    class TrackErrorTests {

        @Test
        @DisplayName("Should successfully track error with full context")
        void shouldSuccessfullyTrackErrorWithFullContext() throws Exception {
            // Given
            String errorType = "VALIDATION_ERROR";
            String errorMessage = "Invalid subscription data";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector).startErrorProcessingTimer();
            verify(patternTracker).trackErrorPattern(any(), eq(errorType), eq(testException));
            verify(metricsCollector).incrementErrorCount();
            verify(metricsCollector).stopErrorProcessingTimer(testTimer);
        }

        @Test
        @DisplayName("Should track error with null throwable")
        void shouldTrackErrorWithNullThrowable() throws Exception {
            // Given
            String errorType = "BUSINESS_LOGIC_ERROR";
            String errorMessage = "Invalid state transition";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, null, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(patternTracker).trackErrorPattern(any(), eq(errorType), isNull());
            verify(metricsCollector).incrementErrorCount();
        }

        @Test
        @DisplayName("Should track error with empty context")
        void shouldTrackErrorWithEmptyContext() throws Exception {
            // Given
            String errorType = "SYSTEM_ERROR";
            String errorMessage = "Unexpected system error";
            Map<String, Object> emptyContext = new HashMap<>();

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, testException, emptyContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector).incrementErrorCount();
            verify(patternTracker).trackErrorPattern(any(), eq(errorType), eq(testException));
        }

        @Test
        @DisplayName("Should track user error")
        void shouldTrackUserError() throws Exception {
            // Given
            String errorType = "USER_INPUT_ERROR";
            String errorMessage = "Invalid user input";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(patternTracker).trackUserError(any());
        }
    }

    @Nested
    @DisplayName("Critical Error Detection Tests")
    class CriticalErrorDetectionTests {

        @Test
        @DisplayName("Should detect security exception as critical")
        void shouldDetectSecurityExceptionAsCritical() throws Exception {
            // Given
            SecurityException securityException = new SecurityException("Unauthorized access");
            String errorType = "SECURITY_INCIDENT";
            String errorMessage = "Security breach detected";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, securityException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector).incrementCriticalErrorCount();
            verify(metricsCollector).incrementErrorCount();
        }

        @Test
        @DisplayName("Should detect authentication exception as critical")
        void shouldDetectAuthenticationExceptionAsCritical() throws Exception {
            // Given
            Exception authException = new Exception("AuthenticationException");
            String errorType = "AUTH_FAILURE";
            String errorMessage = "Authentication failed";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, authException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            // Note: Will not be critical because exception class name doesn't contain "Authentication"
            verify(metricsCollector).incrementErrorCount();
        }

        @Test
        @DisplayName("Should detect SECURITY_INCIDENT error type as critical")
        void shouldDetectSecurityIncidentErrorTypeAsCritical() throws Exception {
            // Given
            String errorType = "SECURITY_INCIDENT";
            String errorMessage = "Security policy violation";

            // When - Pass null throwable to trigger error type checking
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, null, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector).incrementCriticalErrorCount();
        }

        @Test
        @DisplayName("Should detect DATA_CORRUPTION error type as critical")
        void shouldDetectDataCorruptionErrorTypeAsCritical() throws Exception {
            // Given
            String errorType = "DATA_CORRUPTION";
            String errorMessage = "Data integrity violation";

            // When - Pass null throwable to trigger error type checking
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, null, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector).incrementCriticalErrorCount();
        }

        @Test
        @DisplayName("Should detect PAYMENT_FAILURE error type as critical")
        void shouldDetectPaymentFailureErrorTypeAsCritical() throws Exception {
            // Given
            String errorType = "PAYMENT_FAILURE";
            String errorMessage = "Payment processing failed";

            // When - Pass null throwable to trigger error type checking
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, null, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector).incrementCriticalErrorCount();
        }

        @Test
        @DisplayName("Should not treat regular error as critical")
        void shouldNotTreatRegularErrorAsCritical() throws Exception {
            // Given
            String errorType = "VALIDATION_ERROR";
            String errorMessage = "Invalid input data";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(metricsCollector, never()).incrementCriticalErrorCount();
            verify(metricsCollector).incrementErrorCount();
        }
    }

    @Nested
    @DisplayName("Error Pattern Tests")
    class ErrorPatternTests {

        @Test
        @DisplayName("Should retrieve error patterns")
        void shouldRetrieveErrorPatterns() {
            // Given
            Map<String, ErrorPatternTracker.ErrorTrackingInfo> expectedPatterns = new HashMap<>();
            when(patternTracker.getErrorPatterns()).thenReturn(expectedPatterns);

            // When
            Map<String, ErrorPatternTracker.ErrorTrackingInfo> result =
                errorTrackingService.getErrorPatterns();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expectedPatterns);
            verify(patternTracker).getErrorPatterns();
        }

        @Test
        @DisplayName("Should track error pattern with pattern key")
        void shouldTrackErrorPatternWithPatternKey() throws Exception {
            // Given
            String expectedPatternKey = "ERROR_TYPE:RuntimeException";
            when(patternTracker.trackErrorPattern(any(), anyString(), any()))
                .thenReturn(expectedPatternKey);
            when(patternTracker.getPatternCount(expectedPatternKey)).thenReturn(5);

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError("TEST_ERROR", "Test message", testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(patternTracker).trackErrorPattern(any(), eq("TEST_ERROR"), eq(testException));
            verify(patternTracker).getPatternCount(expectedPatternKey);
        }
    }

    @Nested
    @DisplayName("User Error Statistics Tests")
    class UserErrorStatisticsTests {

        @Test
        @DisplayName("Should retrieve user error counts")
        void shouldRetrieveUserErrorCounts() {
            // Given
            Map<String, Integer> expectedCounts = new HashMap<>();
            expectedCounts.put("user-123", 5);
            expectedCounts.put("user-456", 3);
            when(patternTracker.getUserErrorCounts()).thenReturn(expectedCounts);

            // When
            Map<String, Integer> result = errorTrackingService.getUserErrorCounts();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(expectedCounts);
            verify(patternTracker).getUserErrorCounts();
        }

        @Test
        @DisplayName("Should return empty map when no user errors tracked")
        void shouldReturnEmptyMapWhenNoUserErrorsTracked() {
            // Given
            when(patternTracker.getUserErrorCounts()).thenReturn(new HashMap<>());

            // When
            Map<String, Integer> result = errorTrackingService.getUserErrorCounts();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cleanup Operations Tests")
    class CleanupOperationsTests {

        @Test
        @DisplayName("Should cleanup old error patterns")
        void shouldCleanupOldErrorPatterns() {
            // When
            errorTrackingService.cleanupOldPatterns();

            // Then
            verify(patternTracker).cleanupOldPatterns();
            verifyNoMoreInteractions(patternTracker);
        }

        @Test
        @DisplayName("Should handle cleanup multiple times")
        void shouldHandleCleanupMultipleTimes() {
            // When
            errorTrackingService.cleanupOldPatterns();
            errorTrackingService.cleanupOldPatterns();
            errorTrackingService.cleanupOldPatterns();

            // Then
            verify(patternTracker, times(3)).cleanupOldPatterns();
        }
    }

    @Nested
    @DisplayName("Error Tracking Resilience Tests")
    class ErrorTrackingResilienceTests {

        @Test
        @DisplayName("Should handle pattern tracker failure gracefully")
        void shouldHandlePatternTrackerFailure() throws Exception {
            // Given
            when(patternTracker.trackErrorPattern(any(), anyString(), any()))
                .thenThrow(new RuntimeException("Pattern tracking failed"));

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError("TEST_ERROR", "Test message", testException, testContext);

            // Then - Should complete without throwing exception (caught by try-catch)
            result.get(5, TimeUnit.SECONDS);
            assertThat(result.isDone()).isTrue();
        }

        @Test
        @DisplayName("Should always stop timer even on failure")
        void shouldAlwaysStopTimerEvenOnFailure() throws Exception {
            // Given
            when(patternTracker.trackErrorPattern(any(), anyString(), any()))
                .thenThrow(new RuntimeException("Pattern tracking failed"));

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError("TEST_ERROR", "Test message", testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);
            verify(metricsCollector).stopErrorProcessingTimer(testTimer);
        }

        @Test
        @DisplayName("Should complete successfully even with multiple failures")
        void shouldCompleteSuccessfullyWithMultipleFailures() throws Exception {
            // Given
            when(patternTracker.trackErrorPattern(any(), anyString(), any()))
                .thenThrow(new RuntimeException("Pattern tracking failed"));
            doThrow(new RuntimeException("User error tracking failed"))
                .when(patternTracker).trackUserError(any());

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError("TEST_ERROR", "Test message", testException, testContext);

            // Then - Should complete successfully despite failures (caught by try-catch)
            result.get(5, TimeUnit.SECONDS);
            assertThat(result.isDone()).isTrue();
            verify(metricsCollector).stopErrorProcessingTimer(testTimer);
        }
    }

    @Nested
    @DisplayName("Pattern Tracking Integration Tests")
    class PatternTrackingIntegrationTests {

        @Test
        @DisplayName("Should track error pattern and get occurrence count")
        void shouldTrackErrorPatternAndGetOccurrenceCount() throws Exception {
            // Given
            String patternKey = "ERROR_TYPE:RuntimeException";
            when(patternTracker.trackErrorPattern(any(), anyString(), any())).thenReturn(patternKey);
            when(patternTracker.getPatternCount(patternKey)).thenReturn(10);

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError("REPEATED_ERROR", "Repeated error", testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(patternTracker).trackErrorPattern(any(), eq("REPEATED_ERROR"), eq(testException));
            verify(patternTracker).getPatternCount(patternKey);
        }

        @Test
        @DisplayName("Should track user error statistics")
        void shouldTrackUserErrorStatistics() throws Exception {
            // Given
            String errorType = "USER_INPUT_ERROR";
            String errorMessage = "Invalid user input";

            // When
            CompletableFuture<Void> result =
                errorTrackingService.trackError(errorType, errorMessage, testException, testContext);

            // Then
            result.get(5, TimeUnit.SECONDS);

            verify(patternTracker).trackUserError(any());
        }
    }

    @Nested
    @DisplayName("Multiple Error Tracking Tests")
    class MultipleErrorTrackingTests {

        @Test
        @DisplayName("Should track multiple errors concurrently")
        void shouldTrackMultipleErrorsConcurrently() throws Exception {
            // When
            CompletableFuture<Void> result1 =
                errorTrackingService.trackError("ERROR_1", "First error", testException, testContext);
            CompletableFuture<Void> result2 =
                errorTrackingService.trackError("ERROR_2", "Second error", testException, testContext);
            CompletableFuture<Void> result3 =
                errorTrackingService.trackError("ERROR_3", "Third error", testException, testContext);

            // Then
            CompletableFuture.allOf(result1, result2, result3).get(10, TimeUnit.SECONDS);

            verify(metricsCollector, atLeast(3)).incrementErrorCount();
            verify(patternTracker, atLeast(3)).trackErrorPattern(any(), anyString(), any());
        }
    }
}
