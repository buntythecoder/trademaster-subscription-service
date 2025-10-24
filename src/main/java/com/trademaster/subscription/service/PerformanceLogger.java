package com.trademaster.subscription.service;

import com.trademaster.subscription.constants.LogFieldConstants;
import com.trademaster.subscription.service.base.BaseLoggingService;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Performance Logger
 * MANDATORY: Single Responsibility - Handles performance and application logging only
 * MANDATORY: Performance Monitoring - All performance metrics logged for analysis
 *
 * Logs performance metrics, database operations, service calls, and application events.
 * Enables performance analysis and operational troubleshooting.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class PerformanceLogger extends BaseLoggingService {

    /**
     * Log performance metric
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logPerformanceMetric(String operation, String component, long durationMs,
                                   String status, Map<String, Object> additionalMetrics) {
        PERFORMANCE.info("Performance metric",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("component", component),
            StructuredArguments.kv("duration_ms", durationMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now()),
            StructuredArguments.kv("metrics", additionalMetrics)
        );
    }

    /**
     * Log database performance
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logDatabasePerformance(String query, long executionTimeMs, String status,
                                     int recordsAffected) {
        PERFORMANCE.info("Database performance",
            StructuredArguments.kv(LogFieldConstants.OPERATION, "database_query"),
            StructuredArguments.kv("query_type", query),
            StructuredArguments.kv("execution_time_ms", executionTimeMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("records_affected", recordsAffected),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }

    /**
     * Log service performance
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logServicePerformance(String service, String operation, long responseTimeMs,
                                    String status, String responseCode) {
        PERFORMANCE.info("Service performance",
            StructuredArguments.kv(LogFieldConstants.OPERATION, "service_call"),
            StructuredArguments.kv("service", service),
            StructuredArguments.kv("service_operation", operation),
            StructuredArguments.kv("response_time_ms", responseTimeMs),
            StructuredArguments.kv("status", status),
            StructuredArguments.kv("response_code", responseCode),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }

    /**
     * Log application info message
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logInfo(String operation, String message, Map<String, Object> context) {
        log.info("{}",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("message", message),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now()),
            StructuredArguments.kv("context", context)
        );
    }

    /**
     * Log application error message
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logError(String operation, String errorMessage, String errorCode,
                        Throwable throwable, Map<String, Object> context) {
        log.error("Error in operation",
            StructuredArguments.kv(LogFieldConstants.OPERATION, operation),
            StructuredArguments.kv("error_message", errorMessage),
            StructuredArguments.kv("error_code", errorCode),
            StructuredArguments.kv("exception_class", throwable != null ? throwable.getClass().getSimpleName() : null),
            StructuredArguments.kv("stack_trace", throwable != null ? throwable.getMessage() : null),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now()),
            StructuredArguments.kv("context", context),
            throwable
        );
    }
}
