package com.trademaster.subscription.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consul Service Discovery Integration Configuration
 *
 * MANDATORY implementation following TradeMaster Golden Specification patterns.
 * Provides service registration, health checks, and metadata for subscription-service.
 *
 * @author TradeMaster Engineering Team
 * @version 1.0.0
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

    @Value("${management.server.port:${server.port}}")
    private int managementPort;

    @Value("${spring.cloud.consul.discovery.health-check-interval:10s}")
    private String healthCheckInterval;

    @Value("${trademaster.consul.datacenter:trademaster-dc}")
    private String datacenter;

    private final Environment environment;

    /**
     * Consul configuration is managed through application.yml properties.
     * This class provides logging and monitoring for Consul integration status.
     */
    public void logConsulConfigurationStatus() {
        log.info("Consul service discovery configuration:");
        log.info("  Service Name: {}", serviceName);
        log.info("  Service Port: {}", serverPort);
        log.info("  Management Port: {}", managementPort);
        log.info("  Health Check Interval: {}", healthCheckInterval);
        log.info("  Datacenter: {}", datacenter);
        log.info("  Active Profile: {}", getActiveProfile());
    }

    /**
     * Get the active Spring profile for service tagging
     *
     * @return Active profile or 'default' if none specified
     */
    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "default";
    }
}