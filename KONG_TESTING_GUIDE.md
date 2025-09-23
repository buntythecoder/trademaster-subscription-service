# TradeMaster Subscription Service - Kong Gateway Testing Guide

This guide walks you through testing the Kong Gateway integration with the subscription service, including API key authentication and route validation.

## 🚀 Quick Start

### Prerequisites
- Docker and Docker Compose installed
- `curl` command available
- `jq` for JSON formatting (optional but recommended)

### 1. Start the Services

```bash
# Option A: Start with PostgreSQL backend (full Kong features)
docker-compose -f docker-compose.kong-test.yml up -d kong-database kong-bootstrap kong subscription-service

# Option B: Start with DB-less mode (simpler setup)
docker-compose -f docker-compose.kong-test.yml --profile dbless up -d kong-dbless subscription-service
```

### 2. Wait for Services to be Ready

```bash
# Check Kong health
curl -f http://localhost:8001/status

# Check subscription service health
curl -f http://localhost:8085/api/v2/health
```

### 3. Apply Kong Configuration

```bash
# If using DB mode, apply configuration via Admin API
# Note: This requires deck CLI tool or manual API calls

# If using DB-less mode, configuration is already loaded from kong.yaml
```

### 4. Run API Key Tests

```bash
# Make the test script executable
chmod +x test-kong-api-keys.sh

# Run the comprehensive test suite
./test-kong-api-keys.sh
```

## 📋 Test Endpoints Overview

### Public Endpoints (No Authentication)
- `GET /api/v1/test/ping` - Basic ping endpoint
- `GET /api/v1/test/headers` - Echo request headers for debugging
- `GET /api/v2/health` - Health check endpoint
- `GET /api/v2/ping` - Kong-compatible ping endpoint

### External API Endpoints (JWT Authentication Required)
- `GET /api/v1/test/secure` - Secure test endpoint requiring JWT
- `POST /api/v1/test/load` - Load testing endpoint with payload
- `GET /api/v1/subscriptions/*` - Main subscription API endpoints

### Internal API Endpoints (API Key Authentication Required)
- `GET /api/internal/test/service-ping` - Internal service ping
- `GET /api/internal/capabilities` - Service capabilities information
- `GET /api/internal/test/admin` - Admin-only endpoint (requires ADMIN role)

## 🔑 API Key Configuration

### Environment Variables
Copy `.env.kong` to `.env` and update with your actual secrets:

```bash
cp .env.kong .env
# Edit .env file with your actual API keys and secrets
```

### Test API Keys (Development Only)
- **Admin API Key**: `trademaster-subscription-admin-key-12345`
- **Monitor API Key**: `trademaster-subscription-monitor-key-67890`
- **Trading Service API Key**: `trademaster-trading-service-key-abcde`
- **Notification Service API Key**: `trademaster-notification-service-key-fghij`
- **Billing Service API Key**: `trademaster-billing-service-key-klmno`

### Test JWT Token (Development Only)
```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRyYWRlTWFzdGVyIFVzZXIiLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6OTk5OTk5OTk5OX0.dummy
```

## 🧪 Manual Testing Commands

### Test Public Endpoints
```bash
# Basic ping (should always work)
curl -X GET http://localhost:8000/api/v1/test/ping

# Headers echo (useful for debugging Kong headers)
curl -X GET http://localhost:8000/api/v1/test/headers
```

### Test JWT Authentication
```bash
# Without JWT (should return 401)
curl -X GET http://localhost:8000/api/v1/test/secure

# With JWT (should return 200)
curl -X GET http://localhost:8000/api/v1/test/secure \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlRyYWRlTWFzdGVyIFVzZXIiLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6OTk5OTk5OTk5OX0.dummy"
```

### Test API Key Authentication
```bash
# Without API key (should return 401)
curl -X GET http://localhost:8000/api/internal/test/service-ping

# With API key (should return 200)
curl -X GET http://localhost:8000/api/internal/test/service-ping \
  -H "X-API-Key: trademaster-trading-service-key-abcde"

# Service capabilities
curl -X GET http://localhost:8000/api/internal/capabilities \
  -H "X-API-Key: trademaster-trading-service-key-abcde"
```

### Test Admin Endpoints
```bash
# With admin API key (should return 200)
curl -X GET http://localhost:8000/api/internal/test/admin \
  -H "X-API-Key: trademaster-subscription-admin-key-12345"

# With non-admin API key (should return 403)
curl -X GET http://localhost:8000/api/internal/test/admin \
  -H "X-API-Key: trademaster-trading-service-key-abcde"
```

