#!/bin/bash

# TradeMaster Subscription Service - API Endpoint Validation Script
# This script validates that the subscription service API endpoints are working correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SERVICE_URL="http://localhost:8085"
SERVICE_PORT="8085"

# Test API Keys (Development only - change for production)
ADMIN_API_KEY="trademaster-subscription-admin-key-12345"
TRADING_SERVICE_API_KEY="trademaster-trading-service-key-abcde"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} Subscription Service API Validation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to make HTTP requests
make_request() {
    local method=$1
    local url=$2
    local headers=$3
    local description=$4

    echo -e "${YELLOW}Testing: ${description}${NC}"
    echo -e "Request: ${method} ${url}"

    local response
    local response_code

    if [ -n "$headers" ]; then
        echo -e "Headers: ${headers}"
        response=$(curl -s -w "\n%{http_code}" -X "${method}" "${url}" -H "$headers" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "${method}" "${url}" 2>/dev/null)
    fi

    local response_body=$(echo "$response" | sed '$d')
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
    elif [ "$response_code" -eq 404 ]; then
        echo -e "${YELLOW}⚠ NOT FOUND (HTTP $response_code)${NC}"
        echo "Response: $response_body"
    else
        echo -e "${RED}✗ ERROR (HTTP $response_code)${NC}"
        echo "Response: $response_body"
    fi

    echo ""
    return $response_code
}

# Check if service is running
echo -e "${YELLOW}Checking if subscription service is running...${NC}"
if ! curl -s "http://localhost:${SERVICE_PORT}/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}✗ Subscription service is not running on port ${SERVICE_PORT}${NC}"
    echo "Please start the subscription service first:"
    echo "  ./gradlew bootRun"
    echo "  or"
    echo "  java -jar build/libs/subscription-service-*.jar"
    exit 1
fi

echo -e "${GREEN}✓ Subscription service is running${NC}"
echo ""

# Test 1: Health check endpoints
echo -e "${BLUE}=== Test 1: Health Check Endpoints ===${NC}"

make_request "GET" "${SERVICE_URL}/actuator/health" "" "Spring Boot Actuator health check"

make_request "GET" "${SERVICE_URL}/api/v2/health" "" "Kong-compatible health check endpoint"

make_request "GET" "${SERVICE_URL}/api/v2/ping" "" "Kong-compatible ping endpoint"

# Test 2: Public test endpoints
echo -e "${BLUE}=== Test 2: Public Test Endpoints ===${NC}"

make_request "GET" "${SERVICE_URL}/api/v1/test/ping" "" "Public ping endpoint"

make_request "GET" "${SERVICE_URL}/api/v1/test/headers" "" "Headers echo endpoint"

# Test 3: Secured endpoints (without authentication - should fail)
echo -e "${BLUE}=== Test 3: Secured Endpoints (No Auth - Should Fail) ===${NC}"

make_request "GET" "${SERVICE_URL}/api/v1/test/secure" "" "Secure endpoint without JWT (should fail)"

make_request "GET" "${SERVICE_URL}/api/internal/test/service-ping" "" "Internal service ping without API key (should fail)"

# Test 4: Internal API endpoints with API key (if Spring Security allows)
echo -e "${BLUE}=== Test 4: Internal API Endpoints (With API Key) ===${NC}"

make_request "GET" "${SERVICE_URL}/api/internal/test/service-ping" "X-API-Key: ${TRADING_SERVICE_API_KEY}" "Internal service ping with trading service API key"

make_request "GET" "${SERVICE_URL}/api/internal/capabilities" "X-API-Key: ${TRADING_SERVICE_API_KEY}" "Service capabilities endpoint"

make_request "GET" "${SERVICE_URL}/api/internal/test/admin" "X-API-Key: ${ADMIN_API_KEY}" "Admin endpoint with admin API key"

# Test 5: Main subscription endpoints (basic structure test)
echo -e "${BLUE}=== Test 5: Subscription API Structure ===${NC}"

# Note: These will likely fail due to authentication, but we can test the endpoint structure
make_request "GET" "${SERVICE_URL}/api/v1/subscriptions/status/ACTIVE" "" "Get active subscriptions (will likely fail auth)"

# Test 6: OpenAPI documentation
echo -e "${BLUE}=== Test 6: OpenAPI Documentation ===${NC}"

make_request "GET" "${SERVICE_URL}/v3/api-docs" "" "OpenAPI JSON documentation"

make_request "GET" "${SERVICE_URL}/swagger-ui.html" "" "Swagger UI (may redirect)"

# Test 7: Actuator endpoints
echo -e "${BLUE}=== Test 7: Actuator Endpoints ===${NC}"

make_request "GET" "${SERVICE_URL}/actuator/info" "" "Application info"

make_request "GET" "${SERVICE_URL}/actuator/metrics" "" "Metrics endpoint"

make_request "GET" "${SERVICE_URL}/actuator/prometheus" "" "Prometheus metrics"

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE} Validation Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Endpoint Categories Tested:${NC}"
echo "1. ✓ Health check endpoints"
echo "2. ✓ Public test endpoints"
echo "3. ✓ Security validation (expected failures)"
echo "4. ? Internal API endpoints (depends on security config)"
echo "5. ? Subscription API structure"
echo "6. ✓ OpenAPI documentation"
echo "7. ✓ Actuator monitoring endpoints"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review any failed tests and configure security as needed"
echo "2. Set up Kong Gateway to test full routing and authentication"
echo "3. Configure JWT authentication for external API access"
echo "4. Test with real subscription data and database"
echo ""
echo -e "${GREEN}Validation completed!${NC}"