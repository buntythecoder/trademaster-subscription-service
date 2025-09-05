package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Security Mediator for Coordinating Security Components
 * 
 * MANDATORY: Zero Trust Security Policy - Rule #6
 * MANDATORY: Mediator coordinates authentication, authorization, risk assessment
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityMediator {
    
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final RiskAssessmentService riskAssessmentService;
    
    /**
     * Mediate access through all security components
     */
    public Result<SecureContext, SecurityError> mediateAccess(
            SecurityContext context, String correlationId) {
        
        return authenticationService.authenticate(context)
            .flatMap(authResult -> authorizationService.authorize(authResult, context))
            .flatMap(authzResult -> riskAssessmentService.assessRisk(authzResult, context))
            .flatMap(riskResult -> createSecureContext(riskResult, context, correlationId))
            .onSuccess(secureContext -> auditService.logSecureAccess(context, correlationId))
            .onFailure(error -> auditService.logSecurityFailure(context, new SecurityError(error), correlationId))
            .mapError(error -> switch (error) {
                case String msg when msg.contains("authentication") -> 
                    new SecurityError("Authentication failed: " + msg);
                case String msg when msg.contains("authorization") -> 
                    new SecurityError("Authorization denied: " + msg);
                case String msg when msg.contains("risk") -> 
                    new SecurityError("Risk assessment failed: " + msg);
                default -> new SecurityError("Security validation failed: " + error);
            });
    }
    
    private Result<SecureContext, String> createSecureContext(
            RiskResult riskResult, SecurityContext context, String correlationId) {
        
        return switch (riskResult.level()) {
            case LOW, MEDIUM -> Result.success(new SecureContext(
                context.userId(),
                context.sessionId(),
                correlationId,
                riskResult,
                System.currentTimeMillis()
            ));
            case HIGH, CRITICAL -> Result.failure(
                "High risk access denied for user: " + context.userId()
            );
        };
    }
}