## 📊 Kong Admin API Commands

### View Kong Status
```bash
curl http://localhost:8001/status
```

### List Services
```bash
curl http://localhost:8001/services
```

### List Routes
```bash
curl http://localhost:8001/routes
```

### List Consumers
```bash
curl http://localhost:8001/consumers
```

### View Service Health
```bash
curl http://localhost:8001/upstreams/subscription-service-upstream/health
```

## 🔍 Troubleshooting

### Common Issues

1. **Service Not Ready**
   ```bash
   # Check service logs
   docker-compose -f docker-compose.kong-test.yml logs subscription-service

   # Check Kong logs
   docker-compose -f docker-compose.kong-test.yml logs kong
   ```

2. **Database Connection Issues**
   ```bash
   # Check PostgreSQL is running
   docker-compose -f docker-compose.kong-test.yml ps kong-database

   # Check database logs
   docker-compose -f docker-compose.kong-test.yml logs kong-database
   ```

3. **Kong Configuration Issues**
   ```bash
   # Validate kong.yaml configuration
   deck validate --kong-addr http://localhost:8001

   # Check Kong configuration
   curl http://localhost:8001/config
   ```

4. **API Key Authentication Failures**
   - Verify API keys are correctly set in environment variables
   - Check Kong consumer configuration
   - Ensure ServiceApiKeyFilter is properly configured in Spring Security

### Debug Headers
The `/api/v1/test/headers` endpoint will show you all request headers, which is useful for debugging Kong's header injection:

```bash
curl http://localhost:8000/api/v1/test/headers \
  -H "X-API-Key: trademaster-trading-service-key-abcde" | jq
```

Look for Kong-specific headers like:
- `X-Consumer-ID`
- `X-Consumer-Username`
- `X-Correlation-ID`

## 🚦 Expected Results

### Successful API Key Authentication
```json
{
  "status": "service_authenticated",
  "message": "API key authentication successful",
  "service": "subscription-service",
  "timestamp": "2025-01-15T10:30:00",
  "correlationId": "uuid-here",
  "kong": {
    "consumer": "trademaster-trading-service",
    "consumerId": "trading-service-001"
  },
  "serviceInfo": {
    "authenticated": true,
    "authorities": "ROLE_SERVICE"
  }
}
```

### Service Capabilities Response
```json
{
  "service": "subscription-service",
  "version": "1.0.0",
  "capabilities": {
    "subscription_management": true,
    "billing_processing": true,
    "notification_support": true,
    "upgrade_downgrade": true,
    "trial_management": true,
    "subscription_history": true
  },
  "endpoints": {
    "health_check": "/api/v2/health",
    "subscription_create": "/api/v1/subscriptions",
    "subscription_get": "/api/v1/subscriptions/{id}"
  },
  "sla": {
    "target_response_time_ms": 100,
    "availability": "99.9%",
    "max_concurrent_users": 10000
  }
}
```

## 📈 Performance Testing

### Load Testing with curl
```bash
# Simple load test
for i in {1..100}; do
  curl -s -X POST http://localhost:8000/api/v1/test/load \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"test": true, "iteration": '$i'}' > /dev/null &
done
wait
echo "Load test completed"
```

### With wrk (if available)
```bash
# Install wrk first: sudo apt-get install wrk (Ubuntu) or brew install wrk (macOS)
wrk -t4 -c100 -d30s -H "X-API-Key: trademaster-trading-service-key-abcde" \
  http://localhost:8000/api/internal/test/service-ping
```

## 🔧 Production Configuration

For production deployment:

1. **Replace Test API Keys**: Generate secure, random API keys
2. **Use Real JWT Secrets**: Configure with production JWT signing keys
3. **Enable HTTPS**: Configure SSL certificates in Kong
4. **Set Up Monitoring**: Enable Prometheus metrics and Grafana dashboards
5. **Configure Rate Limits**: Adjust rate limiting based on your requirements
6. **Enable Logging**: Configure centralized logging with your log aggregation system

## 🎯 Next Steps

1. **Integration Testing**: Run the full test suite with `./test-kong-api-keys.sh`
2. **Performance Validation**: Use load testing tools to validate performance under load
3. **Security Testing**: Validate API key rotation, JWT expiration, and access controls
4. **Monitoring Setup**: Configure Prometheus and Grafana for production monitoring
5. **Documentation**: Update API documentation with Kong routing information

---

**Note**: This is a development/testing setup. For production use, ensure all secrets are properly secured, HTTPS is enabled, and appropriate security measures are in place.