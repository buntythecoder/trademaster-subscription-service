package com.trademaster.subscription.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Internal Service Communication Client
 * MANDATORY: Single Responsibility - Facade for internal service communication
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Unified facade for Kong Gateway-routed internal API calls between services.
 * Delegates to specialized components following Single Responsibility Principle.
 *
 * Components:
 * - InternalHttpClientConfig: HTTP client configuration
 * - InternalApiCaller: Core API call execution
 * - InternalHealthChecker: Health check operations
 * - InternalServiceException: Exception handling
 *
 * Features:
 * - Kong API key authentication
 * - Correlation ID propagation
 * - Virtual Threads compatibility (OkHttp)
 * - Circuit breaker integration
 * - Structured error handling
 *
 * @author TradeMaster Development Team
 * @version 2.0.0
 * @since 2025-01-09
 */
@Component
@RequiredArgsConstructor
public class InternalServiceClient {

    private final InternalApiCaller apiCaller;
    private final InternalHealthChecker healthChecker;

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
        return apiCaller.callInternalService(serviceUrl, endpoint, payload, method);
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
        return apiCaller.callInternalService(serviceUrl, endpoint, payload, method, correlationId);
    }

    /**
     * Health check for internal service connectivity
     *
     * @param serviceUrl Kong Gateway internal URL for target service
     * @return true if service is healthy, false otherwise
     */
    public boolean checkServiceHealth(String serviceUrl) {
        return healthChecker.checkServiceHealth(serviceUrl);
    }

    /**
     * Health check with correlation ID
     *
     * @param serviceUrl    Kong Gateway internal URL for target service
     * @param correlationId Request correlation ID
     * @return true if service is healthy, false otherwise
     */
    public boolean checkServiceHealth(String serviceUrl, String correlationId) {
        return healthChecker.checkServiceHealth(serviceUrl, correlationId);
    }

    /**
     * Get service status information
     *
     * @param serviceUrl Kong Gateway internal URL for target service
     * @return Service status response
     * @throws InternalServiceException If status check fails
     */
    public String getServiceStatus(String serviceUrl) throws InternalServiceException {
        return apiCaller.getServiceStatus(serviceUrl);
    }
}
