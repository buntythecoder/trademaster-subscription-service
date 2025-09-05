package com.trademaster.subscription.service.command;

import com.trademaster.subscription.common.Result;

import java.util.concurrent.CompletableFuture;

/**
 * Command Pattern Interface for Subscription Operations
 * 
 * MANDATORY: Command Pattern - Advanced Design Pattern Rule #4
 * MANDATORY: Interface Segregation - SOLID Rule #2
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@FunctionalInterface
public interface SubscriptionCommand<T> {
    
    /**
     * Execute the subscription command asynchronously
     */
    CompletableFuture<Result<T, String>> execute();
    
    /**
     * Get command name for logging and metrics
     */
    default String getCommandName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Validate command parameters before execution
     */
    default Result<Void, String> validate() {
        return Result.success(null);
    }
}