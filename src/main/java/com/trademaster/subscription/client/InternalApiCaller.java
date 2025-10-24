package com.trademaster.subscription.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal API Caller
 * MANDATORY: Single Responsibility - API call execution only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles Kong Gateway-routed internal API calls between services.
 * Features: Kong API key authentication, correlation ID propagation,
 * Virtual Threads compatibility.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalApiCaller {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json");
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
    /**
     * MANDATORY: Rule #3 - No ternary/if-else, using Optional pattern
     */
    public String callInternalService(String serviceUrl, String endpoint, String payload,
                                    String method, String correlationId) throws InternalServiceException {
        String fullUrl = serviceUrl + endpoint;
        String reqCorrelationId = Optional.ofNullable(correlationId)
            .orElseGet(() -> UUID.randomUUID().toString());

        try {
            log.info("Calling internal API: {} {} with correlation ID: {}", method, fullUrl, reqCorrelationId);

            Request request = buildRequest(fullUrl, payload, method, reqCorrelationId);

            try (Response response = httpClient.newCall(request).execute()) {
                return handleResponse(response, method, fullUrl, reqCorrelationId);
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
     * Build HTTP request with headers and body
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    private Request buildRequest(String fullUrl, String payload, String method, String correlationId) {
        Request.Builder requestBuilder = new Request.Builder()
            .url(fullUrl)
            .addHeader(API_KEY_HEADER, serviceApiKey)
            .addHeader(SERVICE_ID_HEADER, serviceName)
            .addHeader(CORRELATION_ID_HEADER, correlationId)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json");

        RequestBody body = Optional.ofNullable(payload)
            .filter(p -> !"GET".equalsIgnoreCase(method))
            .map(p -> RequestBody.create(p, JSON_MEDIA_TYPE))
            .orElse(null);

        requestBuilder.method(method.toUpperCase(), body);

        return requestBuilder.build();
    }

    /**
     * Handle HTTP response and errors
     * MANDATORY: Rule #3 - No if-else/ternary, using Optional pattern
     */
    private String handleResponse(Response response, String method, String fullUrl, String correlationId)
            throws InternalServiceException, IOException {
        String responseBody = Optional.ofNullable(response.body())
            .map(body -> {
                try {
                    return body.string();
                } catch (IOException e) {
                    return "";
                }
            })
            .orElse("");

        return Optional.of(response.isSuccessful())
            .filter(Boolean::booleanValue)
            .map(success -> {
                log.info("Internal API call successful: {} {} - Status: {}, Correlation: {}",
                        method, fullUrl, response.code(), correlationId);
                return responseBody;
            })
            .orElseThrow(() -> {
                log.error("Internal API call failed: {} {} - Status: {}, Correlation: {}, Response: {}",
                        method, fullUrl, response.code(), correlationId, responseBody);
                return new InternalServiceException(
                    String.format("HTTP %d: %s", response.code(), responseBody),
                    response.code(),
                    correlationId
                );
            });
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
}
