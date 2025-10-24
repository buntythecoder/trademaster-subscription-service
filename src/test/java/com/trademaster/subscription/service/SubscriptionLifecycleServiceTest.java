package com.trademaster.subscription.service;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Subscription Lifecycle Service Unit Tests
 *
 * MANDATORY: Functional Programming patterns - Uses Result<T,E> pattern
 * MANDATORY: Java 24 Virtual Threads - CompletableFuture with async operations
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Lifecycle Service Tests")
class SubscriptionLifecycleServiceTest {

    @Mock
    private SubscriptionCreator subscriptionCreator;

    @Mock
    private SubscriptionActivator subscriptionActivator;

    @Mock
    private SubscriptionCancellationService subscriptionCancellationService;

    @Mock
    private SubscriptionSuspender subscriptionSuspender;

    @Mock
    private SubscriptionResumer subscriptionResumer;

    @Mock
    private SubscriptionStateManager subscriptionStateManager;

    @InjectMocks
    private SubscriptionLifecycleService lifecycleService;

    private UUID testUserId;
    private UUID testSubscriptionId;
    private UUID testPaymentTransactionId;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSubscriptionId = UUID.randomUUID();
        testPaymentTransactionId = UUID.randomUUID();

        testSubscription = Subscription.builder()
            .id(testSubscriptionId)
            .userId(testUserId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
    }

    @Nested
    @DisplayName("Create Subscription Tests")
    class CreateSubscriptionTests {

        @Test
        @DisplayName("Should successfully create subscription with trial")
        void shouldCreateSubscriptionWithTrial() throws Exception {
            // Given
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(subscriptionCreator.createSubscription(
                eq(testUserId),
                eq(SubscriptionTier.PRO),
                eq(BillingCycle.MONTHLY),
                eq(true)
            )).thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.createSubscription(testUserId, SubscriptionTier.PRO, BillingCycle.MONTHLY, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEqualTo(testSubscription);

            verify(subscriptionCreator).createSubscription(testUserId, SubscriptionTier.PRO, BillingCycle.MONTHLY, true);
            verifyNoMoreInteractions(subscriptionCreator);
        }

        @Test
        @DisplayName("Should successfully create subscription without trial")
        void shouldCreateSubscriptionWithoutTrial() throws Exception {
            // Given
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(subscriptionCreator.createSubscription(
                eq(testUserId),
                eq(SubscriptionTier.FREE),
                eq(BillingCycle.MONTHLY),
                eq(false)
            )).thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.createSubscription(testUserId, SubscriptionTier.FREE, BillingCycle.MONTHLY, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(subscriptionCreator).createSubscription(testUserId, SubscriptionTier.FREE, BillingCycle.MONTHLY, false);
        }

        @Test
        @DisplayName("Should handle creation failure")
        void shouldHandleCreationFailure() throws Exception {
            // Given
            String errorMessage = "User already has active subscription";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(subscriptionCreator.createSubscription(any(), any(), any(), anyBoolean()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.createSubscription(testUserId, SubscriptionTier.PRO, BillingCycle.MONTHLY, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Activate Subscription Tests")
    class ActivateSubscriptionTests {

        @Test
        @DisplayName("Should successfully activate subscription")
        void shouldActivateSubscription() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(subscriptionActivator.activateSubscription(testSubscriptionId, testPaymentTransactionId))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.activateSubscription(testSubscriptionId, testPaymentTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            verify(subscriptionActivator).activateSubscription(testSubscriptionId, testPaymentTransactionId);
        }

        @Test
        @DisplayName("Should handle activation failure")
        void shouldHandleActivationFailure() throws Exception {
            // Given
            String errorMessage = "Subscription not found";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(subscriptionActivator.activateSubscription(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.activateSubscription(testSubscriptionId, testPaymentTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Cancel Subscription Tests")
    class CancelSubscriptionTests {

        @Test
        @DisplayName("Should successfully cancel subscription")
        void shouldCancelSubscription() throws Exception {
            // Given
            String cancellationReason = "User requested cancellation";
            testSubscription.setStatus(SubscriptionStatus.CANCELLED);
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(subscriptionCancellationService.cancelSubscription(testSubscriptionId, cancellationReason))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.cancelSubscription(testSubscriptionId, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

            verify(subscriptionCancellationService).cancelSubscription(testSubscriptionId, cancellationReason);
        }

        @Test
        @DisplayName("Should handle cancellation failure")
        void shouldHandleCancellationFailure() throws Exception {
            // Given
            String errorMessage = "Cannot cancel already cancelled subscription";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(subscriptionCancellationService.cancelSubscription(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.cancelSubscription(testSubscriptionId, "reason");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Suspend Subscription Tests")
    class SuspendSubscriptionTests {

        @Test
        @DisplayName("Should successfully suspend subscription")
        void shouldSuspendSubscription() throws Exception {
            // Given
            String suspensionReason = "Payment failed";
            testSubscription.setStatus(SubscriptionStatus.SUSPENDED);
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(subscriptionSuspender.suspendSubscription(testSubscriptionId, suspensionReason))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.suspendSubscription(testSubscriptionId, suspensionReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);

            verify(subscriptionSuspender).suspendSubscription(testSubscriptionId, suspensionReason);
        }
    }

    @Nested
    @DisplayName("Resume Subscription Tests")
    class ResumeSubscriptionTests {

        @Test
        @DisplayName("Should successfully resume subscription")
        void shouldResumeSubscription() throws Exception {
            // Given
            testSubscription.setStatus(SubscriptionStatus.ACTIVE);
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(testSubscription));

            when(subscriptionResumer.resumeSubscription(testSubscriptionId))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.resumeSubscription(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            verify(subscriptionResumer).resumeSubscription(testSubscriptionId);
        }

        @Test
        @DisplayName("Should handle resume failure")
        void shouldHandleResumeFailure() throws Exception {
            // Given
            String errorMessage = "Cannot resume cancelled subscription";
            CompletableFuture<Result<Subscription, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(subscriptionResumer.resumeSubscription(any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Subscription, String>> result =
                lifecycleService.resumeSubscription(testSubscriptionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Query Operations Tests")
    class QueryOperationsTests {

        @Test
        @DisplayName("Should get active subscription by user ID")
        void shouldGetActiveSubscription() throws Exception {
            // Given
            when(subscriptionStateManager.getActiveSubscription(testUserId))
                .thenReturn(CompletableFuture.completedFuture(Result.success(Optional.of(testSubscription))));

            // When
            CompletableFuture<Result<Optional<Subscription>, String>> result =
                lifecycleService.getActiveSubscription(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isPresent();
            assertThat(result.get().getValue().get()).isEqualTo(testSubscription);

            verify(subscriptionStateManager).getActiveSubscription(testUserId);
        }

        @Test
        @DisplayName("Should return empty when no active subscription")
        void shouldReturnEmptyWhenNoActiveSubscription() throws Exception {
            // Given
            when(subscriptionStateManager.getActiveSubscription(testUserId))
                .thenReturn(CompletableFuture.completedFuture(Result.success(Optional.empty())));

            // When
            CompletableFuture<Result<Optional<Subscription>, String>> result =
                lifecycleService.getActiveSubscription(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEmpty();
        }
    }
}
