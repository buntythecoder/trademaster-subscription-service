-- Remove duplicate columns from subscriptions table
-- Phase 4A.3: Subscription entity refactoring
-- Keeping: cancelled_at, last_billed_date
-- Removing: cancelled_date (duplicate of cancelled_at), last_billing_date (duplicate of last_billed_date)

-- Drop duplicate cancelled_date column (keeping cancelled_at)
ALTER TABLE subscriptions DROP COLUMN IF EXISTS cancelled_date;

-- Drop duplicate last_billing_date column (keeping last_billed_date)
ALTER TABLE subscriptions DROP COLUMN IF EXISTS last_billing_date;
