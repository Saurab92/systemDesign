# When Circuit Breaker Endpoints Are Called

## Circuit Breaker Logic Invocation (Automatic)

### ProductService Methods - Called on EVERY Request

The circuit breaker wraps your business logic and is **automatically invoked** whenever you call the Product APIs:

#### 1. Get All Products
```bash
curl http://localhost:8082/api/products
```

**Flow:**
```
User Request
    ↓
ProductController.getAllProducts()
    ↓
ProductService.getAllProducts()
    ↓
circuitBreaker.execute(() -> {     ← CIRCUIT BREAKER CALLED HERE
    retryTemplate.execute(() -> {
        restClient.get("/products")
    })
}, () -> {
    return List.of();  ← Fallback if circuit is OPEN
})
```

#### 2. Get Product By ID
```bash
curl http://localhost:8082/api/products/1
```

**Flow:**
```
User Request
    ↓
ProductController.getProductById(1)
    ↓
ProductService.getProductById(1)
    ↓
circuitBreaker.execute(() -> {     ← CIRCUIT BREAKER CALLED HERE
    retryTemplate.execute(() -> {
        restClient.get("/products/1")
    })
}, () -> {
    return null;  ← Fallback if circuit is OPEN
})
```

### What Happens Inside Circuit Breaker?

**Every time you call the Product API:**

1. **Circuit State Check** (happens automatically)
   - Is circuit CLOSED? → Allow request
   - Is circuit OPEN? → Check timeout
     - If timeout elapsed → Move to HALF_OPEN → Allow limited requests
     - If timeout NOT elapsed → REJECT → Execute fallback
   - Is circuit HALF_OPEN? → Allow request (testing recovery)

2. **Execute Request**
   - Try to call external API
   - Record success or failure in metrics

3. **Update State** (if needed)
   - If too many failures → Open circuit
   - If enough successes in HALF_OPEN → Close circuit
   - If failure in HALF_OPEN → Reopen circuit

---

## Monitoring Endpoints (Manual/External)

These are **NOT called automatically**. You or monitoring systems call them:

### 1. Status Endpoint - GET /api/circuit-breaker/status

**When to call:**
- ✅ Manual debugging to see current state
- ✅ Monitoring dashboard (Grafana, Prometheus)
- ✅ Health check scripts
- ✅ Troubleshooting issues
- ✅ Operations team checking system health

**Example:**
```bash
# DevOps/SRE team manually checking
curl http://localhost:8082/api/circuit-breaker/status
```

**Response:**
```json
{
  "name": "product-service",
  "state": "OPEN",
  "metrics": {
    "successCount": 10,
    "failureCount": 15,
    "rejectedCount": 5,
    "totalCount": 25,
    "failureRate": "60.00%"
  }
}
```

### 2. Health Endpoint - GET /api/circuit-breaker/health

**When to call:**
- ✅ Load balancer health checks
- ✅ Kubernetes liveness/readiness probes
- ✅ Monitoring systems (Nagios, Datadog)
- ✅ Service mesh health checks
- ✅ Automated alerting systems

**Example:**
```bash
# Kubernetes readiness probe
curl http://localhost:8082/api/circuit-breaker/health
```

**Response (when healthy):**
```json
{
  "status": "UP",
  "circuitBreakerState": "CLOSED",
  "details": { ... }
}
```

**Response (when unhealthy - 503):**
```json
{
  "status": "DOWN",
  "circuitBreakerState": "OPEN",
  "details": { ... }
}
```

### 3. Reset Endpoint - POST /api/circuit-breaker/reset

**When to call:**
- ✅ After fixing underlying service issue
- ✅ Manual intervention by ops team
- ✅ Testing/debugging scenarios
- ✅ Emergency recovery procedures
- ✅ After deploying a fix

**Example:**
```bash
# Operations team manually resetting after service recovery
curl -X POST http://localhost:8082/api/circuit-breaker/reset
```

---

## Real-World Scenarios

### Scenario 1: Normal Operation (Happy Path)

**Timeline:**
```
09:00 AM - User calls: GET /api/products
           ↓ Circuit Breaker: CLOSED → Allow request
           ↓ Retry Template: Try 3 times if needed
           ↓ External API: Returns products
           ✅ Success recorded in metrics

09:01 AM - Another user calls: GET /api/products
           ↓ Circuit Breaker: CLOSED → Allow request
           ✅ Success (circuit stays CLOSED)

09:02 AM - DevOps checks: GET /api/circuit-breaker/status
           Response: state=CLOSED, failureRate=0%
```

### Scenario 2: Service Degradation → Circuit Opens

**Timeline:**
```
10:00 AM - External API starts failing
           User calls: GET /api/products
           ↓ Circuit Breaker: CLOSED → Allow request
           ↓ Retry Template: Tries 3 times
           ❌ All fail → Failure recorded

10:01 AM - More requests come in
           ↓ Circuit Breaker: CLOSED (still)
           ↓ Failures accumulate: 5 failures out of 6 calls
           ↓ Failure rate: 83% > 50% threshold
           🔴 Circuit OPENS automatically

10:02 AM - New user request: GET /api/products
           ↓ Circuit Breaker: OPEN → REJECT immediately
           ↓ Fallback executed → Returns empty list []
           ⚡ Response in milliseconds (no retry attempts)

10:03 AM - Monitoring alert triggered
           DevOps calls: GET /api/circuit-breaker/health
           Response: status=DOWN, state=OPEN (503 status)

10:05 AM - Load balancer health check fails
           Traffic redirected to healthy instances
```

