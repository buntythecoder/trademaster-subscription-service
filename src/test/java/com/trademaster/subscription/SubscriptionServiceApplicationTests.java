package com.trademaster.subscription;

import com.trademaster.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Subscription Service Application
 * 
 * Uses TestContainers for PostgreSQL and Kafka to ensure real integration testing.
 * Tests basic application startup and dependency injection.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=",
    "app.services.payment-gateway.url=http://localhost:8999",
    "app.services.payment-gateway.api-key=test-key"
})
class SubscriptionServiceApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("trademaster_subscription_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        
        // Flyway configuration for tests
        registry.add("spring.flyway.clean-disabled", () -> false);
        registry.add("spring.flyway.clean-on-validation-error", () -> true);
        
        // Disable security for integration tests
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "");
    }

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
        assertThat(subscriptionRepository).isNotNull();
    }

    @Test
    void shouldConnectToDatabase() {
        // Verify database connectivity
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.isCreated()).isTrue();
        
        // Test basic repository functionality
        long count = subscriptionRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldConnectToKafka() {
        // Verify Kafka connectivity
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.isCreated()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotEmpty();
    }

    @Test
    void shouldHaveVirtualThreadsEnabled() {
        // Verify Virtual Threads are enabled
        String virtualThreadsEnabled = System.getProperty("spring.threads.virtual.enabled");
        assertThat(virtualThreadsEnabled).isEqualTo("true");
    }
}