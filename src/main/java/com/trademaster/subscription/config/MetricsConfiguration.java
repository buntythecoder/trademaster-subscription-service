package com.trademaster.subscription.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive Metrics Configuration for Subscription Service
 * 
 * MANDATORY: Production Metrics and Monitoring
 * MANDATORY: Prometheus Integration for Grafana Dashboards
 * MANDATORY: Business and Technical Metrics
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfiguration {

    private final MeterRegistry meterRegistry;
    
    // Business Metrics Counters
    private final AtomicLong activeSubscriptions = new AtomicLong(0);
    private final AtomicLong totalRevenue = new AtomicLong(0);
    private final AtomicInteger currentConcurrentUsers = new AtomicInteger(0);
    
    @Bean
    public Counter subscriptionCreatedCounter() {
        return Counter.builder("subscription.created.total")
            .description("Total number of subscriptions created")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter subscriptionActivatedCounter() {
        return Counter.builder("subscription.activated.total")
            .description("Total number of subscriptions activated")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter subscriptionCancelledCounter() {
        return Counter.builder("subscription.cancelled.total")
            .description("Total number of subscriptions cancelled")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter subscriptionUpgradeCounter() {
        return Counter.builder("subscription.upgraded.total")
            .description("Total number of subscription upgrades")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter paymentProcessedCounter() {
        return Counter.builder("payment.processed.total")
            .description("Total number of payments processed")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter paymentFailedCounter() {
        return Counter.builder("payment.failed.total")
            .description("Total number of failed payments")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter usageLimitExceededCounter() {
        return Counter.builder("usage.limit.exceeded.total")
            .description("Total number of usage limit exceeded events")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    // Performance Timers
    @Bean
    public Timer subscriptionCreationTimer() {
        return Timer.builder("subscription.creation.duration")
            .description("Time taken to create subscription")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Timer paymentProcessingTimer() {
        return Timer.builder("payment.processing.duration")
            .description("Time taken to process payment")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Timer databaseQueryTimer() {
        return Timer.builder("database.query.duration")
            .description("Database query execution time")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Timer externalServiceCallTimer() {
        return Timer.builder("external.service.call.duration")
            .description("External service call duration")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    // Business Health Gauges
    @Bean
    public Gauge activeSubscriptionsGauge() {
        return Gauge.builder("subscription.active.current", activeSubscriptions, AtomicLong::get)
            .description("Current number of active subscriptions")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Gauge totalRevenueGauge() {
        return Gauge.builder("revenue.total.current", totalRevenue, AtomicLong::get)
            .description("Current total revenue in cents")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    @Bean
    public Gauge concurrentUsersGauge() {
        return Gauge.builder("users.concurrent.current", currentConcurrentUsers, AtomicInteger::get)
            .description("Current number of concurrent users")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    // Circuit Breaker Metrics - Automatically registered by Resilience4j
    
    // Virtual Thread Pool Metrics
    @Bean
    public Gauge virtualThreadCountGauge() {
        return Gauge.builder("virtual.threads.active.current", this, obj -> getVirtualThreadCount())
            .description("Current number of active virtual threads")
            .tag("service", "subscription-service")
            .register(meterRegistry);
    }
    
    // JVM Memory Metrics are automatically registered by Micrometer
    
    // Custom Business Logic Metrics Helpers
    public void incrementActiveSubscriptions() {
        activeSubscriptions.incrementAndGet();
    }
    
    public void decrementActiveSubscriptions() {
        activeSubscriptions.decrementAndGet();
    }
    
    public void addRevenue(long amountInCents) {
        totalRevenue.addAndGet(amountInCents);
    }
    
    public void incrementConcurrentUsers() {
        currentConcurrentUsers.incrementAndGet();
    }
    
    public void decrementConcurrentUsers() {
        currentConcurrentUsers.decrementAndGet();
    }
    
    private long getVirtualThreadCount() {
        return Thread.getAllStackTraces().keySet().stream()
            .filter(Thread::isVirtual)
            .count();
    }
}