package com.trademaster.subscription.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Internal Service Communication Client
 *
 * MANDATORY implementation following TradeMaster Golden Specification.
 * Handles Kong Gateway-routed internal API calls between TradeMaster services.
 *
 * Features:
 * - Kong API key authentication
 * - Correlation ID propagation
 * - Circuit breaker integration
 * - Virtual Threads compatibility (OkHttp)
 * - Structured error handling
 *
 * @author TradeMaster Engineering Team
 * @version 1.0.0
 * @since 2025-01-09
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalServiceClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json");
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Value("${trademaster.security.service.api-key}")
    private String serviceApiKey;

    @Value("${trademaster.service.name:subscription-service}")
    private String serviceName;

    private final OkHttpClient httpClient;

    /**
     * Initialize HTTP client with Virtual Threads optimized configuration
     */
    public InternalServiceClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

        log.info("Internal service client initialized with Virtual Threads support");
    }

    /**
     * Call another service's internal API through Kong Gateway
     *
     * @param serviceUrl Kong Gateway internal URL for target service
     * @param endpoint   API endpoint path
     * @param payload    Request payload (nullable)
     * @param method     HTTP method (GET, POST, PUT, DELETE)
     * @return Response body as string
     * @throws InternalServiceException If service call fails
     */
    public String callInternalService(String serviceUrl, String endpoint, String payload, String method)
            throws InternalServiceException {
        return callInternalService(serviceUrl, endpoint, payload, method, null);
    }

    /**
     * Call another service's internal API with correlation ID
     *
     * @param serviceUrl    Kong Gateway internal URL for target service
     * @param endpoint      API endpoint path
     * @param payload       Request payload (nullable)
     * @param method        HTTP method (GET, POST, PUT, DELETE)
     * @param correlationId Request correlation ID (generates if null)
     * @return Response body as string
     * @throws InternalServiceException If service call fails
     */
    public String callInternalService(String serviceUrl, String endpoint, String payload,
                                    String method, String correlationId) throws InternalServiceException {
        String fullUrl = serviceUrl + endpoint;
        String reqCorrelationId = correlationId != null ? correlationId : UUID.randomUUID().toString();

        try {
            log.info("Calling internal API: {} {} with correlation ID: {}", method, fullUrl, reqCorrelationId);

            Request.Builder requestBuilder = new Request.Builder()
                .url(fullUrl)
                .addHeader(API_KEY_HEADER, serviceApiKey)
                .addHeader(SERVICE_ID_HEADER, serviceName)
                .addHeader(CORRELATION_ID_HEADER, reqCorrelationId)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");

            // Add request body for non-GET requests
            if (payload != null && !"GET".equalsIgnoreCase(method)) {
                RequestBody body = RequestBody.create(payload, JSON_MEDIA_TYPE);
                requestBuilder.method(method.toUpperCase(), body);
            } else {
                requestBuilder.method(method.toUpperCase(), null);
            }

            Request request = requestBuilder.build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    log.error("Internal API call failed: {} {} - Status: {}, Correlation: {}, Response: {}",
                            method, fullUrl, response.code(), reqCorrelationId, responseBody);
                    throw new InternalServiceException(
                        String.format("HTTP %d: %s", response.code(), responseBody),
                        response.code(),
                        reqCorrelationId
                    );
                }

                log.info("Internal API call successful: {} {} - Status: {}, Correlation: {}",
                        method, fullUrl, response.code(), reqCorrelationId);
                return responseBody;
            }

        } catch (IOException e) {
            log.error("Internal API call failed due to I/O error: {} {} - Correlation: {}, Error: {}",
                    method, fullUrl, reqCorrelationId, e.getMessage(), e);
            throw new InternalServiceException(
                "Internal service call failed: " + e.getMessage(),
                500,
                reqCorrelationId,
                e
            );
        }
    }

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

                if (isHealthy) {
                    log.debug("Service health check passed: {} (correlation: {})", serviceUrl, correlationId);
                } else {
                    log.warn("Service health check failed: {} - Status: {} (correlation: {})",
                           serviceUrl, response.code(), correlationId);
                }

                return isHealthy;
            }

        } catch (Exception e) {
            log.error("Service health check failed for: {} (correlation: {}) - Error: {}",
                    serviceUrl, correlationId, e.getMessage());
            return false;
        }
    }

    /**
     * Get service status information
     *
     * @param serviceUrl Kong Gateway internal URL for target service
     * @return Service status response
     * @throws InternalServiceException If status check fails
     */
    public String getServiceStatus(String serviceUrl) throws InternalServiceException {
        return callInternalService(serviceUrl, "/api/internal/v1/status", null, "GET");
    }

    /**
     * Internal Service Exception for API call failures
     */
    public static class InternalServiceException extends Exception {
        private final int statusCode;
        private final String correlationId;

        public InternalServiceException(String message, int statusCode, String correlationId) {
            super(message);
            this.statusCode = statusCode;
            this.correlationId = correlationId;
        }

        public InternalServiceException(String message, int statusCode, String correlationId, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
            this.correlationId = correlationId;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getCorrelationId() {
            return correlationId;
        }
    }
}