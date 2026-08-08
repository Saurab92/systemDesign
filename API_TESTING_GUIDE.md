# API Testing Guide

## Application Endpoints

### Product Endpoints (with Circuit Breaker)

#### Get All Products
```bash
curl http://localhost:8082/api/products
```

#### Get Product By ID
```bash
curl http://localhost:8082/api/products/1
```

### Circuit Breaker Management

#### Get Circuit Breaker Status
```bash
curl http://localhost:8082/api/circuit-breaker/status
```

**Expected Response:**
```json
{
  "name": "product-service",
  "state": "CLOSED",
  "metrics": {
    "successCount": 10,
    "failureCount": 0,
    "rejectedCount": 0,
    "totalCount": 10,
    "failureRate": "0.00%"
  }
}
```

#### Get Circuit Breaker Health
```bash
curl http://localhost:8082/api/circuit-breaker/health
```

**Expected Response (Healthy):**
```json
{
  "status": "UP",
  "circuitBreakerState": "CLOSED",
  "details": {
    "successCount": 10,
    "failureCount": 0,
    "rejectedCount": 0,
    "totalCount": 10,
    "failureRate": "0.00%"
  }
}
```

**Expected Response (Unhealthy):**
```json
{
  "status": "DOWN",
  "circuitBreakerState": "OPEN",
  "details": {
    "successCount": 5,
    "failureCount": 10,
    "rejectedCount": 3,
    "totalCount": 15,
    "failureRate": "66.67%"
  }
}
```

#### Reset Circuit Breaker
```bash
curl -X POST http://localhost:8082/api/circuit-breaker/reset
```

**Expected Response:**
```json
{
  "message": "Circuit breaker reset successfully",
  "state": "CLOSED"
}
```

## Testing Circuit Breaker Behavior

### Scenario 1: Normal Operation (CLOSED)
1. Start application: `./mvnw spring-boot:run`
2. Make successful requests:
   ```bash
   for i in {1..10}; do
     curl http://localhost:8082/api/products
     sleep 1
   done
   ```
3. Check status:
   ```bash
   curl http://localhost:8082/api/circuit-breaker/status
   ```
   - State should be: `CLOSED`
   - Failure rate should be: `0.00%`

### Scenario 2: Service Failure (OPEN)
1. Simulate service failure by using invalid external API URL
2. Make multiple requests until circuit opens (need 5+ failures with 50%+ rate)
3. Check status:
   ```bash
   curl http://localhost:8082/api/circuit-breaker/status
   ```
   - State should be: `OPEN`
   - Rejection count should increase

### Scenario 3: Recovery (HALF_OPEN → CLOSED)
1. Wait for timeout (60 seconds default)
2. Make a successful request - circuit moves to HALF_OPEN
3. Make another successful request - circuit closes
4. Check status to confirm CLOSED state

### Scenario 4: Manual Reset
```bash
# Force reset regardless of state
curl -X POST http://localhost:8082/api/circuit-breaker/reset

# Verify reset
curl http://localhost:8082/api/circuit-breaker/status
```

## Monitoring in Real-Time

### Watch Circuit Breaker Status
```bash
# Linux/Mac
watch -n 2 'curl -s http://localhost:8082/api/circuit-breaker/status | jq'

# Alternative (without jq)
while true; do
  clear
  curl -s http://localhost:8082/api/circuit-breaker/status
  sleep 2
done
```

### Monitor Application Logs
```bash
./mvnw spring-boot:run | grep -i "circuit"
```

### Check for State Transitions
Look for these log messages:
- `Circuit breaker 'product-service' initialized`
- `Circuit breaker 'product-service' transitioned to OPEN`
- `Circuit breaker 'product-service' transitioned to HALF_OPEN`
- `Circuit breaker 'product-service' transitioned to CLOSED`

## Load Testing

### Basic Load Test
```bash
# Send 100 requests
for i in {1..100}; do
  curl -s http://localhost:8082/api/products > /dev/null &
done
wait

# Check metrics
curl http://localhost:8082/api/circuit-breaker/status
```

### Concurrent Load Test
```bash
# Using Apache Bench (if installed)
ab -n 100 -c 10 http://localhost:8082/api/products

# Using curl with parallel execution
seq 1 50 | xargs -n1 -P10 -I{} curl -s http://localhost:8082/api/products
```

## Testing Different States

### Force OPEN State
```bash
# 1. Stop external service or use wrong URL in config
# 2. Make 5-10 requests
for i in {1..10}; do curl http://localhost:8082/api/products; done

# 3. Verify OPEN state
curl http://localhost:8082/api/circuit-breaker/status
```

### Test HALF_OPEN Transition
```bash
# 1. Circuit must be OPEN first
# 2. Wait for timeout (60 seconds default)
sleep 60

# 3. Make one request - triggers HALF_OPEN
curl http://localhost:8082/api/products

# 4. Check state
curl http://localhost:8082/api/circuit-breaker/status
# Should show: "state": "HALF_OPEN"
```

### Test Fallback Behavior
```bash
# When circuit is OPEN, fallback returns empty list
curl http://localhost:8082/api/products
# Response: []

# Check logs for fallback message
# "Circuit breaker fallback: returning empty product list"
```

## Verification Checklist

- [ ] Application starts successfully
- [ ] Products endpoint returns data when service is healthy
- [ ] Circuit breaker status endpoint works
- [ ] Circuit breaker health endpoint works
- [ ] Manual reset endpoint works
- [ ] Circuit opens after failures exceed threshold
- [ ] Circuit rejects requests when OPEN
- [ ] Fallback executes when circuit is OPEN
- [ ] Circuit transitions to HALF_OPEN after timeout
- [ ] Circuit closes after successful recovery
- [ ] Metrics are accurately tracked
- [ ] Logs show state transitions
- [ ] All tests pass: `./mvnw test -Dtest=CircuitBreakerTest`

## Common Response Codes

- **200 OK**: Successful request
- **503 Service Unavailable**: Circuit breaker is OPEN or service unavailable
- **500 Internal Server Error**: Unexpected error

## Expected Behavior Summary

| State | Request Behavior | Metrics Update | Next State Trigger |
|-------|-----------------|----------------|-------------------|
| CLOSED | Passes through | Yes | Failure rate > threshold |
| OPEN | Rejected/Fallback | Rejection count++ | Timeout elapsed |
| HALF_OPEN | Limited pass-through | Yes | Success threshold OR failure |

## Tips

1. **Monitor logs** for detailed circuit breaker activity
2. **Use jq** for pretty JSON formatting: `curl ... | jq`
3. **Adjust thresholds** in application.properties for testing
4. **Reset circuit** manually between test scenarios
5. **Check metrics** after each test to understand behavior
