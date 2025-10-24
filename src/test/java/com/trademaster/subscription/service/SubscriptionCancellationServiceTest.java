package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Subscription Cancellation Service Unit Tests
 *
 * MANDATORY: Functional Programming patterns - Railway pattern with Result<T,E>
 * MANDATORY: Java 24 Virtual Threads - CompletableFuture with async operations
 * MANDATORY: Circuit Breaker pattern testing - Resilience4j integration
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Cancellation Service Tests")
class SubscriptionCancellationServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionHistoryRepository historyRepository;

    @Mock
    private SubscriptionMetricsService metricsService;

    @Mock
    private StructuredLoggingService loggingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CircuitBreaker databaseCircuitBreaker;

    @Mock
    private Retry databaseRetry;

    @InjectMocks
    private SubscriptionCancellationService cancellationService;

    private UUID testSubscriptionId;
    private UUID testUserId;
    private Subscription testSubscription;
    private Timer.Sample testTimer;
    private String cancellationReason;

    @BeforeEach
    void setUp() {
        testSubscriptionId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testTimer = mock(Timer.Sample.class);
        cancellationReason = "User requested cancellation";

        testSubscription = Subscription.builder()
            .id(testSubscriptionId)
            .userId(testUserId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();

        // Setup default circuit breaker behavior - pass through the operation
        when(databaseCircuitBreaker.executeSupplier(any()))
            .thenAnswer(invocation -> {
                Supplier<Result<?, String>> supplier = invocation.getArgument(0);
                return supplier.get();
            });

        // Setup default metrics behavior
        when(metricsService.startSubscriptionProcessingTimer())
            .thenReturn(testTimer);
    }

    @Nested
    @DisplayName("Successful Cancellation Tests")
    class SuccessfulCancellationTests {

        @Test
        @DisplayName("Should successfully cancel ACTIVE subscription")
        void shouldCancelActiveSubscription() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription saved = invocation.getArgument(0);
                    return saved;
                });

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(result.get().getValue().getCancelledAt()).isNotNull();
            assertThat(result.get().getValue().getCancellationReason()).isEqualTo(cancellationReason);
            assertThat(result.get().getValue().getAutoRenewal()).isFalse();

            verify(subscriptionRepository).findById(testSubscriptionId);
            verify(subscriptionRepository).save(any(Subscription.class));
            verify(historyRepository).save(any(SubscriptionHistory.class));
            verify(metricsService).recordSubscriptionProcessingTime(eq(testTimer), anyString());
        }

        @Test
        @DisplayName("Should successfully cancel TRIAL subscription")
        void shouldCancelTrialSubscription() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.TRIAL);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, "Trial not suitable");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should successfully cancel SUSPENDED subscription")
        void shouldCancelSuspendedSubscription() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.SUSPENDED);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, "Payment failed");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should set auto renewal to false during cancellation")
        void shouldDisableAutoRenewalDuringCancellation() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);
            testSubscription.setAutoRenewal(true);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getAutoRenewal()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation Failure Tests")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail when subscription not found")
        void shouldFailWhenSubscriptionNotFound() throws Exception {
            // Given
            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.empty());

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("not found");

            verify(subscriptionRepository).findById(testSubscriptionId);
            verify(subscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail when subscription is PENDING")
        void shouldFailWhenSubscriptionIsPending() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.PENDING);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("pending");
            assertThat(result.get().getError()).contains("contact support");
        }

        @Test
        @DisplayName("Should fail when subscription is already CANCELLED")
        void shouldFailWhenAlreadyCancelled() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.CANCELLED);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("already cancelled");
        }

        @Test
        @DisplayName("Should fail when subscription is EXPIRED")
        void shouldFailWhenSubscriptionIsExpired() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.EXPIRED);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("expired");
        }
    }

    @Nested
    @DisplayName("History Recording Tests")
    class HistoryRecordingTests {

        @Test
        @DisplayName("Should record cancellation history with correct details")
        void shouldRecordCancellationHistory() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(historyRepository).save(argThat(history ->
                history.getSubscriptionId().equals(testSubscriptionId) &&
                history.getUserId().equals(testUserId) &&
                history.getAction().equals("SUBSCRIPTION_CANCELLED")
            ));
        }
    }

    @Nested
    @DisplayName("Circuit Breaker and Resilience Tests")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should handle database exception during cancellation")
        void shouldHandleDatabaseException() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new RuntimeException("Database connection timeout"));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("timeout");
        }

        @Test
        @DisplayName("Should handle history recording failure")
        void shouldHandleHistoryRecordingFailure() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenThrow(new RuntimeException("History database error"));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("history");
        }
    }

    @Nested
    @DisplayName("Cancellation Reason Tests")
    class CancellationReasonTests {

        @Test
        @DisplayName("Should store custom cancellation reason")
        void shouldStoreCustomCancellationReason() throws Exception {
            // Given
            String customReason = "Switching to competitor service";
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                cancellationService.cancelSubscription(testSubscriptionId, customReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getCancellationReason()).isEqualTo(customReason);
        }
    }
}
