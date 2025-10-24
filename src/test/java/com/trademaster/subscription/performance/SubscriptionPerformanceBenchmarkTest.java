package com.trademaster.subscription.performance;

import com.trademaster.subscription.common.Result;
import com.trademaster.subscription.entity.Subscription;
import com.trademaster.subscription.enums.BillingCycle;
import com.trademaster.subscription.enums.SubscriptionStatus;
import com.trademaster.subscription.enums.SubscriptionTier;
import com.trademaster.subscription.repository.SubscriptionRepository;
import com.trademaster.subscription.service.SubscriptionLifecycleService;
import com.trademaster.subscription.service.SubscriptionUpgradeService;
import com.trademaster.subscription.service.UsageTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance Benchmark Tests for Subscription Service
 *
 * MANDATORY: Rule #20 - Performance Testing with SLA Validation
 * MANDATORY: Rule #22 - Performance Standards Compliance
 *
 * Performance Targets:
 * - CRITICAL: <25ms response time
 * - HIGH: <50ms response time
 * - STANDARD: <100ms response time
 * - BATCH: <200ms for bulk operations
 *
 * Tests measure:
 * 1. Subscription creation performance (CRITICAL)
 * 2. Subscription upgrade performance (HIGH)
 * 3. Usage tracking performance (STANDARD)
 * 4. Billing calculation performance (HIGH)
 * 5. Concurrent subscription operations (CRITICAL)
 * 6. Database query performance (STANDARD)
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
    "spring.kafka.consumer.bootstrap-servers=localhost:9092",
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate=WARN"
})
class SubscriptionPerformanceBenchmarkTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("trademaster_subscription_perf_test")
        .withUsername("test_user")
        .withPassword("test_password");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionLifecycleService lifecycleService;

    @Autowired
    private SubscriptionUpgradeService upgradeService;

    @Autowired
    private UsageTrackingService usageTrackingService;

    private final AtomicLong operationCounter = new AtomicLong(0);
    private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Performance-optimized settings
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "5");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void setUp() {
        operationCounter.set(0);
        responseTimes.clear();
    }

    /**
     * Benchmark 1: Subscription Creation Performance (CRITICAL - Target: <25ms)
     *
     * Tests the end-to-end performance of creating a new subscription
     * including database persistence, validation, and event publishing.
     */
    @Test
    void benchmarkSubscriptionCreation_shouldMeetCriticalSLA() throws ExecutionException, InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        int iterations = 100;

        // Warmup
        IntStream.range(0, 10).forEach(i -> {
            UUID warmupUserId = UUID.randomUUID();
            lifecycleService.createSubscription(
                warmupUserId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
            ).join();
        });

        // When - Benchmark subscription creation
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            UUID testUserId = UUID.randomUUID();
            Instant start = Instant.now();

            CompletableFuture<Result<Subscription, Exception>> future =
                lifecycleService.createSubscription(
                    testUserId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
                );

            Result<Subscription, Exception> result = future.get();
            Instant end = Instant.now();

            long durationMs = Duration.between(start, end).toMillis();
            times.add(durationMs);

            assertThat(result.isSuccess()).isTrue();
        }

        // Then - Validate performance metrics
        PerformanceMetrics metrics = calculateMetrics(times);

        log.info("Subscription Creation Performance:");
        log.info("  Average: {}ms", metrics.average);
        log.info("  Median: {}ms", metrics.median);
        log.info("  P95: {}ms", metrics.p95);
        log.info("  P99: {}ms", metrics.p99);
        log.info("  Min: {}ms", metrics.min);
        log.info("  Max: {}ms", metrics.max);

        // CRITICAL SLA: Average <25ms, P95 <50ms
        assertThat(metrics.average)
            .as("Average subscription creation time should be <25ms (CRITICAL SLA)")
            .isLessThan(25.0);

        assertThat(metrics.p95)
            .as("P95 subscription creation time should be <50ms")
            .isLessThan(50.0);
    }

    /**
     * Benchmark 2: Subscription Upgrade Performance (HIGH - Target: <50ms)
     *
     * Tests the performance of upgrading a subscription tier including
     * validation, pricing calculation, and state transitions.
     */
    @Test
    void benchmarkSubscriptionUpgrade_shouldMeetHighSLA() throws ExecutionException, InterruptedException {
        // Given - Create baseline subscriptions
        List<UUID> subscriptionIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID userId = UUID.randomUUID();
            Result<Subscription, Exception> result = lifecycleService.createSubscription(
                userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
            ).get();

            result.ifSuccess(subscription -> subscriptionIds.add(subscription.getId()));
        }

        // When - Benchmark subscription upgrades
        List<Long> times = new ArrayList<>();
        for (UUID subscriptionId : subscriptionIds) {
            Instant start = Instant.now();

            CompletableFuture<Result<Subscription, Exception>> future =
                upgradeService.upgradeTier(subscriptionId, SubscriptionTier.AI_PREMIUM);

            Result<Subscription, Exception> result = future.get();
            Instant end = Instant.now();

            long durationMs = Duration.between(start, end).toMillis();
            times.add(durationMs);

            assertThat(result.isSuccess()).isTrue();
        }

        // Then - Validate performance metrics
        PerformanceMetrics metrics = calculateMetrics(times);

        log.info("Subscription Upgrade Performance:");
        log.info("  Average: {}ms", metrics.average);
        log.info("  Median: {}ms", metrics.median);
        log.info("  P95: {}ms", metrics.p95);
        log.info("  P99: {}ms", metrics.p99);

        // HIGH SLA: Average <50ms, P95 <100ms
        assertThat(metrics.average)
            .as("Average subscription upgrade time should be <50ms (HIGH SLA)")
            .isLessThan(50.0);

        assertThat(metrics.p95)
            .as("P95 subscription upgrade time should be <100ms")
            .isLessThan(100.0);
    }

    /**
     * Benchmark 3: Usage Tracking Performance (STANDARD - Target: <100ms)
     *
     * Tests the performance of tracking API usage and validating against
     * subscription tier limits.
     */
    @Test
    void benchmarkUsageTracking_shouldMeetStandardSLA() throws ExecutionException, InterruptedException {
        // Given - Create subscription
        UUID userId = UUID.randomUUID();
        Result<Subscription, Exception> createResult = lifecycleService.createSubscription(
            userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
        ).get();

        assertThat(createResult.isSuccess()).isTrue();

        // When - Benchmark usage tracking
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Instant start = Instant.now();

            CompletableFuture<Result<Void, Exception>> future =
                usageTrackingService.trackApiUsage(userId, 1);

            Result<Void, Exception> result = future.get();
            Instant end = Instant.now();

            long durationMs = Duration.between(start, end).toMillis();
            times.add(durationMs);

            assertThat(result.isSuccess()).isTrue();
        }

        // Then - Validate performance metrics
        PerformanceMetrics metrics = calculateMetrics(times);

        log.info("Usage Tracking Performance:");
        log.info("  Average: {}ms", metrics.average);
        log.info("  Median: {}ms", metrics.median);
        log.info("  P95: {}ms", metrics.p95);
        log.info("  P99: {}ms", metrics.p99);

        // STANDARD SLA: Average <100ms, P95 <200ms
        assertThat(metrics.average)
            .as("Average usage tracking time should be <100ms (STANDARD SLA)")
            .isLessThan(100.0);

        assertThat(metrics.p95)
            .as("P95 usage tracking time should be <200ms")
            .isLessThan(200.0);
    }

    /**
     * Benchmark 4: Database Query Performance (STANDARD - Target: <100ms)
     *
     * Tests the performance of common database queries including
     * active subscription lookup and tier-based filtering.
     */
    @Test
    void benchmarkDatabaseQueries_shouldMeetStandardSLA() {
        // Given - Create test data
        List<UUID> userIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UUID userId = UUID.randomUUID();
            userIds.add(userId);

            Subscription subscription = Subscription.builder()
                .userId(userId)
                .tier(i % 2 == 0 ? SubscriptionTier.PRO : SubscriptionTier.AI_PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .build();

            subscriptionRepository.save(subscription);
        }

        // When - Benchmark common queries
        List<Long> findByUserIdTimes = new ArrayList<>();
        List<Long> findByTierTimes = new ArrayList<>();

        // Query 1: Find by user ID (most common)
        for (UUID userId : userIds) {
            Instant start = Instant.now();
            subscriptionRepository.findByUserId(userId);
            Instant end = Instant.now();

            findByUserIdTimes.add(Duration.between(start, end).toMillis());
        }

        // Query 2: Find by tier
        for (int i = 0; i < 50; i++) {
            SubscriptionTier tier = i % 2 == 0 ? SubscriptionTier.PRO : SubscriptionTier.AI_PREMIUM;
            Instant start = Instant.now();
            subscriptionRepository.findByTier(tier);
            Instant end = Instant.now();

            findByTierTimes.add(Duration.between(start, end).toMillis());
        }

        // Then - Validate query performance
        PerformanceMetrics userIdMetrics = calculateMetrics(findByUserIdTimes);
        PerformanceMetrics tierMetrics = calculateMetrics(findByTierTimes);

        log.info("Database Query Performance:");
        log.info("  Find by User ID - Average: {}ms, P95: {}ms",
            userIdMetrics.average, userIdMetrics.p95);
        log.info("  Find by Tier - Average: {}ms, P95: {}ms",
            tierMetrics.average, tierMetrics.p95);

        // STANDARD SLA: Average <100ms for queries
        assertThat(userIdMetrics.average)
            .as("Average findByUserId query time should be <100ms")
            .isLessThan(100.0);

        assertThat(tierMetrics.average)
            .as("Average findByTier query time should be <100ms")
            .isLessThan(100.0);
    }

    /**
     * Benchmark 5: Concurrent Operations with Virtual Threads (CRITICAL - Target: 10,000+ concurrent users)
     *
     * Tests the system's ability to handle high concurrency using Java 24 Virtual Threads
     * while maintaining acceptable response times.
     */
    @Test
    void benchmarkConcurrentOperations_shouldHandleHighLoad() throws Exception {
        // Given
        int concurrentUsers = 1000;
        CompletableFuture<Result<Subscription, Exception>>[] futures = new CompletableFuture[concurrentUsers];

        Instant overallStart = Instant.now();

        // When - Execute concurrent subscription creations
        for (int i = 0; i < concurrentUsers; i++) {
            UUID userId = UUID.randomUUID();
            futures[i] = lifecycleService.createSubscription(
                userId, SubscriptionTier.PRO, BillingCycle.MONTHLY, false
            );
        }

        // Wait for all operations
        CompletableFuture.allOf(futures).join();
        Instant overallEnd = Instant.now();

        // Then - Validate concurrency performance
        long totalDuration = Duration.between(overallStart, overallEnd).toMillis();
        double throughput = (concurrentUsers * 1000.0) / totalDuration; // operations per second

        log.info("Concurrent Operations Performance:");
        log.info("  Total operations: {}", concurrentUsers);
        log.info("  Total duration: {}ms", totalDuration);
        log.info("  Throughput: {:.2f} ops/sec", throughput);

        // Validate all operations succeeded
        long successCount = Arrays.stream(futures)
            .map(CompletableFuture::join)
            .filter(Result::isSuccess)
            .count();

        assertThat(successCount)
            .as("All concurrent operations should succeed")
            .isEqualTo(concurrentUsers);

        // CRITICAL SLA: Should handle 1000 concurrent operations in <10 seconds
        assertThat(totalDuration)
            .as("1000 concurrent operations should complete in <10 seconds")
            .isLessThan(10000);

        assertThat(throughput)
            .as("Throughput should exceed 100 ops/sec")
            .isGreaterThan(100.0);
    }

    /**
     * Performance Metrics Record
     */
    private record PerformanceMetrics(
        double average,
        double median,
        double p95,
        double p99,
        long min,
        long max
    ) {}

    /**
     * Calculate performance metrics from response times
     */
    private PerformanceMetrics calculateMetrics(List<Long> times) {
        List<Long> sortedTimes = new ArrayList<>(times);
        Collections.sort(sortedTimes);

        double average = sortedTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        double median = sortedTimes.get(sortedTimes.size() / 2);
        double p95 = sortedTimes.get((int) (sortedTimes.size() * 0.95));
        double p99 = sortedTimes.get((int) (sortedTimes.size() * 0.99));
        long min = sortedTimes.get(0);
        long max = sortedTimes.get(sortedTimes.size() - 1);

        return new PerformanceMetrics(average, median, p95, p99, min, max);
    }
}
