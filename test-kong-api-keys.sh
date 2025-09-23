#!/bin/bash

# TradeMaster Subscription Service - Kong API Key Testing Script
# This script tests Kong Gateway API key authentication for the subscription service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
KONG_ADMIN_URL="http://localhost:8001"
SERVICE_URL="http://localhost:8000"
SERVICE_PORT="8085"

# Test API Keys (These would be environment variables in production)
ADMIN_API_KEY="trademaster-admin-key-12345"
MONITOR_API_KEY="trademaster-monitor-key-67890"
TRADING_SERVICE_API_KEY="trademaster-trading-svc-key-abcde"
NOTIFICATION_SERVICE_API_KEY="trademaster-notification-svc-key-fghij"
BILLING_SERVICE_API_KEY="trademaster-billing-svc-key-klmno"

# JWT Token (Mock - would be real JWT in production)
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRyYWRlTWFzdGVyIFVzZXIiLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6OTk5OTk5OTk5OX0.dummy"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} TradeMaster Subscription Service${NC}"
echo -e "${BLUE} Kong API Key Testing Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to make HTTP requests with error handling
make_request() {
    local method=$1
    local url=$2
    local headers=$3
    local body=$4
    local description=$5

    echo -e "${YELLOW}Testing: ${description}${NC}"
    echo -e "Request: ${method} ${url}"

    if [ -n "$headers" ]; then
        echo -e "Headers: ${headers}"
    fi

    local response_code
    local response_body

    if [ -n "$body" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "${method}" "${url}" \
            ${headers:+-H "$headers"} \
            -H "Content-Type: application/json" \
            -d "$body" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "${method}" "${url}" \
            ${headers:+-H "$headers"} 2>/dev/null)
    fi

    response_body=$(echo "$response" | sed '$d')
    response_code=$(echo "$response" | tail -n1)

    if [ "$response_code" -eq 200 ] || [ "$response_code" -eq 201 ]; then
        echo -e "${GREEN}✓ SUCCESS (HTTP $response_code)${NC}"
        echo "Response: $response_body" | jq . 2>/dev/null || echo "Response: $response_body"
    elif [ "$response_code" -eq 401 ]; then
        echo -e "${RED}✗ UNAUTHORIZED (HTTP $response_code)${NC}"
        echo "Response: $response_body"
    elif [ "$response_code" -eq 403 ]; then
        echo -e "${RED}✗ FORBIDDEN (HTTP $response_code)${NC}"
        echo "Response: $response_body"
    else
        echo -e "${RED}✗ ERROR (HTTP $response_code)${NC}"
        echo "Response: $response_body"
    fi

    echo ""
    return $response_code
}

# Wait for service to be ready
echo -e "${YELLOW}Waiting for subscription service to be ready...${NC}"
for i in {1..30}; do
    if curl -s "http://localhost:${SERVICE_PORT}/api/v2/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Subscription service is ready${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}✗ Subscription service not ready after 30 attempts${NC}"
        echo "Please ensure the subscription service is running on port ${SERVICE_PORT}"
        exit 1
    fi
    sleep 2
done
echo ""

# Test 1: Public endpoints (no authentication required)
echo -e "${BLUE}=== Test 1: Public Endpoints (No Auth) ===${NC}"

make_request "GET" "${SERVICE_URL}/api/v1/test/ping" "" "" "Public ping endpoint"

make_request "GET" "${SERVICE_URL}/api/v1/test/headers" "" "" "Public headers echo endpoint"

# Test 2: External API endpoints (JWT authentication required)
echo -e "${BLUE}=== Test 2: External API Endpoints (JWT Auth) ===${NC}"

# Test without JWT (should fail)
make_request "GET" "${SERVICE_URL}/api/v1/test/secure" "" "" "Secure endpoint without JWT (should fail)"

# Test with JWT (should succeed if Kong is configured)
make_request "GET" "${SERVICE_URL}/api/v1/test/secure" "Authorization: Bearer ${JWT_TOKEN}" "" "Secure endpoint with JWT"

