package com.trademaster.subscription.security;

import java.util.UUID;

/**
 * Security Context for External Access Requests
 * 
 * MANDATORY: Immutable security context record
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record SecurityContext(
    UUID userId,
    String sessionId,
    String ipAddress,
    String userAgent,
    String requestPath,
    long timestamp
) {
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID userId;
        private String sessionId;
        private String ipAddress;
        private String userAgent;
        private String requestPath;
        private long timestamp = System.currentTimeMillis();
        
        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder requestPath(String requestPath) {
            this.requestPath = requestPath;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SecurityContext build() {
            return new SecurityContext(userId, sessionId, ipAddress, userAgent, requestPath, timestamp);
        }
    }
}