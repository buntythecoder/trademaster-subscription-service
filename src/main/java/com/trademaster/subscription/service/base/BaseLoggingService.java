package com.trademaster.subscription.service.base;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Logging Service
 * MANDATORY: DRY Principle - Shared infrastructure for logging services
 * MANDATORY: Single Responsibility - Provides common logging infrastructure only
 *
 * @author TradeMaster Development Team
 */
@Slf4j
public abstract class BaseLoggingService {

    protected static final Logger SECURITY_AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");
    protected static final Logger BUSINESS_AUDIT = LoggerFactory.getLogger("BUSINESS_AUDIT");
    protected static final Logger PERFORMANCE = LoggerFactory.getLogger("PERFORMANCE");

    // Context Keys
    protected static final String CORRELATION_ID = "correlationId";
    protected static final String USER_ID = "userId";
    protected static final String SESSION_ID = "sessionId";
    protected static final String IP_ADDRESS = "ipAddress";
    protected static final String USER_AGENT = "userAgent";
    protected static final String SUBSCRIPTION_ID = "subscriptionId";
    protected static final String TRANSACTION_ID = "transactionId";
}
