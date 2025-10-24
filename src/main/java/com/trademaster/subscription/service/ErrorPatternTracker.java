package com.trademaster.subscription.service;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Error Pattern Tracker
 * MANDATORY: Single Responsibility - Error pattern analysis only
 * MANDATORY: Rule #5 - <200 lines per class
 *
 * Tracks error patterns, user error statistics, and provides analysis.
 * Separated from ErrorTrackingService to maintain SRP.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class ErrorPatternTracker {

    private final ConcurrentHashMap<String, ErrorTrackingInfo> errorPatterns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> userErrorCounts = new ConcurrentHashMap<>();

    /**
     * Track error pattern for analysis
     */
    public String trackErrorPattern(ErrorTrackingInfo errorInfo, String errorType, Throwable throwable) {
        String patternKey = createPatternKey(errorType, throwable);
        errorPatterns.merge(patternKey, errorInfo, (existing, newError) -> {
            existing.incrementCount();
            existing.setLastOccurrence(newError.getTimestamp());
            return existing;
        });
        return patternKey;
    }

    /**
     * Track user-specific error count
     */
    public void trackUserError(String userId) {
        java.util.Optional.ofNullable(userId)
            .ifPresent(uid -> userErrorCounts.merge(uid, 1, Integer::sum));
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
     * Get error pattern count by key
     */
    public int getPatternCount(String patternKey) {
        return java.util.Optional.ofNullable(errorPatterns.get(patternKey))
            .map(ErrorTrackingInfo::getCount)
            .orElse(0);
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
     * Create pattern key for error grouping
     * MANDATORY: Functional Programming - Rule #3 (NO if-else)
     */
    private String createPatternKey(String errorType, Throwable throwable) {
        return java.util.Optional.ofNullable(throwable)
            .map(t -> String.format("%s:%s", errorType, t.getClass().getSimpleName()))
            .orElse(errorType);
    }

    /**
     * Error tracking information
     */
    @lombok.Data
    @Builder
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
