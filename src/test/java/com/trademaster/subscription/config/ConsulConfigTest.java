package com.trademaster.subscription.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Consul Configuration Integration Tests
 *
 * Validates Golden Specification compliance for Consul service discovery.
 * Tests ensure that ConsulConfig meets TradeMaster standards for:
 * - 21+ mandatory service tags
 * - 15+ mandatory metadata entries
 * - Proper service identification
 * - Production-ready configuration
 *
 * @author TradeMaster Engineering Team
 * @version 1.0.0
 */
@DisplayName("Consul Configuration Tests")
class ConsulConfigTest {

    private ConsulConfig consulConfig;

    @BeforeEach
    void setUp() throws Exception {
        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setActiveProfiles("test");

        // Create ConsulConfig instance
        consulConfig = new ConsulConfig(mockEnvironment);

        // Use reflection to set @Value fields since we're not using Spring context
        setField(consulConfig, "serviceName", "subscription-service");
        setField(consulConfig, "serverPort", 8085);
        setField(consulConfig, "managementPort", 9085);
        setField(consulConfig, "healthCheckInterval", "30s");
        setField(consulConfig, "datacenter", "trademaster-dc");
        setField(consulConfig, "appVersion", "1.0.0");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = ConsulConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Service tags should meet Golden Specification minimum requirement of 21+ tags")
    void buildServiceTags_ShouldHaveAtLeast21Tags() {
        // When
        List<String> tags = consulConfig.buildServiceTags();

        // Then
        assertThat(tags)
            .as("Service tags must meet Golden Specification requirement of 21+ tags")
            .hasSizeGreaterThanOrEqualTo(21);

        System.out.println("✅ Service tags count: " + tags.size() + " (requirement: ≥21)");
    }

    @Test
    @DisplayName("Service tags should include all mandatory core identity tags")
    void buildServiceTags_ShouldIncludeMandatoryCoreIdentityTags() {
        // When
        List<String> tags = consulConfig.buildServiceTags();

        // Then
        assertThat(tags)
            .as("Core identity tags are mandatory")
            .contains(
                "version=1.0.0",
                "environment=test",
                "java=24",
                "virtual-threads=enabled",
                "framework=spring-boot-3.5.3",
                "protocol=http",
                "datacenter=trademaster-dc"
            );

        System.out.println("✅ Core identity tags validated");
    }

    @Test
    @DisplayName("Service tags should include all mandatory SLA tags")
    void buildServiceTags_ShouldIncludeMandatorySLATags() {
        // When
        List<String> tags = consulConfig.buildServiceTags();

        // Then
        assertThat(tags)
            .as("SLA tags are mandatory per Golden Specification")
            .contains(
                "sla-critical=25ms",
                "sla-high=50ms",
                "sla-standard=100ms"
            );

        System.out.println("✅ SLA tags validated");
    }

    @Test
    @DisplayName("Service tags should include subscription service capabilities")
    void buildServiceTags_ShouldIncludeSubscriptionCapabilities() {
        // When
        List<String> tags = consulConfig.buildServiceTags();

        // Then
        assertThat(tags)
            .as("Subscription capabilities must be advertised")
            .contains(
                "subscription-capabilities=BILLING,TRIAL,UPGRADE,NOTIFICATIONS",
                "api-version=v1"
            );

        System.out.println("✅ Subscription capabilities validated");
    }

    @Test
    @DisplayName("Service tags should include Kong integration tags")
    void buildServiceTags_ShouldIncludeKongIntegrationTags() {
        // When
        List<String> tags = consulConfig.buildServiceTags();

        // Then
        assertThat(tags)
            .as("Kong integration tags are mandatory")
            .contains(
                "kong-upstream=subscription-service",
                "internal-api=/api/internal/v1/subscription",
                "external-api=/api/v1/subscription"
            );

        System.out.println("✅ Kong integration tags validated");
    }

    @Test
    @DisplayName("Service tags should include feature and architecture tags")
    void buildServiceTags_ShouldIncludeFeatureAndArchitectureTags() {
        // When
        List<String> tags = consulConfig.buildServiceTags();

        // Then
        assertThat(tags)
            .as("Feature and architecture tags are mandatory")
            .contains(
                "features=multi-tier,usage-tracking,billing-automation",
                "circuit-breaker=enabled",
                "resilience=resilience4j",
                "security=zero-trust",
                "authentication=jwt,api-key",
                "authorization=role-based",
                "architecture=microservice",
                "deployment=docker"
            );

        System.out.println("✅ Feature and architecture tags validated");
    }

    @Test
    @DisplayName("Service metadata should meet Golden Specification minimum requirement of 15+ entries")
    void buildServiceMetadata_ShouldHaveAtLeast15Entries() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata)
            .as("Service metadata must meet Golden Specification requirement of 15+ entries")
            .hasSizeGreaterThanOrEqualTo(15);

