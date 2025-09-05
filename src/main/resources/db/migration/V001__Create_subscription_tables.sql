-- V001__Create_subscription_tables.sql
-- TradeMaster Subscription Service Database Schema
-- Author: TradeMaster Development Team
-- Version: 1.0.0

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create subscriptions table
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    tier VARCHAR(50) NOT NULL CHECK (tier IN ('FREE', 'PRO', 'AI_PREMIUM', 'INSTITUTIONAL')),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'TRIAL', 'EXPIRED', 'SUSPENDED', 'CANCELLED', 'PAUSED', 'UPGRADE_PENDING', 'DOWNGRADE_PENDING', 'TERMINATED')),
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' CHECK (billing_cycle IN ('MONTHLY', 'QUARTERLY', 'ANNUAL')),
    monthly_price DECIMAL(10,2) NOT NULL,
    billing_amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    start_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    next_billing_date TIMESTAMP,
    last_billing_date TIMESTAMP,
    trial_end_date TIMESTAMP,
    failed_billing_attempts INTEGER NOT NULL DEFAULT 0,
    auto_renewal BOOLEAN NOT NULL DEFAULT true,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    payment_method_id UUID,
    gateway_customer_id VARCHAR(100),
    gateway_subscription_id VARCHAR(100),
    promotion_discount DECIMAL(5,4) DEFAULT 0.0000,
    promotion_code VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for subscriptions table
CREATE INDEX idx_subscription_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscription_status ON subscriptions(status);
CREATE INDEX idx_subscription_tier ON subscriptions(tier);
CREATE INDEX idx_subscription_billing_date ON subscriptions(next_billing_date);
CREATE INDEX idx_subscription_active ON subscriptions(status, next_billing_date);
CREATE INDEX idx_subscription_gateway_customer ON subscriptions(gateway_customer_id);
CREATE INDEX idx_subscription_gateway_subscription ON subscriptions(gateway_subscription_id);
CREATE INDEX idx_subscription_promotion_code ON subscriptions(promotion_code);

-- Create usage_tracking table
CREATE TABLE usage_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    feature_name VARCHAR(50) NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    usage_limit BIGINT NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    reset_date TIMESTAMP NOT NULL,
    reset_frequency_days INTEGER NOT NULL DEFAULT 30,
    limit_exceeded BOOLEAN NOT NULL DEFAULT false,
    first_exceeded_at TIMESTAMP,
    exceeded_count INTEGER NOT NULL DEFAULT 0,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_usage_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- Create indexes for usage_tracking table
CREATE INDEX idx_usage_user_feature ON usage_tracking(user_id, feature_name);
CREATE INDEX idx_usage_subscription ON usage_tracking(subscription_id);
CREATE INDEX idx_usage_period ON usage_tracking(period_start, period_end);
CREATE INDEX idx_usage_reset_date ON usage_tracking(reset_date);
CREATE UNIQUE INDEX uk_usage_user_feature_period ON usage_tracking(user_id, feature_name, period_start);

-- Create subscription_history table
CREATE TABLE subscription_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID NOT NULL,
    user_id UUID NOT NULL,
    change_type VARCHAR(50) NOT NULL CHECK (change_type IN (
        'CREATED', 'ACTIVATED', 'UPGRADED', 'DOWNGRADED', 'BILLING_CYCLE_CHANGED',
        'SUSPENDED', 'CANCELLED', 'TERMINATED', 'REACTIVATED', 'PAUSED', 'RESUMED',
        'TRIAL_STARTED', 'TRIAL_ENDED', 'PAYMENT_FAILED', 'PAYMENT_SUCCEEDED',
        'AUTO_RENEWAL_ENABLED', 'AUTO_RENEWAL_DISABLED', 'PRICE_CHANGED',
        'PROMOTION_APPLIED', 'PROMOTION_REMOVED'
    )),
    old_tier VARCHAR(50),
    new_tier VARCHAR(50),
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    old_billing_cycle VARCHAR(20),
    new_billing_cycle VARCHAR(20),
    old_monthly_price DECIMAL(10,2),
    new_monthly_price DECIMAL(10,2),
    old_billing_amount DECIMAL(10,2),
    new_billing_amount DECIMAL(10,2),
    change_reason TEXT,
    initiated_by VARCHAR(30) NOT NULL CHECK (initiated_by IN ('USER', 'SYSTEM', 'ADMIN', 'PAYMENT_GATEWAY', 'SCHEDULED_TASK')),
    changed_by_user_id UUID,
    payment_transaction_id UUID,
    metadata TEXT,
    effective_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_history_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- Create indexes for subscription_history table
CREATE INDEX idx_history_subscription_id ON subscription_history(subscription_id);
CREATE INDEX idx_history_user_id ON subscription_history(user_id);
CREATE INDEX idx_history_change_type ON subscription_history(change_type);
CREATE INDEX idx_history_created_at ON subscription_history(created_at);
CREATE INDEX idx_history_subscription_date ON subscription_history(subscription_id, created_at);
CREATE INDEX idx_history_effective_date ON subscription_history(effective_date);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_subscriptions_updated_at 
    BEFORE UPDATE ON subscriptions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_usage_tracking_updated_at 
    BEFORE UPDATE ON usage_tracking 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default data for development
