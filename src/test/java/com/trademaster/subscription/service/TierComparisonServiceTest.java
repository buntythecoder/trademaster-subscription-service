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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tier Comparison Service Unit Tests
 *
 * MANDATORY: Functional Programming patterns - Pattern matching and switch expressions
 * MANDATORY: >80% coverage requirement - Comprehensive test scenarios for all tier combinations
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@DisplayName("Tier Comparison Service Tests")
class TierComparisonServiceTest {

    private TierComparisonService tierComparisonService;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        tierComparisonService = new TierComparisonService();

        testSubscription = Subscription.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .tier(SubscriptionTier.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
    }

    @Nested
    @DisplayName("Valid Upgrade Tests")
    class ValidUpgradeTests {

        @Test
        @DisplayName("Should allow upgrade from FREE to PRO")
        void shouldAllowUpgradeFromFreeToPro() {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.PRO);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(testSubscription);
        }

        @Test
        @DisplayName("Should allow upgrade from FREE to AI_PREMIUM")
        void shouldAllowUpgradeFromFreeToAiPremium() {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.AI_PREMIUM);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(testSubscription);
        }

        @Test
        @DisplayName("Should allow upgrade from FREE to INSTITUTIONAL")
        void shouldAllowUpgradeFromFreeToInstitutional() {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.INSTITUTIONAL);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should allow upgrade from PRO to AI_PREMIUM")
        void shouldAllowUpgradeFromProToAiPremium() {
            // Given
            testSubscription.setTier(SubscriptionTier.PRO);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.AI_PREMIUM);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should allow upgrade from PRO to INSTITUTIONAL")
        void shouldAllowUpgradeFromProToInstitutional() {
            // Given
            testSubscription.setTier(SubscriptionTier.PRO);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.INSTITUTIONAL);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should allow upgrade from AI_PREMIUM to INSTITUTIONAL")
        void shouldAllowUpgradeFromAiPremiumToInstitutional() {
            // Given
            testSubscription.setTier(SubscriptionTier.AI_PREMIUM);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.INSTITUTIONAL);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid Downgrade Tests")
    class InvalidDowngradeTests {

        @Test
        @DisplayName("Should reject downgrade from PRO to FREE")
        void shouldRejectDowngradeFromProToFree() {
            // Given
            testSubscription.setTier(SubscriptionTier.PRO);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.FREE);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Cannot downgrade");
            assertThat(result.getError()).contains("PRO");
            assertThat(result.getError()).contains("FREE");
        }

        @Test
        @DisplayName("Should reject downgrade from AI_PREMIUM to FREE")
        void shouldRejectDowngradeFromAiPremiumToFree() {
            // Given
            testSubscription.setTier(SubscriptionTier.AI_PREMIUM);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.FREE);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Cannot downgrade");
        }

        @Test
        @DisplayName("Should reject downgrade from AI_PREMIUM to PRO")
        void shouldRejectDowngradeFromAiPremiumToPro() {
            // Given
            testSubscription.setTier(SubscriptionTier.AI_PREMIUM);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.PRO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Cannot downgrade");
        }

        @Test
        @DisplayName("Should reject downgrade from INSTITUTIONAL to FREE")
        void shouldRejectDowngradeFromInstitutionalToFree() {
            // Given
            testSubscription.setTier(SubscriptionTier.INSTITUTIONAL);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.FREE);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Cannot downgrade");
        }

        @Test
        @DisplayName("Should reject downgrade from INSTITUTIONAL to PRO")
        void shouldRejectDowngradeFromInstitutionalToPro() {
            // Given
            testSubscription.setTier(SubscriptionTier.INSTITUTIONAL);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.PRO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Cannot downgrade");
        }

        @Test
        @DisplayName("Should reject downgrade from INSTITUTIONAL to AI_PREMIUM")
        void shouldRejectDowngradeFromInstitutionalToAiPremium() {
            // Given
            testSubscription.setTier(SubscriptionTier.INSTITUTIONAL);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.AI_PREMIUM);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Cannot downgrade");
        }
    }

    @Nested
    @DisplayName("Same Tier Tests")
    class SameTierTests {

        @Test
        @DisplayName("Should reject upgrade to same tier FREE")
        void shouldRejectUpgradeToSameTierFree() {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.FREE);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Already on requested tier");
            assertThat(result.getError()).contains("FREE");
        }

        @Test
        @DisplayName("Should reject upgrade to same tier PRO")
        void shouldRejectUpgradeToSameTierPro() {
            // Given
            testSubscription.setTier(SubscriptionTier.PRO);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.PRO);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Already on requested tier");
            assertThat(result.getError()).contains("PRO");
        }

        @Test
        @DisplayName("Should reject upgrade to same tier AI_PREMIUM")
        void shouldRejectUpgradeToSameTierAiPremium() {
            // Given
            testSubscription.setTier(SubscriptionTier.AI_PREMIUM);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.AI_PREMIUM);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Already on requested tier");
        }

        @Test
        @DisplayName("Should reject upgrade to same tier INSTITUTIONAL")
        void shouldRejectUpgradeToSameTierInstitutional() {
            // Given
            testSubscription.setTier(SubscriptionTier.INSTITUTIONAL);

            // When
            Result<Subscription, String> result =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.INSTITUTIONAL);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).contains("Already on requested tier");
        }
    }

    @Nested
    @DisplayName("Compare Tiers Tests")
    class CompareTiersTests {

        @ParameterizedTest(name = "{0} to {1} should be {2}")
        @CsvSource({
            "FREE, PRO, UPGRADE",
            "FREE, AI_PREMIUM, UPGRADE",
            "FREE, INSTITUTIONAL, UPGRADE",
            "PRO, AI_PREMIUM, UPGRADE",
            "PRO, INSTITUTIONAL, UPGRADE",
            "AI_PREMIUM, INSTITUTIONAL, UPGRADE",
            "PRO, FREE, DOWNGRADE",
            "AI_PREMIUM, FREE, DOWNGRADE",
            "AI_PREMIUM, PRO, DOWNGRADE",
            "INSTITUTIONAL, FREE, DOWNGRADE",
            "INSTITUTIONAL, PRO, DOWNGRADE",
            "INSTITUTIONAL, AI_PREMIUM, DOWNGRADE",
            "FREE, FREE, SAME",
            "PRO, PRO, SAME",
            "AI_PREMIUM, AI_PREMIUM, SAME",
            "INSTITUTIONAL, INSTITUTIONAL, SAME"
        })
        @DisplayName("Should correctly compare tier combinations")
        void shouldCorrectlyCompareTierCombinations(
            SubscriptionTier current,
            SubscriptionTier target,
            String expected
        ) {
            // When
            String result = tierComparisonService.compareTiers(current, target);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return UPGRADE for ascending tier transition")
        void shouldReturnUpgradeForAscendingTier() {
            // When
            String result = tierComparisonService.compareTiers(SubscriptionTier.FREE, SubscriptionTier.PRO);

            // Then
            assertThat(result).isEqualTo("UPGRADE");
        }

        @Test
        @DisplayName("Should return DOWNGRADE for descending tier transition")
        void shouldReturnDowngradeForDescendingTier() {
            // When
            String result = tierComparisonService.compareTiers(SubscriptionTier.PRO, SubscriptionTier.FREE);

            // Then
            assertThat(result).isEqualTo("DOWNGRADE");
        }

        @Test
        @DisplayName("Should return SAME for identical tiers")
        void shouldReturnSameForIdenticalTiers() {
            // When
            String result = tierComparisonService.compareTiers(SubscriptionTier.PRO, SubscriptionTier.PRO);

            // Then
            assertThat(result).isEqualTo("SAME");
        }
    }

    @Nested
    @DisplayName("Tier Ranking Tests")
    class TierRankingTests {

        @Test
        @DisplayName("Should maintain correct tier ordering: FREE < PRO < AI_PREMIUM < INSTITUTIONAL")
        void shouldMaintainCorrectTierOrdering() {
            // Verify FREE is lowest
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.FREE, SubscriptionTier.PRO))
                .isEqualTo("UPGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.FREE, SubscriptionTier.AI_PREMIUM))
                .isEqualTo("UPGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.FREE, SubscriptionTier.INSTITUTIONAL))
                .isEqualTo("UPGRADE");

            // Verify PRO is second
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.PRO, SubscriptionTier.FREE))
                .isEqualTo("DOWNGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.PRO, SubscriptionTier.AI_PREMIUM))
                .isEqualTo("UPGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.PRO, SubscriptionTier.INSTITUTIONAL))
                .isEqualTo("UPGRADE");

            // Verify AI_PREMIUM is third
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.AI_PREMIUM, SubscriptionTier.FREE))
                .isEqualTo("DOWNGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.AI_PREMIUM, SubscriptionTier.PRO))
                .isEqualTo("DOWNGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.AI_PREMIUM, SubscriptionTier.INSTITUTIONAL))
                .isEqualTo("UPGRADE");

            // Verify INSTITUTIONAL is highest
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.INSTITUTIONAL, SubscriptionTier.FREE))
                .isEqualTo("DOWNGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.INSTITUTIONAL, SubscriptionTier.PRO))
                .isEqualTo("DOWNGRADE");
            assertThat(tierComparisonService.compareTiers(SubscriptionTier.INSTITUTIONAL, SubscriptionTier.AI_PREMIUM))
                .isEqualTo("DOWNGRADE");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle multiple upgrades with same subscription object")
        void shouldHandleMultipleUpgradesWithSameObject() {
            // Given
            testSubscription.setTier(SubscriptionTier.FREE);

            // When - Multiple upgrade validations
            Result<Subscription, String> result1 =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.PRO);
            Result<Subscription, String> result2 =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.AI_PREMIUM);
            Result<Subscription, String> result3 =
                tierComparisonService.validateTierUpgrade(testSubscription, SubscriptionTier.INSTITUTIONAL);

            // Then
            assertThat(result1.isSuccess()).isTrue();
            assertThat(result2.isSuccess()).isTrue();
            assertThat(result3.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should validate upgrade with all tier combinations systematically")
        void shouldValidateUpgradeWithAllTierCombinationsSystematically() {
            // Test all valid upgrade paths
            SubscriptionTier[] tiers = SubscriptionTier.values();

            for (int i = 0; i < tiers.length; i++) {
                for (int j = 0; j < tiers.length; j++) {
                    testSubscription.setTier(tiers[i]);
                    Result<Subscription, String> result =
                        tierComparisonService.validateTierUpgrade(testSubscription, tiers[j]);

                    if (i < j) {
                        // Upgrade should succeed
                        assertThat(result.isSuccess())
                            .withFailMessage("Upgrade from %s to %s should succeed", tiers[i], tiers[j])
                            .isTrue();
                    } else if (i == j) {
                        // Same tier should fail
                        assertThat(result.isFailure())
                            .withFailMessage("Same tier %s to %s should fail", tiers[i], tiers[j])
                            .isTrue();
                        assertThat(result.getError()).contains("Already on requested tier");
                    } else {
                        // Downgrade should fail
                        assertThat(result.isFailure())
                            .withFailMessage("Downgrade from %s to %s should fail", tiers[i], tiers[j])
                            .isTrue();
                        assertThat(result.getError()).contains("Cannot downgrade");
                    }
                }
            }
        }
    }
}