        System.out.println("✅ Service metadata count: " + metadata.size() + " (requirement: ≥15)");
    }

    @Test
    @DisplayName("Service metadata should include all mandatory core service information")
    void buildServiceMetadata_ShouldIncludeMandatoryCoreInfo() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata)
            .as("Core service info is mandatory")
            .containsKeys(
                "version",
                "description",
                "team",
                "contact"
            )
            .containsEntry("version", "1.0.0")
            .containsEntry("description", "TradeMaster Subscription Management Service")
            .containsEntry("team", "subscription-team")
            .containsEntry("contact", "engineering@trademaster.com");

        System.out.println("✅ Core service info validated");
    }

    @Test
    @DisplayName("Service metadata should include supported features information")
    void buildServiceMetadata_ShouldIncludeSupportedFeatures() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata)
            .as("Supported features must be documented")
            .containsKeys(
                "supported-features",
                "supported-tiers",
                "billing-cycles"
            )
            .containsEntry("supported-features", "BILLING,TRIAL,UPGRADE,NOTIFICATIONS")
            .containsEntry("supported-tiers", "FREE,PRO,AI_PREMIUM,INSTITUTIONAL")
            .containsEntry("billing-cycles", "MONTHLY,QUARTERLY,ANNUAL");

        System.out.println("✅ Supported features validated");
    }

    @Test
    @DisplayName("Service metadata should include performance targets")
    void buildServiceMetadata_ShouldIncludePerformanceTargets() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata)
            .as("Performance targets must be defined")
            .containsKeys(
                "performance-target",
                "concurrency-model",
                "max-concurrent-requests"
            )
            .containsEntry("performance-target", "subscription-processing-100ms")
            .containsEntry("concurrency-model", "virtual-threads")
            .containsEntry("max-concurrent-requests", "10000");

        System.out.println("✅ Performance targets validated");
    }

    @Test
    @DisplayName("Service metadata should include architecture information")
    void buildServiceMetadata_ShouldIncludeArchitectureInfo() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata)
            .as("Architecture info is mandatory")
            .containsKeys(
                "architecture",
                "programming-language",
                "framework"
            )
            .containsEntry("architecture", "microservice")
            .containsEntry("programming-language", "java-24")
            .containsEntry("framework", "spring-boot-3.5.3");

        System.out.println("✅ Architecture info validated");
    }

    @Test
    @DisplayName("Service metadata should include integration information")
    void buildServiceMetadata_ShouldIncludeIntegrationInfo() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata)
            .as("Integration info is mandatory")
            .containsKeys(
                "uses-consul",
                "uses-kong",
                "database",
                "cache",
                "messaging"
            )
            .containsEntry("uses-consul", "true")
            .containsEntry("uses-kong", "true")
            .containsEntry("database", "postgresql")
            .containsEntry("cache", "redis")
            .containsEntry("messaging", "kafka");

        System.out.println("✅ Integration info validated");
    }

    @Test
    @DisplayName("Service tags should not contain duplicates")
    void buildServiceTags_ShouldNotContainDuplicates() {
        // When
        List<String> tags = consulConfig.buildServiceTags();
        long uniqueCount = tags.stream().distinct().count();

        // Then
        assertThat(uniqueCount)
            .as("Service tags should not contain duplicates")
            .isEqualTo(tags.size());

        System.out.println("✅ No duplicate tags found");
    }

    @Test
    @DisplayName("Service metadata keys should not contain nulls or empty values")
    void buildServiceMetadata_ShouldNotContainNullOrEmptyValues() {
        // When
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then
        assertThat(metadata.values())
            .as("Metadata values should not be null or empty")
            .allMatch(value -> value != null && !value.trim().isEmpty());

        System.out.println("✅ All metadata values are non-null and non-empty");
    }

    @Test
    @DisplayName("Golden Specification compliance summary")
    void goldenSpecificationCompliance_ShouldMeetAllRequirements() {
        // When
        List<String> tags = consulConfig.buildServiceTags();
        Map<String, String> metadata = consulConfig.buildServiceMetadata();

        // Then - Golden Specification Requirements
        assertThat(tags.size())
            .as("Tags count must be ≥21")
            .isGreaterThanOrEqualTo(21);

        assertThat(metadata.size())
            .as("Metadata count must be ≥15")
            .isGreaterThanOrEqualTo(15);

        // Print compliance report
        System.out.println("\n=== Golden Specification Compliance Report ===");
        System.out.println("✅ Service Tags: " + tags.size() + " (requirement: ≥21)");
        System.out.println("✅ Service Metadata: " + metadata.size() + " (requirement: ≥15)");
        System.out.println("✅ All mandatory tags present");
        System.out.println("✅ All mandatory metadata present");
        System.out.println("✅ COMPLIANCE: 100%");
        System.out.println("==============================================\n");
    }
}
