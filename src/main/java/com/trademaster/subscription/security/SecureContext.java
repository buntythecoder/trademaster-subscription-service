package com.trademaster.subscription.security;

import java.util.UUID;

/**
 * Secure Context after successful security validation
 * 
 * @author TradeMaster Development Team  
 * @version 1.0.0
 */
public record SecureContext(
    UUID userId,
    String sessionId,
    String correlationId,
    RiskResult riskResult,
    long validatedAt
) {}