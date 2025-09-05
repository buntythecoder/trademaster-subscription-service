-- V002__Fix_entity_alignment.sql
-- TradeMaster Subscription Service - Fix Entity-Migration Alignment
-- Author: TradeMaster Development Team
-- Version: 2.0.0
-- Fixes critical gaps identified in comprehensive audit

-- 1. Add missing PAYMENT_FAILED status to subscriptions table CHECK constraint
ALTER TABLE subscriptions DROP CONSTRAINT subscriptions_status_check;
ALTER TABLE subscriptions ADD CONSTRAINT subscriptions_status_check 
    CHECK (status IN ('PENDING', 'ACTIVE', 'TRIAL', 'EXPIRED', 'SUSPENDED', 'CANCELLED', 
                      'PAUSED', 'UPGRADE_PENDING', 'DOWNGRADE_PENDING', 'TERMINATED', 'PAYMENT_FAILED'));

-- 2. Add missing columns to subscriptions table (present in entity but not in migration)
ALTER TABLE subscriptions ADD COLUMN activated_date TIMESTAMP;
ALTER TABLE subscriptions ADD COLUMN cancelled_date TIMESTAMP;
ALTER TABLE subscriptions ADD COLUMN upgraded_date TIMESTAMP;
ALTER TABLE subscriptions ADD COLUMN last_billed_date TIMESTAMP;

-- 3. Add missing action column to subscription_history table
ALTER TABLE subscription_history ADD COLUMN action VARCHAR(50);

-- 4. Add performance indexes identified during audit
CREATE INDEX idx_subscription_user_status ON subscriptions(user_id, status);
CREATE INDEX idx_subscription_cancelled_at ON subscriptions(cancelled_at) WHERE cancelled_at IS NOT NULL;
CREATE INDEX idx_subscription_failed_attempts ON subscriptions(failed_billing_attempts) WHERE failed_billing_attempts > 0;

-- 5. Update subscription_history change_type constraint to include additional action types
ALTER TABLE subscription_history DROP CONSTRAINT subscription_history_change_type_check;
ALTER TABLE subscription_history ADD CONSTRAINT subscription_history_change_type_check 
    CHECK (change_type IN (
        'CREATED', 'ACTIVATED', 'UPGRADED', 'DOWNGRADED', 'BILLING_CYCLE_CHANGED',
        'SUSPENDED', 'CANCELLED', 'TERMINATED', 'REACTIVATED', 'PAUSED', 'RESUMED',
        'TRIAL_STARTED', 'TRIAL_ENDED', 'PAYMENT_FAILED', 'PAYMENT_SUCCEEDED',
        'AUTO_RENEWAL_ENABLED', 'AUTO_RENEWAL_DISABLED', 'PRICE_CHANGED',
        'PROMOTION_APPLIED', 'PROMOTION_REMOVED', 'SUBSCRIPTION_BILLED'
    ));

-- 6. Create optimized view for MRR calculation (reduces complex JPQL query load)
CREATE OR REPLACE VIEW subscription_mrr AS
SELECT 
    SUM(CASE billing_cycle 
        WHEN 'MONTHLY' THEN billing_amount
        WHEN 'QUARTERLY' THEN billing_amount / 3
        WHEN 'ANNUAL' THEN billing_amount / 12
        ELSE 0 
    END) as total_mrr,
    COUNT(*) as active_subscription_count,
    tier,
    CURRENT_TIMESTAMP as calculated_at
FROM subscriptions 
WHERE status IN ('ACTIVE', 'TRIAL', 'EXPIRED')
GROUP BY tier;

-- 7. Create optimized view for ARR calculation  
CREATE OR REPLACE VIEW subscription_arr AS
SELECT 
    SUM(CASE billing_cycle 
        WHEN 'MONTHLY' THEN billing_amount * 12
        WHEN 'QUARTERLY' THEN billing_amount * 4
        WHEN 'ANNUAL' THEN billing_amount
        ELSE 0 
    END) as total_arr,
    COUNT(*) as active_subscription_count,
    tier,
    CURRENT_TIMESTAMP as calculated_at
FROM subscriptions 
WHERE status IN ('ACTIVE', 'TRIAL', 'EXPIRED')
GROUP BY tier;

-- 8. Add database-level version control trigger (enforces optimistic locking)
CREATE OR REPLACE FUNCTION increment_version()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.version = OLD.version + 1;
        RETURN NEW;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_subscriptions_version
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION increment_version();

