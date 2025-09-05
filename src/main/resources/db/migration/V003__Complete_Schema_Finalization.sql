-- V003__Complete_Schema_Finalization.sql
-- TradeMaster Subscription Service - Complete Schema Finalization
-- Author: TradeMaster Development Team
-- Version: 3.0.0
-- Completes final schema alignment for production readiness

-- 1. Add last_used_date column to usage_tracking table for clearer semantics
-- (This provides explicit field instead of using updated_at as proxy)
ALTER TABLE usage_tracking ADD COLUMN last_used_date TIMESTAMP;

-- 2. Add last_reset_date column to usage_tracking table for audit trail
-- (This provides explicit field instead of using reset_date as proxy)
ALTER TABLE usage_tracking ADD COLUMN last_reset_date TIMESTAMP;

-- 3. Create additional performance indexes for production workload
CREATE INDEX idx_subscription_status_tier ON subscriptions(status, tier);
CREATE INDEX idx_subscription_billing_date_status ON subscriptions(next_billing_date, status) WHERE next_billing_date IS NOT NULL;
CREATE INDEX idx_usage_tracking_feature_usage ON usage_tracking(feature_name, usage_count, usage_limit);
CREATE INDEX idx_usage_tracking_last_used ON usage_tracking(last_used_date) WHERE last_used_date IS NOT NULL;
CREATE INDEX idx_usage_tracking_last_reset ON usage_tracking(last_reset_date) WHERE last_reset_date IS NOT NULL;

-- 4. Create production-ready monitoring views
CREATE OR REPLACE VIEW subscription_health_metrics AS
SELECT 
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_count,
    COUNT(*) FILTER (WHERE status = 'TRIAL') as trial_count,
    COUNT(*) FILTER (WHERE status = 'EXPIRED') as expired_count,
    COUNT(*) FILTER (WHERE status = 'SUSPENDED') as suspended_count,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancelled_count,
    COUNT(*) FILTER (WHERE status = 'PAYMENT_FAILED') as payment_failed_count,
    COUNT(*) FILTER (WHERE status = 'TERMINATED') as terminated_count,
    COUNT(*) FILTER (WHERE failed_billing_attempts > 0) as failed_billing_count,
    COUNT(*) FILTER (WHERE next_billing_date < CURRENT_TIMESTAMP AND status = 'ACTIVE') as overdue_billing_count,
    tier,
    CURRENT_TIMESTAMP as calculated_at
FROM subscriptions 
GROUP BY tier;

-- 5. Create usage analytics view
CREATE OR REPLACE VIEW usage_analytics AS
SELECT 
    feature_name,
    COUNT(*) as total_users,
    COUNT(*) FILTER (WHERE limit_exceeded = true) as users_over_limit,
    AVG(usage_count) as avg_usage,
    MAX(usage_count) as max_usage,
    SUM(usage_count) as total_usage,
    COUNT(*) FILTER (WHERE usage_count > 0) as active_users,
    CURRENT_TIMESTAMP as calculated_at
FROM usage_tracking 
WHERE period_start <= CURRENT_TIMESTAMP 
  AND period_end > CURRENT_TIMESTAMP
GROUP BY feature_name;

-- 6. Create subscription lifecycle analytics view
CREATE OR REPLACE VIEW subscription_lifecycle_analytics AS
SELECT 
    DATE_TRUNC('month', created_at) as month,
    tier,
    COUNT(*) FILTER (WHERE change_type = 'CREATED') as new_subscriptions,
    COUNT(*) FILTER (WHERE change_type = 'ACTIVATED') as activations,
    COUNT(*) FILTER (WHERE change_type = 'CANCELLED') as cancellations,
    COUNT(*) FILTER (WHERE change_type = 'UPGRADED') as upgrades,
    COUNT(*) FILTER (WHERE change_type = 'DOWNGRADED') as downgrades,
    COUNT(*) FILTER (WHERE change_type = 'PAYMENT_FAILED') as payment_failures,
    COUNT(*) FILTER (WHERE change_type = 'SUSPENDED') as suspensions,
    COUNT(*) FILTER (WHERE change_type = 'TERMINATED') as terminations
FROM subscription_history
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', created_at), tier
ORDER BY month DESC, tier;

