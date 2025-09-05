package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Risk Assessment Service for Zero Trust Security
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentService {
    
    /**
     * Assess risk level for the request
     */
    public Result<RiskResult, String> assessRisk(
            AuthorizationService.AuthzResult authzResult, SecurityContext context) {
        
        return Result.tryExecute(() -> {
            double riskScore = calculateRiskScore(context);
            
            RiskResult.RiskLevel level = switch ((int) riskScore) {
                case int score when score < 30 -> RiskResult.RiskLevel.LOW;
                case int score when score < 60 -> RiskResult.RiskLevel.MEDIUM;
                case int score when score < 85 -> RiskResult.RiskLevel.HIGH;
                default -> RiskResult.RiskLevel.CRITICAL;
            };
            
            String reason = buildRiskReason(riskScore, context);
            
            return new RiskResult(level, riskScore, reason);
        }).mapError(exception -> "Risk assessment failed: " + exception.getMessage());
    }
    
    private double calculateRiskScore(SecurityContext context) {
        double score = 0.0;
        
        // IP-based risk assessment
        score += assessIpRisk(context.ipAddress());
        
        // User agent risk assessment  
        score += assessUserAgentRisk(context.userAgent());
        
        // Time-based risk assessment
        score += assessTimeRisk(context.timestamp());
        
        // Request pattern risk assessment
        score += assessRequestRisk(context.requestPath());
        
        return Math.min(score, 100.0);
    }
    
    private double assessIpRisk(String ipAddress) {
        // Pattern matching for IP risk assessment
        return switch (ipAddress) {
            case String ip when ip.startsWith("10.") -> 5.0; // Internal network
            case String ip when ip.startsWith("192.168.") -> 5.0; // Private network
            case String ip when ip.startsWith("127.") -> 0.0; // Localhost
            default -> 15.0; // External IP
        };
    }
    
    private double assessUserAgentRisk(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return 30.0; // Missing user agent is suspicious
        }
        
        return switch (userAgent.toLowerCase()) {
            case String ua when ua.contains("chrome") -> 5.0;
            case String ua when ua.contains("firefox") -> 5.0;
            case String ua when ua.contains("safari") -> 5.0;
            case String ua when ua.contains("bot") -> 25.0;
            case String ua when ua.contains("curl") -> 20.0;
            default -> 10.0;
        };
    }
    
    private double assessTimeRisk(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - timestamp);
        
        // Request too old or in future
        return timeDiff > 300000 ? 15.0 : 0.0; // 5 minutes tolerance
    }
    
    private double assessRequestRisk(String requestPath) {
        return switch (requestPath) {
            case String path when path.contains("/admin") -> 25.0;
            case String path when path.contains("/delete") -> 20.0;
            case String path when path.contains("/cancel") -> 15.0;
            default -> 5.0;
        };
    }
    
    private String buildRiskReason(double score, SecurityContext context) {
        return switch ((int) score) {
            case int s when s < 30 -> "Low risk - normal user behavior";
            case int s when s < 60 -> "Medium risk - some suspicious indicators";
            case int s when s < 85 -> "High risk - multiple risk factors detected";
            default -> "Critical risk - blocking access";
        };
    }
}