-- 9. Add comments for new columns and views
COMMENT ON COLUMN subscriptions.activated_date IS 'Date when subscription was first activated';
COMMENT ON COLUMN subscriptions.cancelled_date IS 'Date when subscription was cancelled (duplicate of cancelled_at for compatibility)';
COMMENT ON COLUMN subscriptions.upgraded_date IS 'Date when subscription was last upgraded';
COMMENT ON COLUMN subscriptions.last_billed_date IS 'Date when subscription was last successfully billed';
COMMENT ON COLUMN subscription_history.action IS 'Action performed on subscription (for compatibility with entity)';

COMMENT ON VIEW subscription_mrr IS 'Monthly Recurring Revenue calculation by tier - optimized for analytics queries';
COMMENT ON VIEW subscription_arr IS 'Annual Recurring Revenue calculation by tier - optimized for analytics queries';

-- 10. Create function to safely handle subscription status transitions
CREATE OR REPLACE FUNCTION transition_subscription_status(
    p_subscription_id UUID,
    p_new_status VARCHAR(50),
    p_reason TEXT DEFAULT NULL,
    p_initiated_by VARCHAR(30) DEFAULT 'SYSTEM'
) RETURNS BOOLEAN AS $$
DECLARE
    current_subscription RECORD;
    valid_transitions VARCHAR(50)[];
    is_valid BOOLEAN := FALSE;
BEGIN
    -- Get current subscription
    SELECT * INTO current_subscription
    FROM subscriptions 
    WHERE id = p_subscription_id;
    
    -- Check if subscription exists
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Subscription not found: %', p_subscription_id;
    END IF;
    
    -- Define valid transitions based on current status
    valid_transitions := CASE current_subscription.status
        WHEN 'PENDING' THEN ARRAY['ACTIVE', 'TRIAL', 'SUSPENDED', 'PAYMENT_FAILED', 'TERMINATED']
        WHEN 'ACTIVE' THEN ARRAY['CANCELLED', 'SUSPENDED', 'PAUSED', 'PAYMENT_FAILED', 'UPGRADE_PENDING', 'DOWNGRADE_PENDING', 'EXPIRED']
        WHEN 'TRIAL' THEN ARRAY['ACTIVE', 'CANCELLED', 'SUSPENDED', 'PAYMENT_FAILED', 'EXPIRED']
        WHEN 'EXPIRED' THEN ARRAY['ACTIVE', 'SUSPENDED', 'TERMINATED']
        WHEN 'SUSPENDED' THEN ARRAY['ACTIVE', 'TERMINATED']
        WHEN 'PAYMENT_FAILED' THEN ARRAY['ACTIVE', 'SUSPENDED', 'TERMINATED']
        WHEN 'CANCELLED' THEN ARRAY['TERMINATED', 'ACTIVE']
        WHEN 'PAUSED' THEN ARRAY['ACTIVE', 'CANCELLED', 'TERMINATED']
        WHEN 'UPGRADE_PENDING' THEN ARRAY['ACTIVE', 'SUSPENDED']
        WHEN 'DOWNGRADE_PENDING' THEN ARRAY['ACTIVE', 'SUSPENDED']
        WHEN 'TERMINATED' THEN ARRAY[]::VARCHAR(50)[]
        ELSE ARRAY[]::VARCHAR(50)[]
    END;
    
    -- Check if transition is valid
    SELECT p_new_status = ANY(valid_transitions) INTO is_valid;
    
    IF NOT is_valid THEN
        RAISE EXCEPTION 'Invalid status transition from % to %', current_subscription.status, p_new_status;
    END IF;
    
    -- Update subscription status
    UPDATE subscriptions 
    SET status = p_new_status,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_subscription_id;
    
    -- Log the transition in history
    INSERT INTO subscription_history (
        subscription_id, user_id, change_type, action,
        old_status, new_status, change_reason, 
        initiated_by, effective_date
    ) VALUES (
        p_subscription_id, current_subscription.user_id, 'STATUS_CHANGED', 'SUBSCRIPTION_STATUS_UPDATED',
        current_subscription.status, p_new_status, p_reason,
        p_initiated_by, CURRENT_TIMESTAMP
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION transition_subscription_status(UUID, VARCHAR, TEXT, VARCHAR) IS 'Safely transition subscription status with validation and audit trail';