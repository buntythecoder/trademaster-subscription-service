package com.trademaster.subscription.security;

import com.trademaster.subscription.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Authorization Service for Zero Trust Security
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    
    /**
     * Authorize user access to resources
     */
    public Result<AuthzResult, String> authorize(
            AuthenticationService.AuthResult authResult, SecurityContext context) {
        
        return Result.tryExecute(() -> {
            // Pattern matching for authorization
            return switch (checkPermissions(authResult, context)) {
                case GRANTED -> new AuthzResult(authResult.userId(), "AUTHORIZED", System.currentTimeMillis());
                case INSUFFICIENT_PERMISSIONS -> throw new SecurityException("Insufficient permissions");
                case RESOURCE_NOT_ACCESSIBLE -> throw new SecurityException("Resource not accessible");
                case RATE_LIMITED -> throw new SecurityException("Rate limit exceeded");
                default -> throw new SecurityException("Authorization failed");
            };
        }).mapError(exception -> "Authorization failed: " + exception.getMessage());
    }
    
    private PermissionResult checkPermissions(
            AuthenticationService.AuthResult authResult, SecurityContext context) {

        // Use Optional pattern to handle null paths (Rule #3 - no if-else)
        return java.util.Optional.ofNullable(context.requestPath())
            .map(path -> switch (path) {
                case String p when p.contains("/subscriptions") -> PermissionResult.GRANTED;
                case String p when p.contains("/admin") -> PermissionResult.INSUFFICIENT_PERMISSIONS;
                default -> PermissionResult.GRANTED;
            })
            .orElse(PermissionResult.GRANTED);  // Default for null paths
    }
    
    private enum PermissionResult {
        GRANTED, INSUFFICIENT_PERMISSIONS, RESOURCE_NOT_ACCESSIBLE, RATE_LIMITED
    }
    
    public record AuthzResult(
        java.util.UUID userId,
        String status,
        long authorizedAt
    ) {}
}