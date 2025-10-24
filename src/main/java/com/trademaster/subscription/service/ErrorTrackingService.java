package com.trademaster.subscription.service;

import com.trademaster.subscription.config.CorrelationConfig;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Error Tracking Service
 * MANDATORY: Single Responsibility - Error tracking orchestration only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Coordinates error tracking using specialized services for metrics and pattern analysis.
 * Separated metrics collection and pattern tracking to maintain SRP.
 *
 * @author TradeMaster Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorTrackingService {

    private final StructuredLoggingService loggingService;
    private final ErrorMetricsCollector metricsCollector;
    private final ErrorPatternTracker patternTracker;

    /**
     * Track application error with full context
     * MANDATORY: Method Length - Rule #5 (Max 15 lines per method)
     */
    @Async("subscriptionProcessingExecutor")
    public CompletableFuture<Void> trackError(String errorType, String errorMessage,
                                            Throwable throwable, Map<String, Object> context) {
        return CompletableFuture.runAsync(() -> {
            Timer.Sample sample = metricsCollector.startErrorProcessingTimer();
            try {
                CorrelationContext ctx = extractCorrelationContext();
                ErrorPatternTracker.ErrorTrackingInfo errorInfo = createErrorInfo(errorType, errorMessage, throwable, context, ctx);
                String patternKey = patternTracker.trackErrorPattern(errorInfo, errorType, throwable);
                patternTracker.trackUserError(ctx.userId());
                metricsCollector.incrementErrorCount();
                handleCriticalError(errorType, errorMessage, throwable, ctx);
                logStructuredError(errorMessage, errorType, throwable, errorInfo, ctx, patternKey);
                log.debug("Error tracked: {} (correlationId: {}, errorId: {})",
                        errorType, ctx.correlationId(), errorInfo.getErrorId());
            } catch (Exception e) {
                log.error("Failed to track error", e);
            } finally {
                metricsCollector.stopErrorProcessingTimer(sample);
            }
        });
    }

    /**
     * Extract correlation context from thread local
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private CorrelationContext extractCorrelationContext() {
        return new CorrelationContext(
            CorrelationConfig.CorrelationContext.getCorrelationId(),
            CorrelationConfig.CorrelationContext.getRequestId(),
            CorrelationConfig.CorrelationContext.getUserId()
        );
    }

    /**
     * Create error tracking info object
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private ErrorPatternTracker.ErrorTrackingInfo createErrorInfo(String errorType, String errorMessage,
                                             Throwable throwable, Map<String, Object> context,
                                             CorrelationContext ctx) {
        return ErrorPatternTracker.ErrorTrackingInfo.builder()
            .errorId(UUID.randomUUID().toString())
            .correlationId(ctx.correlationId())
            .requestId(ctx.requestId())
            .userId(ctx.userId())
            .errorType(errorType)
            .errorMessage(errorMessage)
            .exceptionType(throwable != null ? throwable.getClass().getSimpleName() : "Unknown")
            .timestamp(LocalDateTime.now())
            .context(context)
            .build();
    }

    /**
     * Handle critical error with immediate logging
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private void handleCriticalError(String errorType, String errorMessage,
                                     Throwable throwable, CorrelationContext ctx) {
        java.util.Optional.of(isCriticalError(errorType, throwable))
            .filter(isCritical -> isCritical)
            .ifPresent(__ -> {
                metricsCollector.incrementCriticalErrorCount();
                loggingService.logError("critical_error_detected", errorMessage, errorType, throwable,
                    Map.of("correlationId", ctx.correlationId(), "requestId", ctx.requestId(),
                           "userId", ctx.userId() != null ? ctx.userId() : "unknown",
                           "severity", "CRITICAL"));
            });
    }

    /**
     * Log structured error with full context
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    private void logStructuredError(String errorMessage, String errorType, Throwable throwable,
                                    ErrorPatternTracker.ErrorTrackingInfo errorInfo, CorrelationContext ctx,
                                    String patternKey) {
        loggingService.logError("error_tracked", errorMessage, errorType, throwable,
            Map.of("errorId", errorInfo.getErrorId(), "correlationId", ctx.correlationId(),
                   "requestId", ctx.requestId(),
                   "userId", ctx.userId() != null ? ctx.userId() : "unknown",
                   "patternKey", patternKey,
                   "occurrenceCount", patternTracker.getPatternCount(patternKey)));
    }

    /**
     * Correlation context record
     */
    private record CorrelationContext(String correlationId, String requestId, String userId) {}

    /**
     * Get error patterns for analysis
     */
    public Map<String, ErrorPatternTracker.ErrorTrackingInfo> getErrorPatterns() {
        return patternTracker.getErrorPatterns();
    }

    /**
     * Get user error statistics
     */
    public Map<String, Integer> getUserErrorCounts() {
        return patternTracker.getUserErrorCounts();
    }

    /**
     * Clean up old error patterns
     */
    public void cleanupOldPatterns() {
        patternTracker.cleanupOldPatterns();
    }

    /**
     * Check if error is critical
     * MANDATORY: Functional Programming - Rule #3 (NO if-else)
     */
    private boolean isCriticalError(String errorType, Throwable throwable) {
        return java.util.Optional.ofNullable(throwable)
            .map(t -> {
                String exceptionType = t.getClass().getSimpleName();
                return exceptionType.contains("Security") ||
                       exceptionType.contains("Authentication") ||
                       exceptionType.contains("OutOfMemory") ||
                       exceptionType.contains("SQL") ||
                       "INTERNAL_SERVER_ERROR".equals(errorType);
            })
            .orElseGet(() ->
                "SECURITY_INCIDENT".equals(errorType) ||
                "DATA_CORRUPTION".equals(errorType) ||
                "PAYMENT_FAILURE".equals(errorType)
            );
    }
}