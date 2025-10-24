package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.repository.UsageTrackingRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;

/**
 * Subscription Usage Service Unit Tests
 *
 * MANDATORY: Functional Programming patterns - Uses Result<T,E> pattern
 * MANDATORY: Java 24 Virtual Threads - CompletableFuture with async operations
 * MANDATORY: Circuit Breaker pattern testing - Resilience4j integration
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Subscription Usage Service Tests")
class SubscriptionUsageServiceTest {

    @Mock
    private UsageTracker usageTracker;

    @Mock
    private UsageTrackingRepository usageTrackingRepository;

    @Mock
    private CircuitBreaker databaseCircuitBreaker;

    @InjectMocks
    private SubscriptionUsageService usageService;

    private UUID testSubscriptionId;
    private String testFeatureName;
    private UsageTracking testUsageTracking;

    @BeforeEach
    void setUp() {
        testSubscriptionId = UUID.randomUUID();
        testFeatureName = "api_calls";

        testUsageTracking = UsageTracking.builder()
            .id(UUID.randomUUID())
            .subscriptionId(testSubscriptionId)
            .featureName(testFeatureName)
            .usageLimit(1000L)
            .usageCount(500L)
            .build();

        // Setup default circuit breaker behavior - pass through the operation
        when(databaseCircuitBreaker.executeSupplier(any()))
            .thenAnswer(invocation -> {
                Supplier<Result<?, String>> supplier = invocation.getArgument(0);
                return supplier.get();
            });
    }

    @Nested
    @DisplayName("Can Use Feature Tests")
    class CanUseFeatureTests {

        @Test
        @DisplayName("Should successfully check feature availability")
        void shouldCheckFeatureAvailability() throws Exception {
            // Given
            CompletableFuture<Result<Boolean, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(true));

            when(usageTracker.canUseFeature(testSubscriptionId, testFeatureName))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Boolean, String>> result =
                usageService.canUseFeature(testSubscriptionId, testFeatureName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isTrue();

            verify(usageTracker).canUseFeature(testSubscriptionId, testFeatureName);
            verifyNoMoreInteractions(usageTracker);
        }

        @Test
        @DisplayName("Should return false when usage limit reached")
        void shouldReturnFalseWhenLimitReached() throws Exception {
            // Given
            CompletableFuture<Result<Boolean, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(false));

            when(usageTracker.canUseFeature(testSubscriptionId, testFeatureName))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Boolean, String>> result =
                usageService.canUseFeature(testSubscriptionId, testFeatureName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isFalse();
        }

        @Test
        @DisplayName("Should handle feature check failure")
        void shouldHandleFeatureCheckFailure() throws Exception {
            // Given
            String errorMessage = "Subscription not found";
            CompletableFuture<Result<Boolean, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(usageTracker.canUseFeature(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Boolean, String>> result =
                usageService.canUseFeature(testSubscriptionId, testFeatureName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("not found");
        }
    }

    @Nested
    @DisplayName("Increment Usage Tests")
    class IncrementUsageTests {

        @Test
        @DisplayName("Should successfully increment usage")
        void shouldIncrementUsage() throws Exception {
            // Given
            int incrementBy = 10;
            UsageTracking updatedUsage = UsageTracking.builder()
                .id(testUsageTracking.getId())
                .subscriptionId(testSubscriptionId)
                .featureName(testFeatureName)
                .usageLimit(1000L)
                .usageCount(510L)  // 500 + 10
                .build();

            CompletableFuture<Result<UsageTracking, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(updatedUsage));

            when(usageTracker.incrementUsage(testSubscriptionId, testFeatureName, incrementBy))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<UsageTracking, String>> result =
                usageService.incrementUsage(testSubscriptionId, testFeatureName, incrementBy);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getUsageCount()).isEqualTo(510L);

            verify(usageTracker).incrementUsage(testSubscriptionId, testFeatureName, incrementBy);
        }

        @Test
        @DisplayName("Should handle increment failure when limit exceeded")
        void shouldHandleIncrementFailureWhenLimitExceeded() throws Exception {
            // Given
            String errorMessage = "Usage limit exceeded";
            CompletableFuture<Result<UsageTracking, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(usageTracker.incrementUsage(any(), any(), anyInt()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<UsageTracking, String>> result =
                usageService.incrementUsage(testSubscriptionId, testFeatureName, 600);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("limit exceeded");
        }

        @Test
        @DisplayName("Should handle negative increment")
        void shouldHandleNegativeIncrement() throws Exception {
            // Given
            String errorMessage = "Increment must be positive";
            CompletableFuture<Result<UsageTracking, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(usageTracker.incrementUsage(any(), any(), anyInt()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<UsageTracking, String>> result =
                usageService.incrementUsage(testSubscriptionId, testFeatureName, -10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("positive");
        }
    }

    @Nested
    @DisplayName("Get Current Usage Tests")
    class GetCurrentUsageTests {

        @Test
        @DisplayName("Should successfully get current usage")
        void shouldGetCurrentUsage() throws Exception {
            // Given
            UsageTracking usage1 = UsageTracking.builder()
                .featureName("api_calls")
                .usageLimit(1000L)
                .usageCount(500L)
                .build();

            UsageTracking usage2 = UsageTracking.builder()
                .featureName("storage_gb")
                .usageLimit(100L)
                .usageCount(50L)
                .build();

            List<UsageTracking> usageList = List.of(usage1, usage2);

            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(usageList);

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.getCurrentUsage(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).hasSize(2);
            assertThat(result.get().getValue()).containsExactlyInAnyOrder(usage1, usage2);

            verify(usageTrackingRepository).findBySubscriptionId(testSubscriptionId);
        }

        @Test
        @DisplayName("Should return empty list when no usage tracking")
        void shouldReturnEmptyListWhenNoUsageTracking() throws Exception {
            // Given
            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(List.of());

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.getCurrentUsage(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEmpty();
        }

        @Test
        @DisplayName("Should handle database error")
        void shouldHandleDatabaseError() throws Exception {
            // Given
            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenThrow(new RuntimeException("Database connection timeout"));

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.getCurrentUsage(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("timeout");
        }
    }

    @Nested
    @DisplayName("Reset Usage Tests")
    class ResetUsageTests {

        @Test
        @DisplayName("Should successfully reset usage for billing period")
        void shouldResetUsageForBillingPeriod() throws Exception {
            // Given
            UsageTracking resetUsage = UsageTracking.builder()
                .id(testUsageTracking.getId())
                .subscriptionId(testSubscriptionId)
                .featureName(testFeatureName)
                .usageLimit(1000L)
                .usageCount(0L)  // Reset to 0
                .build();

            List<UsageTracking> resetList = List.of(resetUsage);
            CompletableFuture<Result<List<UsageTracking>, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(resetList));

            when(usageTracker.resetUsage(testSubscriptionId))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.resetUsageForBillingPeriod(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).hasSize(1);
            assertThat(result.get().getValue().get(0).getUsageCount()).isEqualTo(0L);

            verify(usageTracker).resetUsage(testSubscriptionId);
        }

        @Test
        @DisplayName("Should handle reset failure when subscription not found")
        void shouldHandleResetFailureWhenSubscriptionNotFound() throws Exception {
            // Given
            String errorMessage = "Subscription not found";
            CompletableFuture<Result<List<UsageTracking>, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(usageTracker.resetUsage(any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.resetUsageForBillingPeriod(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("not found");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker and Resilience Tests")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should handle circuit breaker open during getCurrentUsage")
        void shouldHandleCircuitBreakerOpen() throws Exception {
            // Given
            reset(databaseCircuitBreaker);
            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> Result.failure("Circuit breaker open - database unavailable"));

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.getCurrentUsage(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("Circuit breaker");
        }

        @Test
        @DisplayName("Should execute with resilience on successful operation")
        void shouldExecuteWithResilienceOnSuccess() throws Exception {
            // Given
            List<UsageTracking> usageList = List.of(testUsageTracking);

            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(usageList);

            // When
            CompletableFuture<Result<List<UsageTracking>, String>> result =
                usageService.getCurrentUsage(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).hasSize(1);

            verify(databaseCircuitBreaker).executeSupplier(any());
            verify(usageTrackingRepository).findBySubscriptionId(testSubscriptionId);
        }
    }
}
