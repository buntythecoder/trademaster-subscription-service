package com.trademaster.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TradeMaster Subscription Service Application
 *
 * Complete subscription management service with multi-tier support,
 * usage tracking, billing automation, and compliance features.
 *
 * Features:
 * - Multi-tier subscription management (Free, Pro, AI Premium, Institutional)
 * - Kong API Gateway integration and authentication
 * - Real-time health monitoring and service discovery
 * - Event-driven architecture with virtual threads
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {JpaRepositoriesAutoConfiguration.class})
@ComponentScan(
    basePackages = "com.trademaster.subscription",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.service\\.Subscription.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.service\\.ErrorTrackingService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.controller\\.SubscriptionController"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.repository\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.config\\.ServiceIntegrationConfig"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.events\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.trademaster\\.subscription\\.integration\\..*")
    }
)
@EnableAsync
@EnableScheduling
public class SubscriptionServiceApplication {

    public static void main(String[] args) {
        // MANDATORY: Virtual Threads configuration per TradeMaster Standards
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}