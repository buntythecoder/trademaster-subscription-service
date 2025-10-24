package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Security Facade for External Access Control
 * 
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: External access requires SecurityFacade + SecurityMediator
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFacade {
    
    private final SecurityMediator mediator;
    
    /**
     * Secure access wrapper for external operations
     */
    public <T> CompletableFuture<Result<T, SecurityError>> secureAccess(
            SecurityContext context,
            Function<Void, CompletableFuture<Result<T, String>>> operation) {
        
        return CompletableFuture.supplyAsync(() -> {
            String correlationId = UUID.randomUUID().toString();
            
            return mediator.mediateAccess(context, correlationId)
                .flatMap(secureContext -> 
                    executeSecureOperation(operation, secureContext, correlationId)
                        .join()
                        .mapError(SecurityError::new)
                )
                .onSuccess(result -> logSuccessfulAccess(context, correlationId))
                .onFailure(error -> logFailedAccess(context, error, correlationId));
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
    
    /**
     * Execute operation within security boundary
     * MANDATORY: Rule #3 - No if-else, using Optional pattern
     */
    private <T> CompletableFuture<Result<T, String>> executeSecureOperation(
            Function<Void, CompletableFuture<Result<T, String>>> operation,
            SecureContext secureContext,
            String correlationId) {

        return operation.apply(null)
            .handle((result, throwable) ->
                Optional.ofNullable(throwable)
                    .map(error -> {
                        log.error("Secure operation failed with correlation: {}", correlationId, error);
                        return Result.<T, String>failure("Operation failed: " + error.getMessage());
                    })
                    .orElse(result)
            );
    }
    
    private void logSuccessfulAccess(SecurityContext context, String correlationId) {
        log.info("Secure access granted for user: {} with correlation: {}", 
            context.userId(), correlationId);
    }
    
    private void logFailedAccess(SecurityContext context, SecurityError error, String correlationId) {
        log.warn("Secure access denied for user: {} with correlation: {}, reason: {}", 
            context.userId(), correlationId, error.message());
    }
}