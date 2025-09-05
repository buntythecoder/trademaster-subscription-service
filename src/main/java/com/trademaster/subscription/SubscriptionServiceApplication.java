package com.trademaster.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * TradeMaster Subscription Service Application
 * 
 * Complete subscription management service with multi-tier support,
 * usage tracking, billing automation, and compliance features.
 * 
 * Features:
 * - Multi-tier subscription management (Free, Pro, AI Premium, Institutional)
 * - Automated billing and payment processing
 * - Real-time usage tracking and limit enforcement
 * - Revenue analytics and churn prediction
 * - Event-driven architecture with Kafka
 * - Redis caching for performance
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableKafka
public class SubscriptionServiceApplication {

    public static void main(String[] args) {
        // MANDATORY: Virtual Threads configuration per TradeMaster Standards
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}