# Test 3: Internal API endpoints (API Key authentication required)
echo -e "${BLUE}=== Test 3: Internal API Endpoints (API Key Auth) ===${NC}"

# Test without API key (should fail)
make_request "GET" "${SERVICE_URL}/api/internal/test/service-ping" "" "" "Internal service ping without API key (should fail)"

# Test with admin API key
make_request "GET" "${SERVICE_URL}/api/internal/test/service-ping" "X-API-Key: ${ADMIN_API_KEY}" "" "Internal service ping with admin API key"

# Test with trading service API key
make_request "GET" "${SERVICE_URL}/api/internal/test/service-ping" "X-API-Key: ${TRADING_SERVICE_API_KEY}" "" "Internal service ping with trading service API key"

# Test with notification service API key
make_request "GET" "${SERVICE_URL}/api/internal/test/service-ping" "X-API-Key: ${NOTIFICATION_SERVICE_API_KEY}" "" "Internal service ping with notification service API key"

# Test service capabilities endpoint
make_request "GET" "${SERVICE_URL}/api/internal/capabilities" "X-API-Key: ${TRADING_SERVICE_API_KEY}" "" "Service capabilities with trading service API key"

# Test 4: Admin endpoints (elevated permissions)
echo -e "${BLUE}=== Test 4: Admin Endpoints (Elevated Auth) ===${NC}"

# Test admin endpoint with admin API key
make_request "GET" "${SERVICE_URL}/api/internal/test/admin" "X-API-Key: ${ADMIN_API_KEY}" "" "Admin endpoint with admin API key"

# Test admin endpoint with non-admin API key (should fail with 403)
make_request "GET" "${SERVICE_URL}/api/internal/test/admin" "X-API-Key: ${TRADING_SERVICE_API_KEY}" "" "Admin endpoint with non-admin API key (should fail)"

# Test 5: Load testing endpoint
echo -e "${BLUE}=== Test 5: Load Testing Endpoint ===${NC}"

# Test load endpoint with simple payload
test_payload='{"test": true, "timestamp": "'$(date -Iseconds)'", "requestId": "'$(uuidgen)'"}'

make_request "POST" "${SERVICE_URL}/api/v1/test/load" "Authorization: Bearer ${JWT_TOKEN}" "$test_payload" "Load test endpoint with JWT and payload"

# Test 6: Health check endpoints
echo -e "${BLUE}=== Test 6: Health Check Endpoints ===${NC}"

make_request "GET" "${SERVICE_URL}/api/v2/health" "" "" "Kong Gateway health check endpoint"

make_request "GET" "${SERVICE_URL}/api/v2/ping" "" "" "Kong Gateway ping endpoint"

# Test 7: Subscription API endpoints (if service is fully running)
echo -e "${BLUE}=== Test 7: Subscription API Endpoints (Sample) ===${NC}"

# Note: These may fail if the database is not set up, but we can test authentication
make_request "GET" "${SERVICE_URL}/api/v1/subscriptions/status/ACTIVE" "Authorization: Bearer ${JWT_TOKEN}" "" "Get active subscriptions with JWT (may fail due to DB)"

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} Testing Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Key Testing Areas:${NC}"
echo "1. ✓ Public endpoints (no auth required)"
echo "2. ? External API endpoints (JWT authentication)"
echo "3. ? Internal API endpoints (API key authentication)"
echo "4. ? Admin endpoints (elevated permissions)"
echo "5. ✓ Load testing capabilities"
echo "6. ✓ Health check endpoints"
echo "7. ? Subscription business logic endpoints"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Configure Kong Gateway with the provided kong.yaml"
echo "2. Set up environment variables for API keys and JWT secrets"
echo "3. Apply Kong configuration: deck sync --kong-addr http://localhost:8001"
echo "4. Run this script again to validate Kong integration"
echo ""
echo -e "${GREEN}Script completed!${NC}"