-- 7. Create function for comprehensive subscription validation
CREATE OR REPLACE FUNCTION validate_subscription_integrity(p_subscription_id UUID)
RETURNS TABLE (
    validation_type VARCHAR(50),
    is_valid BOOLEAN,
    message TEXT
) AS $$
BEGIN
    -- Check subscription exists
    RETURN QUERY
    SELECT 'EXISTENCE'::VARCHAR(50), 
           EXISTS(SELECT 1 FROM subscriptions WHERE id = p_subscription_id),
           CASE WHEN EXISTS(SELECT 1 FROM subscriptions WHERE id = p_subscription_id)
                THEN 'Subscription exists'
                ELSE 'Subscription not found'
           END;
    
    -- Check billing consistency
    RETURN QUERY
    SELECT 'BILLING_CONSISTENCY'::VARCHAR(50),
           CASE WHEN s.billing_amount > 0 AND s.monthly_price > 0 
                THEN true
                ELSE false
           END,
           CASE WHEN s.billing_amount > 0 AND s.monthly_price > 0 
                THEN 'Billing amounts are consistent'
                ELSE 'Billing amounts need validation'
           END
    FROM subscriptions s
    WHERE s.id = p_subscription_id;
    
    -- Check status transitions
    RETURN QUERY
    SELECT 'STATUS_TRANSITIONS'::VARCHAR(50),
           CASE WHEN COUNT(sh.id) > 0 THEN true ELSE false END,
           CASE WHEN COUNT(sh.id) > 0 
                THEN 'Has status history'
                ELSE 'No status history found'
           END
    FROM subscription_history sh
    WHERE sh.subscription_id = p_subscription_id;
    
    -- Check usage limits
    RETURN QUERY
    SELECT 'USAGE_LIMITS'::VARCHAR(50),
           CASE WHEN COUNT(ut.id) > 0 THEN true ELSE false END,
           CASE WHEN COUNT(ut.id) > 0 
                THEN 'Usage tracking is configured'
                ELSE 'No usage tracking configured'
           END
    FROM usage_tracking ut
    WHERE ut.subscription_id = p_subscription_id;
END;
$$ LANGUAGE plpgsql;

-- 8. Create function to cleanup expired trial subscriptions
CREATE OR REPLACE FUNCTION cleanup_expired_trials()
RETURNS INTEGER AS $$
DECLARE
    expired_count INTEGER := 0;
    expired_subscription RECORD;
BEGIN
    FOR expired_subscription IN 
        SELECT id, user_id
        FROM subscriptions 
        WHERE status = 'TRIAL' 
        AND trial_end_date < CURRENT_TIMESTAMP
    LOOP
        -- Update to expired status
        UPDATE subscriptions 
        SET status = 'EXPIRED',
            updated_at = CURRENT_TIMESTAMP
        WHERE id = expired_subscription.id;
        
        -- Log the expiration
        INSERT INTO subscription_history (
            subscription_id, user_id, change_type, action,
            old_status, new_status, change_reason,
            initiated_by, effective_date
        ) VALUES (
            expired_subscription.id, expired_subscription.user_id, 
            'TRIAL_ENDED', 'TRIAL_EXPIRED',
            'TRIAL', 'EXPIRED', 'Trial period ended',
            'SYSTEM', CURRENT_TIMESTAMP
        );
        
        expired_count := expired_count + 1;
    END LOOP;
    
    RETURN expired_count;
END;
$$ LANGUAGE plpgsql;

