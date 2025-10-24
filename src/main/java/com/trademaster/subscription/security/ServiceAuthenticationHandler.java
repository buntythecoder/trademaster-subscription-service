package com.trademaster.subscription.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Service Authentication Handler
 * MANDATORY: Single Responsibility - Internal API authentication logic only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Handles Kong API key authentication flow for internal service-to-service communication.
 * Coordinates authentication strategies: Kong consumer validation, direct API key validation.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class ServiceAuthenticationHandler {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String KONG_CONSUMER_ID_HEADER = "X-Consumer-ID";
    private static final String KONG_CONSUMER_USERNAME_HEADER = "X-Consumer-Username";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";

    @Value("${trademaster.security.service.api-key:pTB9KkzqJWNkFDUJHIFyDv5b1tSUpP4q}")
    private String fallbackServiceApiKey;

    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;

    private final ServiceAuthenticationSetter authenticationSetter;
    private final UnauthorizedResponseWriter responseWriter;

    public ServiceAuthenticationHandler(ServiceAuthenticationSetter authenticationSetter,
                                       UnauthorizedResponseWriter responseWriter) {
        this.authenticationSetter = authenticationSetter;
        this.responseWriter = responseWriter;
    }

    /**
     * Process internal API request authentication
     * MANDATORY: Rule #3 - Functional guard pattern, no if-else
     */
    public void processInternalRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                       FilterChain chain, String requestPath, String correlationId) {
        // Guard: Skip auth if disabled
        Optional.of(!serviceAuthEnabled)
            .filter(Boolean::booleanValue)
            .ifPresentOrElse(
                disabled -> {
                    log.warn("Service authentication disabled - allowing request: {}", requestPath);
                    authenticationSetter.setServiceAuthentication("dev-service");
                    try { chain.doFilter(httpRequest, httpResponse); }
                    catch (IOException | ServletException e) { throw new RuntimeException(e); }
                },
                () -> authenticateRequest(httpRequest, httpResponse, chain, requestPath, correlationId)
            );
    }

    /**
     * MANDATORY: Rule #3 - Extracted authentication logic
     */
    private void authenticateRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                     FilterChain chain, String requestPath, String correlationId) {
        String kongConsumerId = httpRequest.getHeader(KONG_CONSUMER_ID_HEADER);
        String kongConsumerUsername = httpRequest.getHeader(KONG_CONSUMER_USERNAME_HEADER);

        // Kong consumer validation using Optional
        Optional.ofNullable(kongConsumerUsername)
            .filter(StringUtils::hasText)
            .filter(username -> StringUtils.hasText(kongConsumerId))
            .ifPresentOrElse(
                username -> {
                    log.info("Kong validated consumer '{}' (ID: {}) for correlation: {}, granting SERVICE access",
                            username, kongConsumerId, correlationId);
                    authenticationSetter.setServiceAuthentication(username);
                    try { chain.doFilter(httpRequest, httpResponse); }
                    catch (IOException | ServletException e) { throw new RuntimeException(e); }
                },
                () -> validateDirectApiKey(httpRequest, httpResponse, chain, requestPath, correlationId)
            );
    }

    /**
     * MANDATORY: Rule #3 - Direct API key validation
     */
    private void validateDirectApiKey(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                      FilterChain chain, String requestPath, String correlationId) {
        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        String serviceId = httpRequest.getHeader(SERVICE_ID_HEADER);

        Optional.ofNullable(apiKey)
            .filter(StringUtils::hasText)
            .ifPresentOrElse(
                key -> processApiKey(httpRequest, httpResponse, chain, requestPath, correlationId, key, serviceId),
                () -> {
                    log.error("Missing Kong consumer headers and X-API-Key header for: {} (correlation: {})",
                            requestPath, correlationId);
                    try {
                        responseWriter.sendUnauthorizedResponse(httpResponse, "Missing service API key or Kong consumer headers", correlationId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * MANDATORY: Rule #3 - Process API key
     */
    private void processApiKey(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                              FilterChain chain, String requestPath, String correlationId,
                              String apiKey, String serviceId) {

        // MANDATORY: Rule #3 - No if-else/ternary, using Optional pattern
        Optional.ofNullable(fallbackServiceApiKey)
            .filter(StringUtils::hasText)
            .ifPresentOrElse(
                fallbackKey -> {
                    // Validate API key if fallback configured
                    Optional.of(fallbackKey.equals(apiKey))
                        .filter(Boolean::booleanValue)
                        .ifPresentOrElse(
                            valid -> completeAuthentication(httpRequest, httpResponse, chain, requestPath, correlationId, serviceId),
                            () -> {
                                log.error("Invalid API key for direct service request: {} (correlation: {}, service: {})",
                                        requestPath, correlationId, serviceId);
                                try {
                                    responseWriter.sendUnauthorizedResponse(httpResponse, "Invalid service API key", correlationId);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        );
                },
                () -> {
                    // No fallback key configured - Kong dynamic auth
                    log.info("Direct API key request with Kong dynamic authentication (no fallback validation): {} (correlation: {})",
                            requestPath, correlationId);
                    completeAuthentication(httpRequest, httpResponse, chain, requestPath, correlationId, serviceId);
                }
            );
    }

    /**
     * MANDATORY: Rule #3 - Complete authentication process
     */
    private void completeAuthentication(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                       FilterChain chain, String requestPath, String correlationId, String serviceId) {
        String authenticationName = Optional.ofNullable(serviceId)
            .filter(StringUtils::hasText)
            .orElse("direct-service-call");

        authenticationSetter.setServiceAuthentication(authenticationName);
        log.info("Direct API key authentication successful for: {} (correlation: {}, service: {})",
                requestPath, correlationId, serviceId);

        try {
            chain.doFilter(httpRequest, httpResponse);
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }
}