### Scenario 3: Automatic Recovery

**Timeline:**
```
10:00 AM - Circuit is OPEN
           All requests rejected with fallback

11:00 AM - Timeout elapsed (60 seconds)
           Next user request: GET /api/products
           ↓ Circuit Breaker: OPEN → Check timeout → Elapsed!
           ↓ Transition to HALF_OPEN
           🟡 Circuit allows LIMITED test request

11:00:10 AM - Request succeeds
              ✅ Success 1/2 (need 2 for threshold)

11:00:15 AM - Another request succeeds
              ✅ Success 2/2
              🟢 Circuit CLOSES automatically

11:01 AM - DevOps checks: GET /api/circuit-breaker/status
           Response: state=CLOSED, metrics reset
```

### Scenario 4: Manual Intervention

**Timeline:**
```
02:00 PM - Circuit is OPEN due to downstream service issue
           DevOps team fixes the external service

02:05 PM - DevOps manually resets circuit
           POST /api/circuit-breaker/reset
           Response: state=CLOSED

02:06 PM - Normal traffic resumes
           All requests flow through normally
```

---

## Integration Points

### 1. Application Monitoring (Automatic)

**Prometheus Scraping** (if configured):
```yaml
# Prometheus config
scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8082']
    scrape_interval: 15s  # Calls metrics every 15 seconds
```

### 2. Load Balancer Health Checks (Automatic)

**Nginx Config:**
```nginx
upstream backend {
    server localhost:8082 max_fails=3 fail_timeout=30s;
    
    # Health check calls /health every 10 seconds
    health_check interval=10s uri=/api/circuit-breaker/health;
}
```

### 3. Kubernetes Probes (Automatic)

**K8s Deployment:**
```yaml
livenessProbe:
  httpGet:
    path: /api/circuit-breaker/health
    port: 8082
  initialDelaySeconds: 30
  periodSeconds: 10  # Calls every 10 seconds

readinessProbe:
  httpGet:
    path: /api/circuit-breaker/health
    port: 8082
  initialDelaySeconds: 10
  periodSeconds: 5  # Calls every 5 seconds
```

### 4. Grafana Dashboard (Polling)

```javascript
// Dashboard queries circuit breaker status every minute
setInterval(() => {
    fetch('http://localhost:8082/api/circuit-breaker/status')
        .then(response => response.json())
        .then(data => updateDashboard(data));
}, 60000);  // Every 60 seconds
```

---

## Summary: When Things Get Called

| Component | When Called | Who Calls It | Frequency |
|-----------|-------------|--------------|-----------|
| **Circuit Breaker Logic** | Every product API call | Automatic (ProductService) | Every request |
| **GET /status** | Manual or monitoring | DevOps/Monitoring systems | Ad-hoc or every 30-60s |
| **GET /health** | Health checks | Load balancers/K8s | Every 5-30s |
| **POST /reset** | Manual intervention | DevOps team | Rarely, as needed |
| **State Transitions** | Based on metrics/timeout | Automatic (internal) | Real-time |

---

## Key Takeaways

1. **Circuit Breaker Logic is Automatic**
   - Called on every Product API request
   - No manual intervention needed
   - Works transparently

2. **Monitoring Endpoints are External**
   - Called by humans or monitoring tools
   - Not part of normal request flow
   - Used for observability

3. **State Transitions are Automatic**
   - CLOSED → OPEN: When failure threshold exceeded
   - OPEN → HALF_OPEN: After timeout elapsed
   - HALF_OPEN → CLOSED: After successful recovery
   - No manual triggers needed

4. **You Don't Need to Do Anything Special**
   - Just call your normal APIs: `GET /api/products`
   - Circuit breaker works behind the scenes
   - Check status endpoints when needed

---

## Testing It Yourself

### See Circuit Breaker in Action:

```bash
# 1. Start application
./mvnw spring-boot:run

# 2. Make normal requests (circuit is CLOSED)
for i in {1..5}; do
    curl http://localhost:8082/api/products
    echo "Request $i completed"
done

# 3. Check status
curl http://localhost:8082/api/circuit-breaker/status
# Should show: state=CLOSED, low failure rate

# 4. Simulate failures (stop external API or misconfigure URL)
# Make multiple requests that fail

# 5. Watch circuit open automatically
curl http://localhost:8082/api/circuit-breaker/status
# Should show: state=OPEN, high failure rate

# 6. See fast-fail behavior (returns instantly with fallback)
time curl http://localhost:8082/api/products
# Returns in milliseconds with empty list

# 7. Wait for timeout (60 seconds)
sleep 60

# 8. Make another request (triggers HALF_OPEN)
curl http://localhost:8082/api/products

# 9. Check status
curl http://localhost:8082/api/circuit-breaker/status
# Should show: state=HALF_OPEN

# 10. If service is healthy, circuit closes automatically
```

Hope this clarifies when and how everything gets called! 🎯
