package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
 * Subscription Billing Service Unit Tests
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
@DisplayName("Subscription Billing Service Tests")
class SubscriptionBillingServiceTest {

    @Mock
    private BillingProcessor billingProcessor;

    @Mock
    private BillingCycleManager cycleManager;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CircuitBreaker databaseCircuitBreaker;

    @InjectMocks
    private SubscriptionBillingService billingService;

    private UUID testSubscriptionId;
    private UUID testPaymentTransactionId;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testSubscriptionId = UUID.randomUUID();
        testPaymentTransactionId = UUID.randomUUID();

        testSubscription = Subscription.builder()
            .id(testSubscriptionId)
            .userId(UUID.randomUUID())
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
    }

    @Nested
    @DisplayName("Process Billing Tests")
    class ProcessBillingTests {

        @Test
        @DisplayName("Should successfully process billing")
        void shouldProcessBilling() throws Exception {
            // Given
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(billingProcessor.processBilling(testSubscriptionId, testPaymentTransactionId))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                billingService.processBilling(testSubscriptionId, testPaymentTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEqualTo(testSubscription);

            verify(billingProcessor).processBilling(testSubscriptionId, testPaymentTransactionId);
            verifyNoMoreInteractions(billingProcessor);
        }

        @Test
        @DisplayName("Should handle billing processing failure")
        void shouldHandleBillingProcessingFailure() throws Exception {
            // Given
            String errorMessage = "Payment processing failed";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(billingProcessor.processBilling(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                billingService.processBilling(testSubscriptionId, testPaymentTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("Should handle payment transaction not found")
        void shouldHandlePaymentTransactionNotFound() throws Exception {
            // Given
            String errorMessage = "Payment transaction not found";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(billingProcessor.processBilling(testSubscriptionId, testPaymentTransactionId))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                billingService.processBilling(testSubscriptionId, testPaymentTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("not found");
        }
    }

    @Nested
    @DisplayName("Update Billing Cycle Tests")
    class UpdateBillingCycleTests {

        @Test
        @DisplayName("Should successfully update billing cycle to quarterly")
        void shouldUpdateBillingCycleToQuarterly() throws Exception {
            // Given
            testSubscription.setBillingCycle(BillingCycle.QUARTERLY);
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(cycleManager.updateBillingCycle(testSubscriptionId, BillingCycle.QUARTERLY))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                billingService.updateBillingCycle(testSubscriptionId, BillingCycle.QUARTERLY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getBillingCycle()).isEqualTo(BillingCycle.QUARTERLY);

            verify(cycleManager).updateBillingCycle(testSubscriptionId, BillingCycle.QUARTERLY);
        }

        @Test
        @DisplayName("Should successfully update billing cycle to annual")
        void shouldUpdateBillingCycleToAnnual() throws Exception {
            // Given
            testSubscription.setBillingCycle(BillingCycle.ANNUAL);
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(cycleManager.updateBillingCycle(testSubscriptionId, BillingCycle.ANNUAL))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                billingService.updateBillingCycle(testSubscriptionId, BillingCycle.ANNUAL);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
        }

        @Test
        @DisplayName("Should handle billing cycle update failure")
        void shouldHandleBillingCycleUpdateFailure() throws Exception {
            // Given
            String errorMessage = "Cannot change billing cycle during trial period";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(cycleManager.updateBillingCycle(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                billingService.updateBillingCycle(testSubscriptionId, BillingCycle.QUARTERLY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("trial period");
        }
    }

    @Nested
    @DisplayName("Get Upcoming Billing Amount Tests")
    class GetUpcomingBillingAmountTests {

        @Test
        @DisplayName("Should calculate monthly PRO tier billing amount")
        void shouldCalculateMonthlyProAmount() throws Exception {
            // Given
            testSubscription.setTier(SubscriptionTier.PRO);
            testSubscription.setBillingCycle(BillingCycle.MONTHLY);

            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    Supplier<Result<BigDecimal, String>> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<BigDecimal, String>> result =
                billingService.getUpcomingBillingAmount(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEqualTo(SubscriptionTier.PRO.getMonthlyPrice());

            verify(subscriptionRepository).findById(testSubscriptionId);
        }

        @Test
        @DisplayName("Should calculate quarterly AI_PREMIUM tier billing amount")
        void shouldCalculateQuarterlyAiPremiumAmount() throws Exception {
            // Given
            testSubscription.setTier(SubscriptionTier.AI_PREMIUM);
            testSubscription.setBillingCycle(BillingCycle.QUARTERLY);

            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    Supplier<Result<BigDecimal, String>> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<BigDecimal, String>> result =
                billingService.getUpcomingBillingAmount(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEqualTo(SubscriptionTier.AI_PREMIUM.getQuarterlyPrice());
        }

        @Test
        @DisplayName("Should calculate annual INSTITUTIONAL tier billing amount")
        void shouldCalculateAnnualInstitutionalAmount() throws Exception {
            // Given
            testSubscription.setTier(SubscriptionTier.INSTITUTIONAL);
            testSubscription.setBillingCycle(BillingCycle.ANNUAL);

            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    Supplier<Result<BigDecimal, String>> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.of(testSubscription));

            // When
            CompletableFuture<Result<BigDecimal, String>> result =
                billingService.getUpcomingBillingAmount(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEqualTo(SubscriptionTier.INSTITUTIONAL.getAnnualPrice());
        }

        @Test
        @DisplayName("Should handle subscription not found")
        void shouldHandleSubscriptionNotFound() throws Exception {
            // Given
            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    Supplier<Result<BigDecimal, String>> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

            when(subscriptionRepository.findById(testSubscriptionId))
                .thenReturn(Optional.empty());

            // When
            CompletableFuture<Result<BigDecimal, String>> result =
                billingService.getUpcomingBillingAmount(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("not found");
        }

        @Test
        @DisplayName("Should handle database error with circuit breaker")
        void shouldHandleDatabaseErrorWithCircuitBreaker() throws Exception {
            // Given
            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenReturn(Result.failure("Circuit breaker open - database unavailable"));

            // When
            CompletableFuture<Result<BigDecimal, String>> result =
                billingService.getUpcomingBillingAmount(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("Circuit breaker");
        }
    }

    @Nested
    @DisplayName("Get Subscriptions Due For Billing Tests")
    class GetSubscriptionsDueForBillingTests {

        @Test
        @DisplayName("Should retrieve subscriptions due for billing")
        void shouldRetrieveSubscriptionsDueForBilling() throws Exception {
            // Given
            Subscription subscription1 = Subscription.builder()
                .id(UUID.randomUUID())
                .tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .build();

            Subscription subscription2 = Subscription.builder()
                .id(UUID.randomUUID())
                .tier(SubscriptionTier.AI_PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .build();

            List<Subscription> dueSubscriptions = List.of(subscription1, subscription2);

            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    Supplier<Result<List<Subscription>, String>> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

            when(subscriptionRepository.findSubscriptionsDueForBilling(any(LocalDateTime.class)))
                .thenReturn(dueSubscriptions);

            // When
            CompletableFuture<Result<List<Subscription>, String>> result =
                billingService.getSubscriptionsDueForBilling();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).hasSize(2);
            assertThat(result.get().getValue()).containsExactlyInAnyOrder(subscription1, subscription2);

            verify(subscriptionRepository).findSubscriptionsDueForBilling(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return empty list when no subscriptions due")
        void shouldReturnEmptyListWhenNoSubscriptionsDue() throws Exception {
            // Given
            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    Supplier<Result<List<Subscription>, String>> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

            when(subscriptionRepository.findSubscriptionsDueForBilling(any(LocalDateTime.class)))
                .thenReturn(List.of());

            // When
            CompletableFuture<Result<List<Subscription>, String>> result =
                billingService.getSubscriptionsDueForBilling();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEmpty();
        }

        @Test
        @DisplayName("Should handle database error when fetching due subscriptions")
        void shouldHandleDatabaseError() throws Exception {
            // Given
            when(databaseCircuitBreaker.executeSupplier(any()))
                .thenReturn(Result.failure("Database connection timeout"));

            // When
            CompletableFuture<Result<List<Subscription>, String>> result =
                billingService.getSubscriptionsDueForBilling();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("timeout");
        }
    }
}
