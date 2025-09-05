package com.trademaster.subscription.integration;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * User Profile Service Integration Client
 * 
 * MANDATORY: Zero Trust Security - External Service Access
 * MANDATORY: Circuit Breaker Pattern for Resilience
 * MANDATORY: Functional Programming Patterns
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker userProfileServiceCircuitBreaker;
    private final Retry userProfileServiceRetry;
    
    @Value("${trademaster.services.user-profile.base-url:http://localhost:8083}")
    private String userProfileServiceBaseUrl;
    
    /**
     * Get user profile
     */
    public CompletableFuture<Result<UserProfileResponse, String>> getUserProfile(UUID userId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.debug("Getting user profile for: {}", userId);
                    
                    String url = userProfileServiceBaseUrl + "/api/v1/users/" + userId + "/profile";
                    UserProfileResponse response = restTemplate.getForObject(url, UserProfileResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("User profile retrieval failed for user: {}", userId, e);
                    return Result.<UserProfileResponse, String>failure("User profile retrieval failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Update user subscription tier in profile
     */
    public CompletableFuture<Result<Void, String>> updateUserSubscriptionTier(UUID userId, String subscriptionTier) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.info("Updating subscription tier for user: {} to tier: {}", userId, subscriptionTier);
                    
                    UpdateSubscriptionTierRequest request = new UpdateSubscriptionTierRequest(subscriptionTier);
                    String url = userProfileServiceBaseUrl + "/api/v1/users/" + userId + "/subscription-tier";
                    restTemplate.put(url, request);
                    
                    return Result.<Void, String>success(null);
                } catch (Exception e) {
                    log.error("User subscription tier update failed for user: {}", userId, e);
                    return Result.<Void, String>failure("User subscription tier update failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Get user preferences
     */
    public CompletableFuture<Result<UserPreferencesResponse, String>> getUserPreferences(UUID userId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.debug("Getting user preferences for: {}", userId);
                    
                    String url = userProfileServiceBaseUrl + "/api/v1/users/" + userId + "/preferences";
                    UserPreferencesResponse response = restTemplate.getForObject(url, UserPreferencesResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("User preferences retrieval failed for user: {}", userId, e);
                    return Result.<UserPreferencesResponse, String>failure("User preferences retrieval failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Validate user exists and is active
     */
    public CompletableFuture<Result<Boolean, String>> validateUser(UUID userId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.debug("Validating user existence for: {}", userId);
                    
                    String url = userProfileServiceBaseUrl + "/api/v1/users/" + userId + "/validate";
                    UserValidationResponse response = restTemplate.getForObject(url, UserValidationResponse.class);
                    
                    return Result.success(response != null && response.isValid() && response.isActive());
                } catch (Exception e) {
                    log.error("User validation failed for user: {}", userId, e);
                    return Result.<Boolean, String>failure("User validation failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        Supplier<Result<T, String>> decoratedSupplier = Retry.decorateSupplier(userProfileServiceRetry, operation);
        return userProfileServiceCircuitBreaker.executeSupplier(decoratedSupplier);
    }
    
    // DTOs for User Profile Service Integration
    
    public record UserProfileResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String status,
        String subscriptionTier,
        boolean emailVerified
    ) {}
    
    public record UpdateSubscriptionTierRequest(
        String subscriptionTier
    ) {}
    
    public record UserPreferencesResponse(
        UUID userId,
        boolean emailNotifications,
        boolean smsNotifications,
        String timezone,
        String language,
        String currency
    ) {}
    
    public record UserValidationResponse(
        UUID userId,
        boolean isValid,
        boolean isActive,
        String status
    ) {}
}