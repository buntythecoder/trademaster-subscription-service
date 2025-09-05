package com.trademaster.subscription.service;

import com.trademaster.subscription.config.CorrelationConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Error Tracking Service
 * 
 * Provides comprehensive error tracking, correlation, and analysis capabilities.
 * Tracks error patterns, rates, and provides insights for system reliability.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorTrackingService {

    private final StructuredLoggingService loggingService;
    private final MeterRegistry meterRegistry;
    
    // Error tracking metrics
    private final Counter errorCounter;
    private final Counter criticalErrorCounter;
    private final Timer errorProcessingTimer;
    
    // In-memory error tracking (in production, use Redis or database)
    private final ConcurrentHashMap<String, ErrorTrackingInfo> errorPatterns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> userErrorCounts = new ConcurrentHashMap<>();

    public ErrorTrackingService(StructuredLoggingService loggingService, MeterRegistry meterRegistry) {
        this.loggingService = loggingService;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.errorCounter = Counter.builder("subscription.errors.total")
                .description("Total number of errors tracked")
                .register(meterRegistry);
                
        this.criticalErrorCounter = Counter.builder("subscription.errors.critical")
                .description("Number of critical errors tracked")
                .register(meterRegistry);
                
        this.errorProcessingTimer = Timer.builder("subscription.error.processing.duration")
                .description("Time taken to process and track errors")
                .register(meterRegistry);
    }

    /**
     * Track application error with full context
     */
    @Async("subscriptionProcessingExecutor")
    public CompletableFuture<Void> trackError(String errorType, String errorMessage, 
                                            Throwable throwable, Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                String correlationId = CorrelationConfig.CorrelationContext.getCorrelationId();
                String requestId = CorrelationConfig.CorrelationContext.getRequestId();
                String userId = CorrelationConfig.CorrelationContext.getUserId();
                
                // Create error tracking entry
                ErrorTrackingInfo errorInfo = ErrorTrackingInfo.builder()
                        .errorId(UUID.randomUUID().toString())
                        .correlationId(correlationId)
                        .requestId(requestId)
                        .userId(userId)
                        .errorType(errorType)
                        .errorMessage(errorMessage)
                        .exceptionType(throwable != null ? throwable.getClass().getSimpleName() : "Unknown")
                        .timestamp(LocalDateTime.now())
                        .context(context)
                        .build();
                
                // Track error pattern
                String patternKey = createPatternKey(errorType, throwable);
                errorPatterns.merge(patternKey, errorInfo, (existing, newError) -> {
                    existing.incrementCount();
                    existing.setLastOccurrence(newError.getTimestamp());
                    return existing;
                });
                
                // Track user error count
                if (userId != null) {
                    userErrorCounts.merge(userId, 1, Integer::sum);
                }
                
                // Update metrics
                errorCounter.increment();
                
                if (isCriticalError(errorType, throwable)) {
                    criticalErrorCounter.increment();
                    
                    // Log critical error immediately
                    loggingService.logError(
                        "critical_error_detected",
                        errorMessage,
                        errorType,
                        throwable,
                        Map.of(
                            "correlationId", correlationId,
                            "requestId", requestId,
                            "userId", userId != null ? userId : "unknown",
                            "severity", "CRITICAL"
                        )
                    );
                }
                
                // Log structured error
                loggingService.logError(
                    "error_tracked",
                    errorMessage,
                    errorType,
                    throwable,
                    Map.of(
                        "errorId", errorInfo.getErrorId(),
                        "correlationId", correlationId,
                        "requestId", requestId,
                        "userId", userId != null ? userId : "unknown",
                        "patternKey", patternKey,
                        "occurrenceCount", errorPatterns.get(patternKey).getCount()
                    )
                );
                
                log.debug("Error tracked: {} (correlationId: {}, errorId: {})", 
                        errorType, correlationId, errorInfo.getErrorId());
                        
            } catch (Exception e) {
                log.error("Failed to track error", e);
            } finally {
                sample.stop(errorProcessingTimer);
            }
        });
    }

    /**
     * Track validation error with field details
     */
    public CompletableFuture<Void> trackValidationError(Map<String, Object> fieldErrors, String source) {
        return trackError(
            "VALIDATION_ERROR",
            "Request validation failed",
            null,
            Map.of(
                "fieldErrors", fieldErrors,
                "source", source,
                "errorCategory", "validation"
            )
        );
    }

    /**
     * Track security incident
     */
    public CompletableFuture<Void> trackSecurityIncident(String incidentType, String details, 
                                                        String severity, String userId) {
        return trackError(
            "SECURITY_INCIDENT",
            details,
            null,
            Map.of(
                "incidentType", incidentType,
                "severity", severity,
                "affectedUserId", userId != null ? userId : "unknown",
                "errorCategory", "security"
            )
        );
    }

    /**
     * Track business logic error
     */
    public CompletableFuture<Void> trackBusinessError(String businessRule, String violation, 
                                                     String entityId, String entityType) {
        return trackError(
            "BUSINESS_RULE_VIOLATION",
            violation,
            null,
            Map.of(
                "businessRule", businessRule,
                "entityId", entityId,
                "entityType", entityType,
                "errorCategory", "business"
            )
        );
    }

    /**
     * Get error patterns for analysis
     */
    public Map<String, ErrorTrackingInfo> getErrorPatterns() {
        return Map.copyOf(errorPatterns);
    }

    /**
     * Get user error statistics
     */
    public Map<String, Integer> getUserErrorCounts() {
        return Map.copyOf(userErrorCounts);
    }

    /**
     * Check if error is critical
     */
    private boolean isCriticalError(String errorType, Throwable throwable) {
        if (throwable == null) {
            return "SECURITY_INCIDENT".equals(errorType) || 
                   "DATA_CORRUPTION".equals(errorType) ||
                   "PAYMENT_FAILURE".equals(errorType);
        }
        
        String exceptionType = throwable.getClass().getSimpleName();
        return exceptionType.contains("Security") ||
               exceptionType.contains("Authentication") ||
               exceptionType.contains("OutOfMemory") ||
               exceptionType.contains("SQL") ||
               "INTERNAL_SERVER_ERROR".equals(errorType);
    }

    /**
     * Create pattern key for error grouping
     */
    private String createPatternKey(String errorType, Throwable throwable) {
        if (throwable != null) {
            return String.format("%s:%s", errorType, throwable.getClass().getSimpleName());
        }
        return errorType;
    }

    /**
     * Clear old error patterns (cleanup method)
     */
    @Async("subscriptionProcessingExecutor")
    public void cleanupOldPatterns() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        errorPatterns.entrySet().removeIf(entry -> 
                entry.getValue().getLastOccurrence().isBefore(cutoff));
        
        log.info("Cleaned up old error patterns, remaining: {}", errorPatterns.size());
    }

    /**
     * Error tracking information
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorTrackingInfo {
        private String errorId;
        private String correlationId;
        private String requestId;
        private String userId;
        private String errorType;
        private String errorMessage;
        private String exceptionType;
        private LocalDateTime timestamp;
        private LocalDateTime lastOccurrence;
        private Map<String, Object> context;
        @Builder.Default
        private int count = 1;

        public void incrementCount() {
            this.count++;
        }
    }
}