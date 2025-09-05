package com.trademaster.subscription.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Audit Service for Security Event Logging
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    /**
     * Log successful secure access
     */
    public void logSecureAccess(SecurityContext context, String correlationId) {
        log.info("SECURITY_ACCESS_GRANTED - User: {}, Session: {}, IP: {}, Path: {}, Correlation: {}",
            context.userId(), 
            context.sessionId(), 
            context.ipAddress(), 
            context.requestPath(),
            correlationId
        );
    }
    
    /**
     * Log security failure
     */
    public void logSecurityFailure(SecurityContext context, SecurityError error, String correlationId) {
        log.warn("SECURITY_ACCESS_DENIED - User: {}, IP: {}, Path: {}, Error: {}, Type: {}, Correlation: {}",
            context.userId(),
            context.ipAddress(),
            context.requestPath(),
            error.message(),
            error.type(),
            correlationId
        );
    }
    
    /**
     * Log high-risk access attempt
     */
    public void logHighRiskAccess(SecurityContext context, RiskResult riskResult, String correlationId) {
        log.warn("HIGH_RISK_ACCESS - User: {}, IP: {}, Risk: {}, Score: {}, Reason: {}, Correlation: {}",
            context.userId(),
            context.ipAddress(),
            riskResult.level(),
            riskResult.score(),
            riskResult.reason(),
            correlationId
        );
    }
}