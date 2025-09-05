package com.trademaster.subscription.integration;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Payment Service Integration Client
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
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker paymentServiceCircuitBreaker;
    private final Retry paymentServiceRetry;
    
    @Value("${trademaster.services.payment.base-url:http://localhost:8082}")
    private String paymentServiceBaseUrl;
    
    /**
     * Process subscription payment
     */
    public CompletableFuture<Result<PaymentResponse, String>> processPayment(PaymentRequest paymentRequest) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.info("Processing payment for subscription: {}", paymentRequest.subscriptionId());
                    
                    String url = paymentServiceBaseUrl + "/api/v1/payments/process";
                    PaymentResponse response = restTemplate.postForObject(url, paymentRequest, PaymentResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("Payment processing failed for subscription: {}", paymentRequest.subscriptionId(), e);
                    return Result.<PaymentResponse, String>failure("Payment processing failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Create payment method
     */
    public CompletableFuture<Result<PaymentMethodResponse, String>> createPaymentMethod(CreatePaymentMethodRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.info("Creating payment method for user: {}", request.userId());
                    
                    String url = paymentServiceBaseUrl + "/api/v1/payment-methods";
                    PaymentMethodResponse response = restTemplate.postForObject(url, request, PaymentMethodResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("Payment method creation failed for user: {}", request.userId(), e);
                    return Result.<PaymentMethodResponse, String>failure("Payment method creation failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Refund payment
     */
    public CompletableFuture<Result<RefundResponse, String>> refundPayment(RefundRequest refundRequest) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.info("Processing refund for payment: {}", refundRequest.paymentId());
                    
                    String url = paymentServiceBaseUrl + "/api/v1/payments/" + refundRequest.paymentId() + "/refund";
                    RefundResponse response = restTemplate.postForObject(url, refundRequest, RefundResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("Refund processing failed for payment: {}", refundRequest.paymentId(), e);
                    return Result.<RefundResponse, String>failure("Refund processing failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    /**
     * Get payment status
     */
    public CompletableFuture<Result<PaymentStatusResponse, String>> getPaymentStatus(UUID paymentId) {
        return CompletableFuture.supplyAsync(() -> 
            executeWithResilience(() -> {
                try {
                    log.debug("Getting payment status for: {}", paymentId);
                    
                    String url = paymentServiceBaseUrl + "/api/v1/payments/" + paymentId + "/status";
                    PaymentStatusResponse response = restTemplate.getForObject(url, PaymentStatusResponse.class);
                    
                    return Result.success(response);
                } catch (Exception e) {
                    log.error("Payment status retrieval failed for payment: {}", paymentId, e);
                    return Result.<PaymentStatusResponse, String>failure("Payment status retrieval failed: " + e.getMessage());
                }
            }),
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
    
    // Circuit breaker helper
    private <T> Result<T, String> executeWithResilience(Supplier<Result<T, String>> operation) {
        Supplier<Result<T, String>> decoratedSupplier = Retry.decorateSupplier(paymentServiceRetry, operation);
        return paymentServiceCircuitBreaker.executeSupplier(decoratedSupplier);
    }
    
    // DTOs for Payment Service Integration
    
    public record PaymentRequest(
        UUID subscriptionId,
        UUID userId,
        UUID paymentMethodId,
        BigDecimal amount,
        String currency,
        String description
    ) {}
    
    public record PaymentResponse(
        UUID paymentId,
        String status,
        String gatewayTransactionId,
        BigDecimal amount,
        String currency
    ) {}
    
    public record CreatePaymentMethodRequest(
        UUID userId,
        String paymentType,
        String token,
        boolean setAsDefault
    ) {}
    
    public record PaymentMethodResponse(
        UUID paymentMethodId,
        String status,
        String last4Digits,
        String expiryMonth,
        String expiryYear
    ) {}
    
    public record RefundRequest(
        UUID paymentId,
        BigDecimal amount,
        String reason
    ) {}
    
    public record RefundResponse(
        UUID refundId,
        String status,
        BigDecimal refundedAmount
    ) {}
    
    public record PaymentStatusResponse(
        UUID paymentId,
        String status,
        BigDecimal amount,
        String currency,
        String gatewayTransactionId
    ) {}
}