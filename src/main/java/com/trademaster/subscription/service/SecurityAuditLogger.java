package com.trademaster.subscription.service;

import com.trademaster.subscription.constants.LogFieldConstants;
import com.trademaster.subscription.service.base.BaseLoggingService;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Security Audit Logger
 * MANDATORY: Single Responsibility - Handles security event audit logging only
 * MANDATORY: Security - All security events logged for incident response
 *
 * Logs all security-relevant events for audit trail and threat detection.
 * Includes security incidents, rate limit violations, and unauthorized access attempts.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class SecurityAuditLogger extends BaseLoggingService {

    /**
     * Log security incident
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logSecurityIncident(String incidentType, String severity, String userId,
                                  String ipAddress, String userAgent, Map<String, Object> details) {
        SECURITY_AUDIT.warn("Security incident detected",
            StructuredArguments.kv("incident_type", incidentType),
            StructuredArguments.kv(LogFieldConstants.SEVERITY, severity),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv(LogFieldConstants.OPERATION, "security_incident"),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now()),
            StructuredArguments.kv("details", details)
        );
    }

    /**
     * Log rate limit violation
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logRateLimitViolation(String endpoint, String userId, String ipAddress,
                                    String userAgent, String violationType) {
        SECURITY_AUDIT.warn("Rate limit violation",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("violation_type", violationType),
            StructuredArguments.kv(LogFieldConstants.OPERATION, "rate_limit_violation"),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }

    /**
     * Log unauthorized access attempt
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void logUnauthorizedAccess(String resource, String userId, String ipAddress,
                                     String userAgent, String attemptedAction) {
        SECURITY_AUDIT.warn("Unauthorized access attempt",
            StructuredArguments.kv("resource", resource),
            StructuredArguments.kv(LogFieldConstants.USER_ID, userId),
            StructuredArguments.kv("ip_address", ipAddress),
            StructuredArguments.kv("user_agent", userAgent),
            StructuredArguments.kv("attempted_action", attemptedAction),
            StructuredArguments.kv(LogFieldConstants.OPERATION, "unauthorized_access"),
            StructuredArguments.kv(LogFieldConstants.TIMESTAMP, Instant.now())
        );
    }
}