-- 9. Create function for subscription metrics export (for analytics service integration)
CREATE OR REPLACE FUNCTION get_subscription_metrics(
    p_start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP - INTERVAL '30 days',
    p_end_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
RETURNS TABLE (
    metric_name VARCHAR(50),
    metric_value NUMERIC,
    tier VARCHAR(50),
    period_start TIMESTAMP,
    period_end TIMESTAMP
) AS $$
BEGIN
    -- Active subscriptions by tier
    RETURN QUERY
    SELECT 'ACTIVE_SUBSCRIPTIONS'::VARCHAR(50),
           COUNT(*)::NUMERIC,
           s.tier::VARCHAR(50),
           p_start_date,
           p_end_date
    FROM subscriptions s
    WHERE s.status = 'ACTIVE'
    AND s.created_at BETWEEN p_start_date AND p_end_date
    GROUP BY s.tier;
    
    -- MRR by tier
    RETURN QUERY
    SELECT 'MRR'::VARCHAR(50),
           SUM(CASE s.billing_cycle 
               WHEN 'MONTHLY' THEN s.billing_amount
               WHEN 'QUARTERLY' THEN s.billing_amount / 3
               WHEN 'ANNUAL' THEN s.billing_amount / 12
               ELSE 0 
           END)::NUMERIC,
           s.tier::VARCHAR(50),
           p_start_date,
           p_end_date
    FROM subscriptions s
    WHERE s.status IN ('ACTIVE', 'TRIAL')
    AND s.created_at BETWEEN p_start_date AND p_end_date
    GROUP BY s.tier;
    
    -- Churn rate by tier
    RETURN QUERY
    SELECT 'CHURN_RATE'::VARCHAR(50),
           (COUNT(*) FILTER (WHERE sh.change_type = 'CANCELLED')::NUMERIC / 
            NULLIF(COUNT(*) FILTER (WHERE sh.change_type = 'ACTIVATED'), 0) * 100)::NUMERIC,
           s.tier::VARCHAR(50),
           p_start_date,
           p_end_date
    FROM subscription_history sh
    JOIN subscriptions s ON sh.subscription_id = s.id
    WHERE sh.created_at BETWEEN p_start_date AND p_end_date
    GROUP BY s.tier;
END;
$$ LANGUAGE plpgsql;

-- 10. Add comprehensive comments for new elements
COMMENT ON COLUMN usage_tracking.last_used_date IS 'Explicit timestamp when feature was last used (clearer than using updated_at)';
COMMENT ON COLUMN usage_tracking.last_reset_date IS 'Explicit timestamp when usage was last reset (audit trail)';

COMMENT ON VIEW subscription_health_metrics IS 'Real-time subscription health dashboard metrics by tier';
COMMENT ON VIEW usage_analytics IS 'Feature usage analytics with limit enforcement statistics';
COMMENT ON VIEW subscription_lifecycle_analytics IS 'Monthly subscription lifecycle trends and business metrics';

COMMENT ON FUNCTION validate_subscription_integrity(UUID) IS 'Comprehensive validation of subscription data integrity and business rules';
COMMENT ON FUNCTION cleanup_expired_trials() IS 'Automated cleanup function for expired trial subscriptions - run via scheduled task';
COMMENT ON FUNCTION get_subscription_metrics(TIMESTAMP, TIMESTAMP) IS 'Export subscription metrics for analytics service integration';

-- 11. Create production-ready stored procedures for common operations

-- Reset usage for all users at billing cycle
CREATE OR REPLACE FUNCTION reset_usage_for_billing_cycle()
RETURNS INTEGER AS $$
DECLARE
    reset_count INTEGER := 0;
    usage_record RECORD;
BEGIN
    FOR usage_record IN 
        SELECT ut.id, ut.user_id, ut.feature_name, ut.period_start, ut.period_end
        FROM usage_tracking ut
        WHERE ut.reset_date <= CURRENT_TIMESTAMP
    LOOP
        -- Reset usage count
        UPDATE usage_tracking 
        SET usage_count = 0,
            limit_exceeded = false,
            exceeded_count = 0,
            first_exceeded_at = NULL,
            reset_date = CASE 
                WHEN reset_frequency_days = 1 THEN CURRENT_TIMESTAMP + INTERVAL '1 day'
                WHEN reset_frequency_days = 30 THEN CURRENT_TIMESTAMP + INTERVAL '1 month'
                ELSE CURRENT_TIMESTAMP + (reset_frequency_days || ' days')::INTERVAL
            END,
            last_reset_date = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = usage_record.id;
        
        reset_count := reset_count + 1;
    END LOOP;
    
    RETURN reset_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION reset_usage_for_billing_cycle() IS 'Automated reset of usage tracking for billing cycle - run via scheduled task';

-- 12. Grant appropriate permissions for application user
-- (These would be customized based on actual database user setup)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON subscriptions TO subscription_service_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON usage_tracking TO subscription_service_user;
-- GRANT SELECT, INSERT ON subscription_history TO subscription_service_user;
-- GRANT USAGE ON SEQUENCE subscriptions_id_seq TO subscription_service_user;

-- 13. Create final schema validation
DO $$
BEGIN
    -- Validate all tables exist
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'subscriptions') THEN
        RAISE EXCEPTION 'subscriptions table missing';
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'usage_tracking') THEN
        RAISE EXCEPTION 'usage_tracking table missing';
    END IF;
    
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'subscription_history') THEN
        RAISE EXCEPTION 'subscription_history table missing';
    END IF;
    
    -- Validate essential indexes exist
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_subscription_user_id') THEN
        RAISE EXCEPTION 'Critical index idx_subscription_user_id missing';
    END IF;
    
    RAISE NOTICE 'Schema validation completed successfully';
END $$;