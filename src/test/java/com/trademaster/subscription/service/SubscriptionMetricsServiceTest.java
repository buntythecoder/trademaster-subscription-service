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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Subscription Metrics Service Unit Tests
 *
 * MANDATORY: Facade Pattern - Verifies delegation to specialized metrics services
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Metrics Service Tests")
class SubscriptionMetricsServiceTest {

    @Mock
    private SubscriptionMetricsRecorder metricsRecorder;

    @Mock
    private PerformanceTimerService timerService;

    @InjectMocks
    private SubscriptionMetricsService metricsService;

    private Timer.Sample testTimer;

    @BeforeEach
    void setUp() {
        testTimer = mock(Timer.Sample.class);
    }

    @Nested
    @DisplayName("Business Metrics Recording Tests")
    class BusinessMetricsTests {

        @Test
        @DisplayName("Should record subscription created")
        void shouldRecordSubscriptionCreated() {
            // Given
            String tier = "PRO";

            // When
            metricsService.recordSubscriptionCreated(tier);

            // Then
            verify(metricsRecorder).recordSubscriptionCreated(tier);
            verifyNoMoreInteractions(metricsRecorder);
        }

        @Test
        @DisplayName("Should record subscription activated")
        void shouldRecordSubscriptionActivated() {
            // Given
            String tier = "PRO";
            String billingCycle = "MONTHLY";

            // When
            metricsService.recordSubscriptionActivated(tier, billingCycle);

            // Then
            verify(metricsRecorder).recordSubscriptionActivated(tier, billingCycle);
        }

        @Test
        @DisplayName("Should record subscription cancelled")
        void shouldRecordSubscriptionCancelled() {
            // Given
            String tier = "PRO";
            String reason = "User requested cancellation";

            // When
            metricsService.recordSubscriptionCancelled(tier, reason);

            // Then
            verify(metricsRecorder).recordSubscriptionCancelled(tier, reason);
        }

        @Test
        @DisplayName("Should record subscription upgraded")
        void shouldRecordSubscriptionUpgraded() {
            // Given
            String fromTier = "FREE";
            String toTier = "PRO";

            // When
            metricsService.recordSubscriptionUpgraded(fromTier, toTier);

            // Then
            verify(metricsRecorder).recordSubscriptionUpgraded(fromTier, toTier);
        }

        @Test
        @DisplayName("Should record subscription downgraded")
        void shouldRecordSubscriptionDowngraded() {
            // Given
            String fromTier = "AI_PREMIUM";
            String toTier = "PRO";

            // When
            metricsService.recordSubscriptionDowngraded(fromTier, toTier);

            // Then
            verify(metricsRecorder).recordSubscriptionDowngraded(fromTier, toTier);
        }
    }

    @Nested
    @DisplayName("Billing Metrics Tests")
    class BillingMetricsTests {

        @Test
        @DisplayName("Should record billing successful")
        void shouldRecordBillingSuccessful() {
            // Given
            String tier = "PRO";
            String billingCycle = "MONTHLY";
            long processingTimeMs = 150L;

            // When
            metricsService.recordBillingSuccessful(tier, billingCycle, processingTimeMs);

            // Then
            verify(metricsRecorder).recordBillingSuccessful(tier, billingCycle, processingTimeMs);
        }

        @Test
        @DisplayName("Should record billing failed")
        void shouldRecordBillingFailed() {
            // Given
            String tier = "PRO";
            String billingCycle = "MONTHLY";
            String errorType = "PAYMENT_DECLINED";

            // When
            metricsService.recordBillingFailed(tier, billingCycle, errorType);

            // Then
            verify(metricsRecorder).recordBillingFailed(tier, billingCycle, errorType);
        }

        @Test
        @DisplayName("Should record billing failed with different error types")
        void shouldRecordBillingFailedWithDifferentErrors() {
            // Given
            String[] errorTypes = {"PAYMENT_DECLINED", "CARD_EXPIRED", "INSUFFICIENT_FUNDS", "NETWORK_ERROR"};

            // When & Then
            for (String errorType : errorTypes) {
                metricsService.recordBillingFailed("PRO", "MONTHLY", errorType);
                verify(metricsRecorder).recordBillingFailed("PRO", "MONTHLY", errorType);
            }
        }
    }

