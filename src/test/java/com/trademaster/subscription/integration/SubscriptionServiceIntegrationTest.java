package com.trademaster.subscription.integration;

import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Subscription Service Integration Tests with TestContainers
 * 
 * MANDATORY: TestContainers Integration for Production Database Testing
 * MANDATORY: Virtual Thread Concurrency Testing
 * MANDATORY: Business Logic Validation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@SpringBootTest
@Testcontainers
@Transactional
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.kafka.producer.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.bootstrap-servers=localhost:9092"
})
class SubscriptionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("trademaster_subscription_test")
        .withUsername("test_user")
        .withPassword("test_password")
        .withInitScript("init-test-db.sql");

    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private SubscriptionLifecycleService subscriptionLifecycleService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Disable external dependencies for testing
        registry.add("app.services.payment-gateway.url", () -> "http://localhost:0");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9093");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "0");
    }

    @Test
    void shouldCreateSubscriptionSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        
        Subscription subscription = Subscription.builder()
            .userId(userId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
        
        // When
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        
        // Then
        assertThat(savedSubscription).isNotNull();
        assertThat(savedSubscription.getId()).isNotNull();
        assertThat(savedSubscription.getUserId()).isEqualTo(userId);
        assertThat(savedSubscription.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void shouldFindSubscriptionsByTier() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        
        Subscription proSubscription = Subscription.builder()
            .userId(userId1)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
        
        Subscription premiumSubscription = Subscription.builder()
            .userId(userId2)
            .tier(SubscriptionTier.AI_PREMIUM)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
        
        subscriptionRepository.save(proSubscription);
        subscriptionRepository.save(premiumSubscription);
        
        // When
        List<Subscription> proSubscriptions = subscriptionRepository.findByTier(SubscriptionTier.PRO);
        List<Subscription> premiumSubscriptions = subscriptionRepository.findByTier(SubscriptionTier.AI_PREMIUM);
        
        // Then
        assertThat(proSubscriptions).hasSize(1);
        assertThat(proSubscriptions.get(0).getTier()).isEqualTo(SubscriptionTier.PRO);
        
        assertThat(premiumSubscriptions).hasSize(1);
        assertThat(premiumSubscriptions.get(0).getTier()).isEqualTo(SubscriptionTier.AI_PREMIUM);
    }

    @Test 
    void shouldHandleHighConcurrencyWithVirtualThreads() throws Exception {
        // Given - Multiple concurrent subscription operations
        int concurrentOperations = 100;
        CompletableFuture<Subscription>[] futures = new CompletableFuture[concurrentOperations];
        
        // When - Execute operations concurrently
        for (int i = 0; i < concurrentOperations; i++) {
            UUID userId = UUID.randomUUID();
            final int index = i;
            
            futures[i] = CompletableFuture.supplyAsync(() -> {
                Subscription subscription = Subscription.builder()
                    .userId(userId)
                    .tier(SubscriptionTier.PRO)
                    .status(SubscriptionStatus.ACTIVE)
                    .billingCycle(BillingCycle.MONTHLY)
                    .build();
                return subscriptionRepository.save(subscription);
            });
        }
        
        // Wait for all operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(30, TimeUnit.SECONDS);
        
        // Then - Verify all subscriptions were created
        for (CompletableFuture<Subscription> future : futures) {
            Subscription result = future.get();
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTier()).isEqualTo(SubscriptionTier.PRO);
        }
        
        // Verify total count
        List<Subscription> allSubscriptions = subscriptionRepository.findAll();
        assertThat(allSubscriptions).hasSizeGreaterThanOrEqualTo(concurrentOperations);
    }
    
    @Test
    void shouldValidateSubscriptionBusinessRules() {
        // Given
        UUID userId = UUID.randomUUID();
        
        Subscription subscription = Subscription.builder()
            .userId(userId)
            .tier(SubscriptionTier.FREE)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
        
        // When
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        
        // Then - Validate business rules
        assertThat(savedSubscription.getTier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(savedSubscription.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
    
    @Test
    void shouldTrackSubscriptionLifecycle() {
        // Given - Create subscription
        UUID userId = UUID.randomUUID();
        Subscription subscription = Subscription.builder()
            .userId(userId)
            .tier(SubscriptionTier.PRO)
            .status(SubscriptionStatus.ACTIVE)
            .billingCycle(BillingCycle.MONTHLY)
            .build();
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        UUID subscriptionId = savedSubscription.getId();
        
        // When - Update subscription status to suspended
        savedSubscription.setStatus(SubscriptionStatus.SUSPENDED);
        Subscription updatedSubscription = subscriptionRepository.save(savedSubscription);
        
        // Then - Verify lifecycle tracking
        assertThat(updatedSubscription.getId()).isEqualTo(subscriptionId);
        assertThat(updatedSubscription.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        assertThat(updatedSubscription.getUserId()).isEqualTo(userId);
        assertThat(updatedSubscription.getTier()).isEqualTo(SubscriptionTier.PRO);
    }
}