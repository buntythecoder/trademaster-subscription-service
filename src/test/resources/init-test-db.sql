-- Test Database Initialization Script
-- PostgreSQL TestContainers setup for Subscription Service

-- Create test user if not exists (handled by TestContainers)
-- TestContainers automatically creates the specified user and database

-- Basic test data setup
-- Note: Flyway migrations will handle the actual schema creation
-- This file is just a placeholder for TestContainer initialization

-- Ensure proper timezone for testing
SET timezone = 'UTC';

-- Any test-specific setup can go here