INSERT INTO subscriptions (
    id, user_id, tier, status, billing_cycle, monthly_price, billing_amount,
    currency, start_date, auto_renewal, metadata
) VALUES 
(
    uuid_generate_v4(),
    uuid_generate_v4(),
    'FREE',
    'ACTIVE',
    'MONTHLY',
    0.00,
    0.00,
    'INR',
    CURRENT_TIMESTAMP,
    false,
    '{"development": "sample_free_account"}'
);

-- Create function to check subscription status
CREATE OR REPLACE FUNCTION check_subscription_access(p_user_id UUID, p_feature VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    subscription_record RECORD;
    usage_record RECORD;
BEGIN
    -- Get active subscription
    SELECT * INTO subscription_record
    FROM subscriptions 
    WHERE user_id = p_user_id 
    AND status IN ('ACTIVE', 'TRIAL', 'EXPIRED')
    AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)
    ORDER BY created_at DESC
    LIMIT 1;
    
    -- No active subscription
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;
    
    -- Check usage limits
    SELECT * INTO usage_record
    FROM usage_tracking
    WHERE user_id = p_user_id
    AND feature_name = p_feature
    AND period_start <= CURRENT_TIMESTAMP
    AND period_end > CURRENT_TIMESTAMP;
    
    -- No usage tracking record, allow access
    IF NOT FOUND THEN
        RETURN TRUE;
    END IF;
    
    -- Check if unlimited or within limits
    IF usage_record.usage_limit = -1 OR usage_record.usage_count < usage_record.usage_limit THEN
        RETURN TRUE;
    END IF;
    
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- Create function to increment usage
CREATE OR REPLACE FUNCTION increment_usage(p_user_id UUID, p_feature VARCHAR, p_amount BIGINT DEFAULT 1)
RETURNS BOOLEAN AS $$
DECLARE
    usage_record RECORD;
    new_count BIGINT;
BEGIN
    -- Get current usage record
    SELECT * INTO usage_record
    FROM usage_tracking
    WHERE user_id = p_user_id
    AND feature_name = p_feature
    AND period_start <= CURRENT_TIMESTAMP
    AND period_end > CURRENT_TIMESTAMP;
    
    -- No usage record found
    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;
    
    -- Calculate new count
    new_count := usage_record.usage_count + p_amount;
    
    -- Update usage
    UPDATE usage_tracking 
    SET usage_count = new_count,
        limit_exceeded = CASE 
            WHEN usage_limit = -1 THEN false
            WHEN new_count > usage_limit THEN true
            ELSE limit_exceeded
        END,
        exceeded_count = CASE
            WHEN usage_limit != -1 AND new_count > usage_limit AND NOT limit_exceeded THEN exceeded_count + 1
            ELSE exceeded_count
        END,
        first_exceeded_at = CASE
            WHEN usage_limit != -1 AND new_count > usage_limit AND first_exceeded_at IS NULL THEN CURRENT_TIMESTAMP
            ELSE first_exceeded_at
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = usage_record.id;
    
    -- Return whether usage is within limits
    RETURN (usage_record.usage_limit = -1 OR new_count <= usage_record.usage_limit);
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE subscriptions IS 'Core subscription management table with tier-based access control';
COMMENT ON TABLE usage_tracking IS 'Real-time usage tracking and limit enforcement';
COMMENT ON TABLE subscription_history IS 'Complete audit trail of subscription changes for compliance';

COMMENT ON COLUMN subscriptions.tier IS 'Subscription tier: FREE, PRO, AI_PREMIUM, INSTITUTIONAL';
COMMENT ON COLUMN subscriptions.status IS 'Current subscription status with state machine validation';
COMMENT ON COLUMN subscriptions.billing_cycle IS 'Billing frequency with automatic discount calculation';
COMMENT ON COLUMN subscriptions.monthly_price IS 'Base monthly price before discounts';
COMMENT ON COLUMN subscriptions.billing_amount IS 'Actual charged amount after discounts';
COMMENT ON COLUMN subscriptions.failed_billing_attempts IS 'Failed payment attempts for dunning management';
COMMENT ON COLUMN subscriptions.promotion_discount IS 'Applied promotional discount percentage (0.0000-1.0000)';

COMMENT ON COLUMN usage_tracking.usage_limit IS 'Maximum allowed usage (-1 for unlimited)';
COMMENT ON COLUMN usage_tracking.reset_frequency_days IS 'How often usage resets (1=daily, 30=monthly)';
COMMENT ON COLUMN usage_tracking.limit_exceeded IS 'Flag indicating if limit has been exceeded';

COMMENT ON FUNCTION check_subscription_access(UUID, VARCHAR) IS 'Check if user has access to a feature based on subscription and usage limits';
COMMENT ON FUNCTION increment_usage(UUID, VARCHAR, BIGINT) IS 'Increment feature usage and return whether still within limits';