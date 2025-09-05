package com.trademaster.subscription.security;

/**
 * Risk Assessment Result
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record RiskResult(
    RiskLevel level,
    double score,
    String reason
) {
    
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}