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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Subscription Notification Service Unit Tests
 *
 * MANDATORY: Functional Programming patterns - Uses Result<T,E> pattern
 * MANDATORY: Java 24 Virtual Threads - CompletableFuture with async operations
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Notification Service Tests")
class SubscriptionNotificationServiceTest {

    @Mock
    private SubscriptionEventPublisher eventPublisher;

    @Mock
    private BatchNotificationProcessor batchProcessor;

    @InjectMocks
    private SubscriptionNotificationService notificationService;

    private Subscription testSubscription;
    private UUID testUserId;
    private UUID testTransactionId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testTransactionId = UUID.randomUUID();

        testSubscription = Subscription.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
    }

    @Nested
    @DisplayName("Publish Subscription Created Tests")
    class PublishSubscriptionCreatedTests {

        @Test
        @DisplayName("Should successfully publish subscription created event")
        void shouldPublishSubscriptionCreatedEvent() throws Exception {
            // Given
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(null));

            when(eventPublisher.publishCreated(testSubscription))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionCreated(testSubscription);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(eventPublisher).publishCreated(testSubscription);
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should handle publishing failure")
        void shouldHandlePublishingFailure() throws Exception {
            // Given
            String errorMessage = "Event bus unavailable";
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(eventPublisher.publishCreated(any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionCreated(testSubscription);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("unavailable");
        }
    }

    @Nested
    @DisplayName("Publish Subscription Activated Tests")
    class PublishSubscriptionActivatedTests {

        @Test
        @DisplayName("Should successfully publish subscription activated event")
        void shouldPublishSubscriptionActivatedEvent() throws Exception {
            // Given
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(null));

            when(eventPublisher.publishActivated(testSubscription))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionActivated(testSubscription);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(eventPublisher).publishActivated(testSubscription);
        }

        @Test
        @DisplayName("Should handle activation event failure")
        void shouldHandleActivationEventFailure() throws Exception {
            // Given
            String errorMessage = "Failed to publish activation event";
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(eventPublisher.publishActivated(any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionActivated(testSubscription);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("activation");
        }
    }

    @Nested
    @DisplayName("Publish Subscription Upgraded Tests")
    class PublishSubscriptionUpgradedTests {

        @Test
        @DisplayName("Should successfully publish subscription upgraded event")
        void shouldPublishSubscriptionUpgradedEvent() throws Exception {
            // Given
            SubscriptionTier previousTier = SubscriptionTier.FREE;
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(null));

            when(eventPublisher.publishUpgraded(testSubscription, previousTier))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionUpgraded(testSubscription, previousTier);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(eventPublisher).publishUpgraded(testSubscription, previousTier);
        }

        @Test
        @DisplayName("Should handle upgrade event failure")
        void shouldHandleUpgradeEventFailure() throws Exception {
            // Given
            String errorMessage = "Failed to publish upgrade event";
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(eventPublisher.publishUpgraded(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionUpgraded(testSubscription, SubscriptionTier.FREE);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("upgrade");
        }
    }

    @Nested
    @DisplayName("Publish Subscription Cancelled Tests")
    class PublishSubscriptionCancelledTests {

        @Test
        @DisplayName("Should successfully publish subscription cancelled event")
        void shouldPublishSubscriptionCancelledEvent() throws Exception {
            // Given
            String cancellationReason = "User requested cancellation";
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(null));

            when(eventPublisher.publishCancelled(testSubscription, cancellationReason))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionCancelled(testSubscription, cancellationReason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(eventPublisher).publishCancelled(testSubscription, cancellationReason);
        }

        @Test
        @DisplayName("Should handle cancellation event failure")
        void shouldHandleCancellationEventFailure() throws Exception {
            // Given
            String errorMessage = "Failed to publish cancellation event";
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(eventPublisher.publishCancelled(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionCancelled(testSubscription, "reason");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("cancellation");
        }
    }

    @Nested
    @DisplayName("Publish Subscription Billed Tests")
    class PublishSubscriptionBilledTests {

        @Test
        @DisplayName("Should successfully publish subscription billed event")
        void shouldPublishSubscriptionBilledEvent() throws Exception {
            // Given
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(null));

            when(eventPublisher.publishBilled(testSubscription, testTransactionId))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionBilled(testSubscription, testTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();

            verify(eventPublisher).publishBilled(testSubscription, testTransactionId);
        }

        @Test
        @DisplayName("Should handle billing event failure")
        void shouldHandleBillingEventFailure() throws Exception {
            // Given
            String errorMessage = "Failed to publish billing event";
            CompletableFuture<Result<Void, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(eventPublisher.publishBilled(any(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<Void, String>> result =
                notificationService.publishSubscriptionBilled(testSubscription, testTransactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("billing");
        }
    }

    @Nested
    @DisplayName("Publish Batch Notifications Tests")
    class PublishBatchNotificationsTests {

        @Test
        @DisplayName("Should successfully publish batch notifications")
        void shouldPublishBatchNotifications() throws Exception {
            // Given
            Subscription sub1 = Subscription.builder()
                .id(UUID.randomUUID())
                .tier(SubscriptionTier.PRO)
                .status(SubscriptionStatus.ACTIVE)
                .build();

            Subscription sub2 = Subscription.builder()
                .id(UUID.randomUUID())
                .tier(SubscriptionTier.AI_PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .build();

            List<Subscription> subscriptions = List.of(sub1, sub2);
            List<UUID> processedIds = List.of(sub1.getId(), sub2.getId());
            String eventType = "BILLING_REMINDER";

            CompletableFuture<Result<List<UUID>, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(processedIds));

            when(batchProcessor.processBatchNotifications(subscriptions, eventType))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<List<UUID>, String>> result =
                notificationService.publishBatchNotifications(subscriptions, eventType);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).hasSize(2);
            assertThat(result.get().getValue()).containsExactlyInAnyOrder(sub1.getId(), sub2.getId());

            verify(batchProcessor).processBatchNotifications(subscriptions, eventType);
        }

        @Test
        @DisplayName("Should handle empty subscription list")
        void shouldHandleEmptySubscriptionList() throws Exception {
            // Given
            List<Subscription> emptyList = List.of();
            String eventType = "BILLING_REMINDER";

            CompletableFuture<Result<List<UUID>, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(List.of()));

            when(batchProcessor.processBatchNotifications(emptyList, eventType))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<List<UUID>, String>> result =
                notificationService.publishBatchNotifications(emptyList, eventType);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).isEmpty();
        }

        @Test
        @DisplayName("Should handle batch processing failure")
        void shouldHandleBatchProcessingFailure() throws Exception {
            // Given
            String errorMessage = "Batch processing failed - event bus timeout";
            CompletableFuture<Result<List<UUID>, String>> expectedResult =
                CompletableFuture.completedFuture(Result.failure(errorMessage));

            when(batchProcessor.processBatchNotifications(anyList(), any()))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<List<UUID>, String>> result =
                notificationService.publishBatchNotifications(List.of(testSubscription), "BILLING_REMINDER");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isFailure()).isTrue();
            assertThat(result.get().getError()).contains("timeout");
        }

        @Test
        @DisplayName("Should handle partial batch processing success")
        void shouldHandlePartialBatchProcessingSuccess() throws Exception {
            // Given
            Subscription sub1 = Subscription.builder()
                .id(UUID.randomUUID())
                .tier(SubscriptionTier.PRO)
                .build();

            Subscription sub2 = Subscription.builder()
                .id(UUID.randomUUID())
                .tier(SubscriptionTier.AI_PREMIUM)
                .build();

            List<Subscription> subscriptions = List.of(sub1, sub2);
            List<UUID> partialIds = List.of(sub1.getId()); // Only one succeeded

            CompletableFuture<Result<List<UUID>, String>> expectedResult =
                CompletableFuture.completedFuture(Result.success(partialIds));

            when(batchProcessor.processBatchNotifications(subscriptions, "EXPIRY_WARNING"))
                .thenReturn(expectedResult);

            // When
            CompletableFuture<Result<List<UUID>, String>> result =
                notificationService.publishBatchNotifications(subscriptions, "EXPIRY_WARNING");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get().isSuccess()).isTrue();
            assertThat(result.get().getValue()).hasSize(1);
            assertThat(result.get().getValue()).contains(sub1.getId());
        }
    }
}
