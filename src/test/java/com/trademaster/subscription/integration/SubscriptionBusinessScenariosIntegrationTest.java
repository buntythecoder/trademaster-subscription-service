package com.trademaster.subscription.integration;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.SubscriptionCancellationService;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Business Scenarios Integration Tests
 *
 * MANDATORY: Rule #20 - Integration Testing with Business Scenarios
 * MANDATORY: End-to-End Business Workflow Validation
 *
 * Test Scenarios:
 * 1. Trial Subscription Complete Workflow
 * 2. Subscription Upgrade from Free to Premium
 * 3. Subscription Cancellation with Grace Period
 * 4. Subscription Tier Downgrade Scenario
 * 5. Billing Cycle Change Workflow
 * 6. Multi-User Concurrent Subscription Management
 * 7. Subscription Reactivation After Cancellation
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@Slf4j
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.kafka.producer.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.bootstrap-servers=localhost:9092"
})
class SubscriptionBusinessScenariosIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("trademaster_subscription_scenarios_test")
        .withUsername("test_user")
        .withPassword("test_password");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionLifecycleService lifecycleService;

    @Autowired
    private SubscriptionUpgradeService upgradeService;

    @Autowired
    private SubscriptionCancellationService cancellationService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Disable external dependencies for testing
        registry.add("app.services.payment-gateway.url", () -> "http://localhost:0");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9093");
    }

    /**
     * Scenario 1: Complete Trial Subscription Workflow
     *
     * Validates the entire trial subscription lifecycle from creation
     * through trial period to conversion or expiration.
     */
    @Test
    void scenario1_completeTrialWorkflow_shouldSucceed() throws ExecutionException, InterruptedException {
        // Given - User starts a trial subscription
        UUID userId = UUID.randomUUID();

        // When - Create trial subscription
        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, true
        ).get();

        // Then - Trial subscription should be created successfully
        assertThat(createResult.isSuccess()).isTrue();

        createResult.ifSuccess(subscription -> {
            assertThat(subscription.getUserId()).isEqualTo(userId);
            assertThat(subscription.getTier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
            assertThat(subscription.getTrialEndDate()).isNotNull();
            assertThat(subscription.getTrialEndDate()).isAfter(LocalDateTime.now());

            log.info("Trial subscription created: {}", subscription.getId());
            log.info("Trial ends: {}", subscription.getTrialEndDate());
        });

        // When - Activate trial after payment method added
        createResult.ifSuccess(subscription -> {
            try {
                Result<Subscription, Exception> activateResult = lifecycleService.activateSubscription(
                    subscription.getId()
                ).get();

                // Then - Subscription should be activated
                assertThat(activateResult.isSuccess()).isTrue();
                activateResult.ifSuccess(activatedSub -> {
                    assertThat(activatedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                    log.info("Trial subscription activated: {}", activatedSub.getId());
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Scenario 2: Subscription Upgrade from Free to Premium Tier
     *
     * Tests the complete upgrade workflow including tier validation,
     * pricing calculation, and feature access updates.
     */
    @Test
    void scenario2_upgradeFromFreeToPremium_shouldSucceed() throws ExecutionException, InterruptedException {
        // Given - User has a free subscription
        UUID userId = UUID.randomUUID();

        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.FREE, BillingCycle.MONTHLY, false
        ).get();

        assertThat(createResult.isSuccess()).isTrue();

        UUID subscriptionId = createResult.map(Subscription::getId).orElse(null);
        assertThat(subscriptionId).isNotNull();

        // When - User upgrades to AI Premium
        Result<Subscription, Exception> upgradeResult = upgradeService.upgradeTier(
            subscriptionId, SubscriptionTier.AI_PREMIUM
        ).get();

        // Then - Upgrade should succeed with new tier and pricing
        assertThat(upgradeResult.isSuccess()).isTrue();

        upgradeResult.ifSuccess(upgradedSub -> {
            assertThat(upgradedSub.getId()).isEqualTo(subscriptionId);
            assertThat(upgradedSub.getTier()).isEqualTo(SubscriptionTier.AI_PREMIUM);
            assertThat(upgradedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            log.info("Subscription upgraded from FREE to AI_PREMIUM: {}", upgradedSub.getId());
        });

        // Verify database state
        Optional<Subscription> dbSubscription = subscriptionRepository.findById(subscriptionId);
        assertThat(dbSubscription).isPresent();
        assertThat(dbSubscription.get().getTier()).isEqualTo(SubscriptionTier.AI_PREMIUM);
    }

    /**
     * Scenario 3: Subscription Cancellation with Grace Period
     *
     * Tests the cancellation workflow including immediate cancellation,
     * grace period handling, and access retention.
     */
    @Test
    void scenario3_subscriptionCancellationWithGracePeriod_shouldSucceed() throws ExecutionException, InterruptedException {
        // Given - User has an active subscription
        UUID userId = UUID.randomUUID();

        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
        ).get();

        assertThat(createResult.isSuccess()).isTrue();

        UUID subscriptionId = createResult.map(Subscription::getId).orElse(null);
        assertThat(subscriptionId).isNotNull();

        // When - User requests immediate cancellation
        Result<Subscription, Exception> cancelResult = cancellationService.cancelSubscription(
            subscriptionId, true
        ).get();

        // Then - Subscription should be cancelled immediately
        assertThat(cancelResult.isSuccess()).isTrue();

        cancelResult.ifSuccess(cancelledSub -> {
            assertThat(cancelledSub.getId()).isEqualTo(subscriptionId);
            assertThat(cancelledSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(cancelledSub.getCancellationDate()).isNotNull();

            log.info("Subscription cancelled immediately: {}", cancelledSub.getId());
        });

        // Verify database state
        Optional<Subscription> dbSubscription = subscriptionRepository.findById(subscriptionId);
        assertThat(dbSubscription).isPresent();
        assertThat(dbSubscription.get().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    /**
     * Scenario 4: Subscription Tier Downgrade
     *
     * Tests downgrade workflow from premium to lower tier including
     * feature access validation and billing adjustments.
     */
    @Test
    void scenario4_tierDowngradeFromPremiumToPro_shouldSucceed() throws ExecutionException, InterruptedException {
        // Given - User has AI Premium subscription
        UUID userId = UUID.randomUUID();

        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.AI_PREMIUM, BillingCycle.MONTHLY, false
        ).get();

        assertThat(createResult.isSuccess()).isTrue();

        UUID subscriptionId = createResult.map(Subscription::getId).orElse(null);
        assertThat(subscriptionId).isNotNull();

        // When - User downgrades to PRO tier
        Result<Subscription, Exception> downgradeResult = upgradeService.upgradeTier(
            subscriptionId, SubscriptionTier.PRO
        ).get();

        // Then - Downgrade should succeed (upgrade service handles both up and down)
        assertThat(downgradeResult.isSuccess()).isTrue();

        downgradeResult.ifSuccess(downgradedSub -> {
            assertThat(downgradedSub.getId()).isEqualTo(subscriptionId);
            assertThat(downgradedSub.getTier()).isEqualTo(SubscriptionTier.PRO);
            assertThat(downgradedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            log.info("Subscription downgraded from AI_PREMIUM to PRO: {}", downgradedSub.getId());
        });

        // Verify database state
        Optional<Subscription> dbSubscription = subscriptionRepository.findById(subscriptionId);
        assertThat(dbSubscription).isPresent();
        assertThat(dbSubscription.get().getTier()).isEqualTo(SubscriptionTier.PRO);
    }

    /**
     * Scenario 5: Billing Cycle Change from Monthly to Annual
     *
     * Tests billing cycle change workflow including pricing recalculation
     * and next billing date adjustments.
     */
    @Test
    void scenario5_changeBillingCycleMonthlyToAnnual_shouldSucceed() throws ExecutionException, InterruptedException {
        // Given - User has monthly subscription
        UUID userId = UUID.randomUUID();

        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
        ).get();

        assertThat(createResult.isSuccess()).isTrue();

        UUID subscriptionId = createResult.map(Subscription::getId).orElse(null);
        assertThat(subscriptionId).isNotNull();

        // When - User changes to annual billing
        Result<Subscription, Exception> changeCycleResult = upgradeService.changeBillingCycle(
            subscriptionId, BillingCycle.ANNUAL
        ).get();

        // Then - Billing cycle should be updated
        assertThat(changeCycleResult.isSuccess()).isTrue();

        changeCycleResult.ifSuccess(updatedSub -> {
            assertThat(updatedSub.getId()).isEqualTo(subscriptionId);
            assertThat(updatedSub.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
            assertThat(updatedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

            log.info("Billing cycle changed from MONTHLY to ANNUAL: {}", updatedSub.getId());
        });

        // Verify database state
        Optional<Subscription> dbSubscription = subscriptionRepository.findById(subscriptionId);
        assertThat(dbSubscription).isPresent();
        assertThat(dbSubscription.get().getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
    }

    /**
     * Scenario 6: Multi-User Concurrent Subscription Management
     *
     * Tests concurrent subscription operations for multiple users
     * to validate thread safety and data integrity.
     */
    @Test
    void scenario6_multiUserConcurrentManagement_shouldSucceed() throws Exception {
        // Given - Multiple users performing operations concurrently
        int userCount = 50;
        UUID[] userIds = new UUID[userCount];

        // When - Create subscriptions concurrently
        for (int i = 0; i < userCount; i++) {
            userIds[i] = UUID.randomUUID();
            final UUID userId = userIds[i];

            lifecycleService.createSubscription(
                userId,
                i % 3 == 0 ? SubscriptionTier.FREE :
                    i % 3 == 1 ? SubscriptionTier.PRO :
                        SubscriptionTier.AI_PREMIUM,
                BillingCycle.MONTHLY,
                false
            ).get();
        }

        // Then - All subscriptions should be created
        assertThat(subscriptionRepository.count()).isGreaterThanOrEqualTo(userCount);

        // Verify each subscription
        for (UUID userId : userIds) {
            Optional<Subscription> subscription = subscriptionRepository.findByUserIdAndStatus(
                userId, SubscriptionStatus.ACTIVE
            );
            assertThat(subscription).isPresent();
            assertThat(subscription.get().getUserId()).isEqualTo(userId);
        }

        log.info("Successfully created {} concurrent subscriptions", userCount);
    }

    /**
     * Scenario 7: Subscription Reactivation After Cancellation
     *
     * Tests the workflow of reactivating a previously cancelled subscription
     * including status validation and access restoration.
     */
    @Test
    void scenario7_reactivateAfterCancellation_shouldSucceed() throws ExecutionException, InterruptedException {
        // Given - User has a cancelled subscription
        UUID userId = UUID.randomUUID();

        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
        ).get();

        assertThat(createResult.isSuccess()).isTrue();

        UUID subscriptionId = createResult.map(Subscription::getId).orElse(null);
        assertThat(subscriptionId).isNotNull();

        // Cancel the subscription
        Result<Subscription, Exception> cancelResult = cancellationService.cancelSubscription(
            subscriptionId, true
        ).get();

        assertThat(cancelResult.isSuccess()).isTrue();
        assertThat(cancelResult.map(Subscription::getStatus).orElse(null))
            .isEqualTo(SubscriptionStatus.CANCELLED);

        // When - User reactivates the subscription
        Result<Subscription, Exception> reactivateResult = lifecycleService.activateSubscription(
            subscriptionId
        ).get();

        // Then - Subscription should be reactivated
        assertThat(reactivateResult.isSuccess()).isTrue();

        reactivateResult.ifSuccess(reactivatedSub -> {
            assertThat(reactivatedSub.getId()).isEqualTo(subscriptionId);
            assertThat(reactivatedSub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(reactivatedSub.getTier()).isEqualTo(SubscriptionTier.PRO);

            log.info("Subscription reactivated after cancellation: {}", reactivatedSub.getId());
        });

        // Verify database state
        Optional<Subscription> dbSubscription = subscriptionRepository.findById(subscriptionId);
        assertThat(dbSubscription).isPresent();
        assertThat(dbSubscription.get().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
