package com.trademaster.subscription.service;

import com.trademaster.subscription.service.base.BaseLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Context Manager
 * MANDATORY: Single Responsibility - Handles MDC context management only
 * MANDATORY: Functional Programming - No if-else statements
 *
 * Manages Mapped Diagnostic Context (MDC) for structured logging across Virtual Threads.
 * Ensures correlation IDs and context are preserved throughout async operations.
 *
 * @author TradeMaster Development Team
 */
@Service
@Slf4j
public class ContextManager extends BaseLoggingService {

    /**
     * Set correlation ID for request tracking
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId != null ? correlationId : UUID.randomUUID().toString());
    }

    /**
     * Set user context for logging
     * MANDATORY: Functional Programming - Rule #3 (NO if-else)
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void setUserContext(String userId, String sessionId, String ipAddress, String userAgent) {
        java.util.Optional.ofNullable(userId).ifPresent(id -> MDC.put(USER_ID, id));
        java.util.Optional.ofNullable(sessionId).ifPresent(sid -> MDC.put(SESSION_ID, sid));
        java.util.Optional.ofNullable(ipAddress).ifPresent(ip -> MDC.put(IP_ADDRESS, ip));
        java.util.Optional.ofNullable(userAgent).ifPresent(ua -> MDC.put(USER_AGENT, ua));
    }

    /**
     * Set business context for logging
     * MANDATORY: Functional Programming - Rule #3 (NO if-else)
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void setBusinessContext(String subscriptionId, String transactionId) {
        java.util.Optional.ofNullable(subscriptionId).ifPresent(subId -> MDC.put(SUBSCRIPTION_ID, subId));
        java.util.Optional.ofNullable(transactionId).ifPresent(txnId -> MDC.put(TRANSACTION_ID, txnId));
    }

    /**
     * Clear all MDC context
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void clearContext() {
        MDC.clear();
    }

    /**
     * Clear specific context key
     * MANDATORY: Max 15 lines, complexity ≤7
     */
    public void clearContext(String key) {
        MDC.remove(key);
    }
}