    @Nested
    @DisplayName("Usage Metrics Tests")
    class UsageMetricsTests {

        @Test
        @DisplayName("Should record usage limit exceeded")
        void shouldRecordUsageLimitExceeded() {
            // Given
            String userId = "user-123";
            String feature = "api_calls";
            String tier = "PRO";

            // When
            metricsService.recordUsageLimitExceeded(userId, feature, tier);

            // Then
            verify(metricsRecorder).recordUsageLimitExceeded(userId, feature, tier);
        }

        @Test
        @DisplayName("Should record multiple usage limit exceeded events")
        void shouldRecordMultipleUsageLimitExceeded() {
            // Given
            String[] features = {"api_calls", "storage_gb", "concurrent_users", "data_exports"};

            // When & Then
            for (String feature : features) {
                metricsService.recordUsageLimitExceeded("user-123", feature, "PRO");
                verify(metricsRecorder).recordUsageLimitExceeded("user-123", feature, "PRO");
            }
        }
    }

    @Nested
    @DisplayName("Conversion and Churn Metrics Tests")
    class ConversionChurnMetricsTests {

        @Test
        @DisplayName("Should record trial conversion success")
        void shouldRecordTrialConversionSuccess() {
            // Given
            String tier = "PRO";
            boolean converted = true;

            // When
            metricsService.recordTrialConversion(tier, converted);

            // Then
            verify(metricsRecorder).recordTrialConversion(tier, converted);
        }

        @Test
        @DisplayName("Should record trial conversion failure")
        void shouldRecordTrialConversionFailure() {
            // Given
            String tier = "PRO";
            boolean converted = false;

            // When
            metricsService.recordTrialConversion(tier, converted);

            // Then
            verify(metricsRecorder).recordTrialConversion(tier, converted);
        }

        @Test
        @DisplayName("Should record churn event")
        void shouldRecordChurnEvent() {
            // Given
            String tier = "PRO";
            String churnReason = "Price too high";
            long subscriptionAgeInDays = 90L;

            // When
            metricsService.recordChurnEvent(tier, churnReason, subscriptionAgeInDays);

            // Then
            verify(metricsRecorder).recordChurnEvent(tier, churnReason, subscriptionAgeInDays);
        }

        @Test
        @DisplayName("Should record churn with various reasons")
        void shouldRecordChurnWithVariousReasons() {
            // Given
            String[] churnReasons = {
                "Price too high",
                "Missing features",
                "Poor support",
                "Switching to competitor",
                "No longer needed"
            };

            // When & Then
            for (int i = 0; i < churnReasons.length; i++) {
                long age = 30L + (i * 10);
                metricsService.recordChurnEvent("PRO", churnReasons[i], age);
                verify(metricsRecorder).recordChurnEvent("PRO", churnReasons[i], age);
            }
        }
    }

    @Nested
    @DisplayName("Price Change Metrics Tests")
    class PriceChangeMetricsTests {

        @Test
        @DisplayName("Should record price increase")
        void shouldRecordPriceIncrease() {
            // Given
            String tier = "PRO";
            double oldPrice = 29.99;
            double newPrice = 34.99;

            // When
            metricsService.recordPriceChange(tier, oldPrice, newPrice);

            // Then
            verify(metricsRecorder).recordPriceChange(tier, oldPrice, newPrice);
        }

        @Test
        @DisplayName("Should record price decrease")
        void shouldRecordPriceDecrease() {
            // Given
            String tier = "PRO";
            double oldPrice = 34.99;
            double newPrice = 29.99;

            // When
            metricsService.recordPriceChange(tier, oldPrice, newPrice);

            // Then
            verify(metricsRecorder).recordPriceChange(tier, oldPrice, newPrice);
        }
    }

    @Nested
    @DisplayName("Performance Timer Tests")
    class PerformanceTimerTests {

