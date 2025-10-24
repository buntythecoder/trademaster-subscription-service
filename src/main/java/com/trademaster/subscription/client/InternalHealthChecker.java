package com.trademaster.subscription.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Internal Health Checker
 * MANDATORY: Single Responsibility - Health check operations only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Checks health and connectivity of internal services through Kong Gateway.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalHealthChecker {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Value("${trademaster.security.service.api-key}")
    private String serviceApiKey;

    @Value("${trademaster.service.name:subscription-service}")
    private String serviceName;

    @Qualifier("internalServiceHttpClient")
    private final OkHttpClient httpClient;

    /**
     * Health check for internal service connectivity
     *
     * @param serviceUrl Kong Gateway internal URL for target service
     * @return true if service is healthy, false otherwise
     */
    public boolean checkServiceHealth(String serviceUrl) {
        return checkServiceHealth(serviceUrl, UUID.randomUUID().toString());
    }

    /**
     * Health check with correlation ID
     *
     * @param serviceUrl    Kong Gateway internal URL for target service
     * @param correlationId Request correlation ID
     * @return true if service is healthy, false otherwise
     */
    public boolean checkServiceHealth(String serviceUrl, String correlationId) {
        try {
            String healthUrl = serviceUrl + "/api/internal/v1/health";

            Request request = new Request.Builder()
                .url(healthUrl)
                .addHeader(API_KEY_HEADER, serviceApiKey)
                .addHeader(SERVICE_ID_HEADER, serviceName)
                .addHeader(CORRELATION_ID_HEADER, correlationId)
                .addHeader("Accept", "application/json")
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                boolean isHealthy = response.isSuccessful();

                // MANDATORY: Rule #3 - No if-else, using Optional pattern
                java.util.Optional.of(isHealthy)
                    .filter(Boolean::booleanValue)
                    .ifPresentOrElse(
                        healthy -> log.debug("Service health check passed: {} (correlation: {})", serviceUrl, correlationId),
                        () -> log.warn("Service health check failed: {} - Status: {} (correlation: {})",
                                serviceUrl, response.code(), correlationId)
                    );

                return isHealthy;
            }

        } catch (Exception e) {
            log.error("Service health check failed for: {} (correlation: {}) - Error: {}",
                    serviceUrl, correlationId, e.getMessage());
            return false;
        }
    }
}
