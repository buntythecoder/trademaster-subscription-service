package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.entity.SubscriptionHistory;
import com.trademaster.subscription.entity.UsageTracking;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionHistoryRepository;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.repository.UsageTrackingRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Subscription Upgrade Service Unit Tests
 *
 * MANDATORY: Functional Programming patterns - Uses Result<T,E> pattern with flatMap chains
 * MANDATORY: Java 24 Virtual Threads - CompletableFuture with async operations
 * MANDATORY: Circuit Breaker pattern testing - Resilience4j integration
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Upgrade Service Tests")
class SubscriptionUpgradeServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionHistoryRepository historyRepository;

    @Mock
    private UsageTrackingRepository usageTrackingRepository;

    @Mock
    private SubscriptionMetricsService metricsService;

    @Mock
    private StructuredLoggingService loggingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TierComparisonService tierComparisonService;

    @Mock
    private CircuitBreaker databaseCircuitBreaker;

    @Mock
    private Retry databaseRetry;

    @Mock
    private SubscriptionBusinessLogic businessLogic;

    @InjectMocks
    private SubscriptionUpgradeService upgradeService;

    private UUID testSubscriptionId;
    private UUID testUserId;
    private Subscription testSubscription;
    private Timer.Sample testTimer;

    @BeforeEach
    void setUp() {
        testSubscriptionId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testTimer = mock(Timer.Sample.class); // Mock timer object

        testSubscription = Subscription.builder()
            .id(testSubscriptionId)
            .userId(testUserId)
            .tier(SubscriptionTier.FREE)
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
    @DisplayName("Successful Upgrade Tests")
    class SuccessfulUpgradeTests {

        @Test
        @DisplayName("Should successfully upgrade from FREE to PRO")
        void shouldUpgradeFromFreeToPro() throws Exception {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(tierComparisonService.validateTierUpgrade(any(), eq(SubscriptionTier.PRO)))
                .thenReturn(Result.success(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription saved = invocation.getArgument(0);
                    return saved;
                });

            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(List.of());

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getTier()).isEqualTo(SubscriptionTier.PRO);

            verify(subscriptionRepository).findById(testSubscriptionId);
            verify(tierComparisonService).validateTierUpgrade(any(), eq(SubscriptionTier.PRO));
            verify(subscriptionRepository).save(any(Subscription.class));
            verify(historyRepository).save(any(SubscriptionHistory.class));
            verify(metricsService).recordSubscriptionProcessingTime(eq(testTimer), anyString());
        }

        @Test
        @DisplayName("Should successfully upgrade from PRO to AI_PREMIUM")
        void shouldUpgradeFromProToAiPremium() throws Exception {
            // Given
            testSubscription.setTier(SubscriptionTier.PRO);
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(tierComparisonService.validateTierUpgrade(any(), eq(SubscriptionTier.AI_PREMIUM)))
                .thenReturn(Result.success(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(List.of());

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.AI_PREMIUM);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getTier()).isEqualTo(SubscriptionTier.AI_PREMIUM);
        }

        @Test
        @DisplayName("Should update usage limits during upgrade")
        void shouldUpdateUsageLimitsDuringUpgrade() throws Exception {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            UsageTracking usageTracking = UsageTracking.builder()
                .subscriptionId(testSubscriptionId)
                .featureName("api_calls")
                .usageLimit(1000L)
                .usageCount(500L)
                .build();

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(tierComparisonService.validateTierUpgrade(any(), eq(SubscriptionTier.PRO)))
                .thenReturn(Result.success(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(List.of(usageTracking));

            when(usageTrackingRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(usageTrackingRepository).findBySubscriptionId(testSubscriptionId);
            verify(usageTrackingRepository).saveAll(anyList());
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
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("not found");

            verify(subscriptionRepository).findById(testSubscriptionId);
            verify(tierComparisonService, never()).validateTierUpgrade(any(), any());
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
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("pending");
        }

        @Test
        @DisplayName("Should fail when subscription is CANCELLED")
        void shouldFailWhenSubscriptionIsCancelled() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.CANCELLED);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("cancelled");
        }

        @Test
        @DisplayName("Should fail when subscription is SUSPENDED")
        void shouldFailWhenSubscriptionIsSuspended() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.SUSPENDED);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("suspended");
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
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("expired");
        }

        @Test
        @DisplayName("Should fail when tier validation fails")
        void shouldFailWhenTierValidationFails() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(tierComparisonService.validateTierUpgrade(any(), any()))
                .thenReturn(Result.failure("Cannot downgrade from PRO to FREE"));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.FREE);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("downgrade");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker and Resilience Tests")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Should handle circuit breaker open during database save")
        void shouldHandleCircuitBreakerOpen() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(tierComparisonService.validateTierUpgrade(any(), any()))
                .thenReturn(Result.success(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("Database");
        }

        @Test
        @DisplayName("Should handle database exception during upgrade")
        void shouldHandleDatabaseException() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenThrow(new RuntimeException("Database connection timeout"));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("timeout");
        }
    }

    @Nested
    @DisplayName("TRIAL Status Upgrade Tests")
    class TrialUpgradeTests {

        @Test
        @DisplayName("Should allow upgrade from TRIAL status to PRO")
        void shouldAllowUpgradeFromTrialToPro() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.TRIAL);
            testSubscription.setTier(SubscriptionTier.FREE);

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            when(tierComparisonService.validateTierUpgrade(any(), eq(SubscriptionTier.PRO)))
                .thenReturn(Result.success(testSubscription));

            when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            when(usageTrackingRepository.findBySubscriptionId(testSubscriptionId))
                .thenReturn(List.of());

            when(historyRepository.save(any(SubscriptionHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompletableFuture<Result<Subscription, String>> result =
                upgradeService.upgradeSubscription(testSubscriptionId, SubscriptionTier.PRO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getTier()).isEqualTo(SubscriptionTier.PRO);
        }
    }
}