        @Test
        @DisplayName("Should start subscription processing timer")
        void shouldStartSubscriptionProcessingTimer() {
            // Given
            when(timerService.startSubscriptionProcessingTimer())
                .thenReturn(testTimer);

            // When
            Timer.Sample result = metricsService.startSubscriptionProcessingTimer();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testTimer);
            verify(timerService).startSubscriptionProcessingTimer();
        }

        @Test
        @DisplayName("Should record subscription processing time")
        void shouldRecordSubscriptionProcessingTime() {
            // Given
            String operation = "create_subscription";

            // When
            metricsService.recordSubscriptionProcessingTime(testTimer, operation);

            // Then
            verify(timerService).recordSubscriptionProcessingTime(testTimer, operation);
        }

        @Test
        @DisplayName("Should start billing processing timer")
        void shouldStartBillingProcessingTimer() {
            // Given
            when(timerService.startBillingProcessingTimer())
                .thenReturn(testTimer);

            // When
            Timer.Sample result = metricsService.startBillingProcessingTimer();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testTimer);
            verify(timerService).startBillingProcessingTimer();
        }

        @Test
        @DisplayName("Should record billing processing time")
        void shouldRecordBillingProcessingTime() {
            // Given
            String operation = "process_payment";

            // When
            metricsService.recordBillingProcessingTime(testTimer, operation);

            // Then
            verify(timerService).recordBillingProcessingTime(testTimer, operation);
        }

        @Test
        @DisplayName("Should start usage check timer")
        void shouldStartUsageCheckTimer() {
            // Given
            when(timerService.startUsageCheckTimer())
                .thenReturn(testTimer);

            // When
            Timer.Sample result = metricsService.startUsageCheckTimer();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testTimer);
            verify(timerService).startUsageCheckTimer();
        }

        @Test
        @DisplayName("Should record usage check time")
        void shouldRecordUsageCheckTime() {
            // Given
            String feature = "api_calls";

            // When
            metricsService.recordUsageCheckTime(testTimer, feature);

            // Then
            verify(timerService).recordUsageCheckTime(testTimer, feature);
        }

        @Test
        @DisplayName("Should start notification processing timer")
        void shouldStartNotificationProcessingTimer() {
            // Given
            when(timerService.startNotificationProcessingTimer())
                .thenReturn(testTimer);

            // When
            Timer.Sample result = metricsService.startNotificationProcessingTimer();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testTimer);
            verify(timerService).startNotificationProcessingTimer();
        }
    }

    @Nested
    @DisplayName("Gauge Update Tests")
    class GaugeUpdateTests {

        @Test
        @DisplayName("Should update active subscriptions count")
        void shouldUpdateActiveSubscriptions() {
            // Given
            long count = 1000L;

            // When
            metricsService.updateActiveSubscriptions(count);

            // Then
            verify(timerService).updateActiveSubscriptions(count);
        }

        @Test
        @DisplayName("Should update trial subscriptions count")
        void shouldUpdateTrialSubscriptions() {
            // Given
            long count = 150L;

            // When
            metricsService.updateTrialSubscriptions(count);

            // Then
            verify(timerService).updateTrialSubscriptions(count);
        }

        @Test
        @DisplayName("Should update suspended subscriptions count")
        void shouldUpdateSuspendedSubscriptions() {
            // Given
            long count = 25L;

            // When
            metricsService.updateSuspendedSubscriptions(count);

            // Then
            verify(timerService).updateSuspendedSubscriptions(count);
        }

        @Test
        @DisplayName("Should update monthly recurring revenue")
        void shouldUpdateMonthlyRecurringRevenue() {
            // Given
            long mrr = 50000L;

            // When
            metricsService.updateMonthlyRecurringRevenue(mrr);

            // Then
            verify(timerService).updateMonthlyRecurringRevenue(mrr);
        }

        @Test
        @DisplayName("Should update annual recurring revenue")
        void shouldUpdateAnnualRecurringRevenue() {
            // Given
            long arr = 600000L;

            // When
            metricsService.updateAnnualRecurringRevenue(arr);

            // Then
            verify(timerService).updateAnnualRecurringRevenue(arr);
        }

        @Test
        @DisplayName("Should update all gauges in sequence")
        void shouldUpdateAllGaugesInSequence() {
            // When
            metricsService.updateActiveSubscriptions(1000L);
            metricsService.updateTrialSubscriptions(150L);
            metricsService.updateSuspendedSubscriptions(25L);
            metricsService.updateMonthlyRecurringRevenue(50000L);
            metricsService.updateAnnualRecurringRevenue(600000L);

            // Then
            verify(timerService).updateActiveSubscriptions(1000L);
            verify(timerService).updateTrialSubscriptions(150L);
            verify(timerService).updateSuspendedSubscriptions(25L);
            verify(timerService).updateMonthlyRecurringRevenue(50000L);
            verify(timerService).updateAnnualRecurringRevenue(600000L);
        }
    }

    @Nested
    @DisplayName("Integration Workflow Tests")
    class IntegrationWorkflowTests {

        @Test
        @DisplayName("Should track complete subscription creation workflow")
        void shouldTrackCompleteSubscriptionCreationWorkflow() {
            // Given
            when(timerService.startSubscriptionProcessingTimer()).thenReturn(testTimer);

            // When - Simulate complete workflow
            Timer.Sample timer = metricsService.startSubscriptionProcessingTimer();
            metricsService.recordSubscriptionCreated("PRO");
            metricsService.recordSubscriptionActivated("PRO", "MONTHLY");
            metricsService.updateActiveSubscriptions(1001L);
            metricsService.updateMonthlyRecurringRevenue(30000L);
            metricsService.recordSubscriptionProcessingTime(timer, "create_subscription");

            // Then - Verify all metrics recorded
            verify(timerService).startSubscriptionProcessingTimer();
            verify(metricsRecorder).recordSubscriptionCreated("PRO");
            verify(metricsRecorder).recordSubscriptionActivated("PRO", "MONTHLY");
            verify(timerService).updateActiveSubscriptions(1001L);
            verify(timerService).updateMonthlyRecurringRevenue(30000L);
            verify(timerService).recordSubscriptionProcessingTime(timer, "create_subscription");
        }

        @Test
        @DisplayName("Should track complete upgrade workflow")
        void shouldTrackCompleteUpgradeWorkflow() {
            // Given
            when(timerService.startSubscriptionProcessingTimer()).thenReturn(testTimer);

            // When - Simulate upgrade workflow
            Timer.Sample timer = metricsService.startSubscriptionProcessingTimer();
            metricsService.recordSubscriptionUpgraded("FREE", "PRO");
            metricsService.updateMonthlyRecurringRevenue(35000L);
            metricsService.recordSubscriptionProcessingTime(timer, "upgrade_subscription");

            // Then
            verify(timerService).startSubscriptionProcessingTimer();
            verify(metricsRecorder).recordSubscriptionUpgraded("FREE", "PRO");
            verify(timerService).updateMonthlyRecurringRevenue(35000L);
            verify(timerService).recordSubscriptionProcessingTime(timer, "upgrade_subscription");
        }

        @Test
        @DisplayName("Should track complete cancellation workflow")
        void shouldTrackCompleteCancellationWorkflow() {
            // Given
            when(timerService.startSubscriptionProcessingTimer()).thenReturn(testTimer);

            // When - Simulate cancellation workflow
            Timer.Sample timer = metricsService.startSubscriptionProcessingTimer();
            metricsService.recordSubscriptionCancelled("PRO", "Price too high");
            metricsService.recordChurnEvent("PRO", "Price too high", 90L);
            metricsService.updateActiveSubscriptions(999L);
            metricsService.updateMonthlyRecurringRevenue(29000L);
            metricsService.recordSubscriptionProcessingTime(timer, "cancel_subscription");

            // Then
            verify(timerService).startSubscriptionProcessingTimer();
            verify(metricsRecorder).recordSubscriptionCancelled("PRO", "Price too high");
            verify(metricsRecorder).recordChurnEvent("PRO", "Price too high", 90L);
            verify(timerService).updateActiveSubscriptions(999L);
            verify(timerService).updateMonthlyRecurringRevenue(29000L);
            verify(timerService).recordSubscriptionProcessingTime(timer, "cancel_subscription");
        }
    }
}
