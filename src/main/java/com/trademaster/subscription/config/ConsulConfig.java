package com.trademaster.subscription.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Consul Service Discovery Integration Configuration
 *
 * MANDATORY implementation following TradeMaster Golden Specification patterns.
 * Provides service registration, health checks, and metadata for subscription-service.
 *
 * COMPLIANCE: Phase 2 - Task 2.1 Complete
 * - 21+ service tags implemented
 * - 15+ metadata entries implemented
 * - ConsulDiscoveryProperties customization via BeanPostProcessor
 * - Profile-aware configuration
 *
 * @author TradeMaster Engineering Team
 * @version 2.0.0
 * @since 2025-01-09
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.consul.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ConsulConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port}")
    private int serverPort;

    @Value("${management.server.port:9085}")
    private int managementPort;

    @Value("${spring.cloud.consul.discovery.health-check-interval:30s}")
    private String healthCheckInterval;

    @Value("${trademaster.consul.datacenter:trademaster-dc}")
    private String datacenter;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    private final Environment environment;

    /**
     * Build MANDATORY service tags per Golden Specification
     *
     * MANDATORY: Must include 21+ service tags for compliance
     *
     * @return List of service tags (23 tags)
     */
    public List<String> buildServiceTags() {
        return List.of(
            // Core Identity Tags
            "version=" + appVersion,
            "environment=" + getActiveProfile(),
            "java=24",
            "virtual-threads=enabled",
            "framework=spring-boot-3.5.3",
            "protocol=http",
            "datacenter=" + datacenter,

            // SLA Tags per Golden Specification
            "sla-critical=25ms",
            "sla-high=50ms",
            "sla-standard=100ms",

            // Subscription Service Capabilities
            "subscription-capabilities=BILLING,TRIAL,UPGRADE,NOTIFICATIONS",
            "api-version=v1",

            // Kong Integration Tags
            "kong-upstream=subscription-service",
            "internal-api=/api/internal/v1/subscription",
            "external-api=/api/v1/subscription",

            // Feature Tags
            "features=multi-tier,usage-tracking,billing-automation",
            "circuit-breaker=enabled",
            "resilience=resilience4j",

            // Security & Compliance Tags
            "security=zero-trust",
            "authentication=jwt,api-key",
            "authorization=role-based",

            // Architecture Tags
            "architecture=microservice",
            "deployment=docker"
        );
    }

    /**
     * Build service metadata per Golden Specification
     *
     * MANDATORY: Must include 15+ metadata entries for compliance
     *
     * @return Map of service metadata (18 entries)
     */
    public Map<String, String> buildServiceMetadata() {
        Map<String, String> metadata = new HashMap<>();

        // Core Service Info
        metadata.put("version", appVersion);
        metadata.put("description", "TradeMaster Subscription Management Service");
        metadata.put("team", "subscription-team");
        metadata.put("contact", "engineering@trademaster.com");

        // Supported Features
        metadata.put("supported-features", "BILLING,TRIAL,UPGRADE,NOTIFICATIONS");
        metadata.put("supported-tiers", "FREE,PRO,AI_PREMIUM,INSTITUTIONAL");
        metadata.put("billing-cycles", "MONTHLY,QUARTERLY,ANNUAL");

        // Performance Targets
        metadata.put("performance-target", "subscription-processing-100ms");
        metadata.put("concurrency-model", "virtual-threads");
        metadata.put("max-concurrent-requests", "10000");

        // Architecture Info
        metadata.put("architecture", "microservice");
        metadata.put("programming-language", "java-24");
        metadata.put("framework", "spring-boot-3.5.3");

        // Integration Info
        metadata.put("uses-consul", "true");
        metadata.put("uses-kong", "true");
        metadata.put("database", "postgresql");
        metadata.put("cache", "redis");
        metadata.put("messaging", "kafka");

        return metadata;
    }

    /**
     * Get the active Spring profile for service tagging
     * MANDATORY: Rule #3 - No ternary operators, using Optional pattern
     *
     * @return Active profile or 'default' if none specified
     */
    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return java.util.Optional.of(profiles)
            .filter(p -> p.length > 0)
            .map(p -> p[0])
            .orElse("default");
    }

    /**
     * Log Consul configuration status for monitoring
     */
    public void logConsulConfigurationStatus() {
        log.info("Consul service discovery configuration:");
        log.info("  Service Name: {}", serviceName);
        log.info("  Service Port: {}", serverPort);
        log.info("  Management Port: {}", managementPort);
        log.info("  Health Check Interval: {}", healthCheckInterval);
        log.info("  Datacenter: {}", datacenter);
        log.info("  Active Profile: {}", getActiveProfile());
        log.info("  App Version: {}", appVersion);
        log.info("  Service Tags: 23 tags configured");
        log.info("  Service Metadata: 18 metadata entries configured");
    }

    /**
     * BeanPostProcessor to customize ConsulDiscoveryProperties with Golden Specification tags/metadata
     *
     * MANDATORY: This processor intercepts Spring's auto-configured ConsulDiscoveryProperties
     * and enriches it with TradeMaster Golden Specification compliant tags and metadata.
     */
    @Component
    @ConditionalOnProperty(name = "spring.cloud.consul.enabled", havingValue = "true", matchIfMissing = true)
    @RequiredArgsConstructor
    @Slf4j
    public static class ConsulDiscoveryPropertiesCustomizer implements BeanPostProcessor {

        private final ConsulConfig consulConfig;

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof ConsulDiscoveryProperties props) {
                // Enhance auto-configured bean with Golden Specification tags and metadata
                List<String> existingTags = props.getTags();
                List<String> goldenTags = consulConfig.buildServiceTags();

                // Merge existing tags with Golden Specification tags
                // MANDATORY: Rule #3 - No ternary operators, using Optional pattern
                List<String> mergedTags = Stream.concat(
                    java.util.Optional.ofNullable(existingTags)
                        .map(List::stream)
                        .orElse(Stream.empty()),
                    goldenTags.stream()
                ).distinct().toList();

                props.setTags(mergedTags);

                // Set Golden Specification metadata
                Map<String, String> metadata = consulConfig.buildServiceMetadata();
                props.setMetadata(metadata);

                // Enhance instance ID with UUID for uniqueness
                String enhancedInstanceId = props.getServiceName() + ":" +
                                           props.getPort() + ":" +
                                           UUID.randomUUID().toString().substring(0, 8);
                props.setInstanceId(enhancedInstanceId);

                log.info("✅ ConsulDiscoveryProperties customized with Golden Specification");
                log.info("   Instance ID: {}", enhancedInstanceId);
                log.info("   Total Tags: {}", props.getTags().size());
                log.info("   Total Metadata: {}", props.getMetadata().size());

                return props;
            }
            return bean;
        }
    }
}
