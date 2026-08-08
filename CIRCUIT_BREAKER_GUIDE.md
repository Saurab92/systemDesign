# Circuit Breaker Implementation Guide

## Overview

This implementation follows the Circuit Breaker pattern to prevent cascading failures in distributed systems. The circuit breaker monitors service health and automatically blocks requests when failure thresholds are exceeded.

## Architecture

### Three States

1. **CLOSED** (Normal Operation)
   - All requests pass through
   - Monitors failure rate
   - Opens circuit when failure threshold is exceeded

2. **OPEN** (Failing - Fast Fail)
   - Blocks all requests immediately
   - Returns fallback responses
   - Prevents overwhelming failing service
   - Automatically transitions to HALF_OPEN after timeout

3. **HALF_OPEN** (Recovery Testing)
   - Allows limited test requests
   - Closes circuit if success threshold met
   - Reopens immediately on any failure

## Key Components

### 1. CircuitBreaker.java
Core implementation with thread-safe state management using atomic operations.

**Features:**
- Atomic state transitions using `AtomicReference`
- Configurable failure/success thresholds
- Automatic timeout and recovery
- Fallback support
- Comprehensive metrics tracking

### 2. CircuitBreakerMetrics.java
Thread-safe metrics collection:
- Success/failure/rejection counts
- Failure rate calculation
- Timestamp tracking
- State transition history

### 3. CircuitBreakerState.java
Enum defining three states: CLOSED, OPEN, HALF_OPEN

### 4. CircuitBreakerException.java
Custom exception thrown when circuit is open

## Configuration

### application.properties

```properties
# Circuit Breaker Configuration
circuit-breaker.failure-threshold=50        # Failure rate % to open circuit
circuit-breaker.success-threshold=2         # Consecutive successes to close circuit
circuit-breaker.timeout=60000              # Milliseconds before trying HALF_OPEN
circuit-breaker.minimum-number-of-calls=5  # Minimum calls before evaluating failure rate
```

### Configuration Parameters Explained

- **failure-threshold** (default: 50%): Percentage of failed calls that triggers circuit opening
- **success-threshold** (default: 2): Number of consecutive successful calls in HALF_OPEN state required to close circuit
- **timeout** (default: 60000ms): Time to wait in OPEN state before attempting recovery
- **minimum-number-of-calls** (default: 5): Minimum number of calls required before calculating failure rate

## Usage Example

### In Service Layer

```java
@Service
public class ProductService {
    private final CircuitBreaker circuitBreaker;
    
    public List<Product> getAllProducts() {
        try {
            return circuitBreaker.execute(() -> {
                // Primary operation
                return externalApiCall();
            }, () -> {
                // Fallback when circuit is open
                return getCachedProducts();
            });
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
            return List.of();
        }
    }
}
```

## Monitoring Endpoints

### 1. Check Circuit Breaker Status
```bash
GET /api/circuit-breaker/status
```

Response:
```json
{
  "name": "product-service",
  "state": "CLOSED",
  "metrics": {
    "successCount": 45,
    "failureCount": 2,
    "rejectedCount": 0,
    "totalCount": 47,
    "failureRate": "4.26%"
  }
}
```

### 2. Health Check
```bash
GET /api/circuit-breaker/health
```

Response (when healthy):
```json
{
  "status": "UP",
  "circuitBreakerState": "CLOSED",
  "details": {
    "successCount": 45,
    "failureCount": 2,
    "rejectedCount": 0,
    "totalCount": 47,
    "failureRate": "4.26%"
  }
}
```

### 3. Manual Reset
```bash
POST /api/circuit-breaker/reset
```

Response:
```json
{
  "message": "Circuit breaker reset successfully",
  "state": "CLOSED"
}
```

## Best Practices Implemented

### 1. Thread Safety
- All state management uses atomic operations
- No synchronized blocks for better performance
- Lock-free concurrent access

### 2. Fail Fast
- Immediate rejection when circuit is OPEN
- Prevents resource exhaustion
- Reduces latency during outages

### 3. Automatic Recovery
- Self-healing with HALF_OPEN state
- Gradual service restoration
- Configurable recovery criteria

### 4. Fallback Strategies
- Optional fallback function support
- Graceful degradation
- Maintains service availability

### 5. Comprehensive Monitoring
- Real-time metrics
- State transition tracking
- Detailed logging at all levels

### 6. Configurable Thresholds
- Environment-specific tuning
- No hardcoded values
- Externalized configuration

### 7. Minimum Call Window
- Prevents premature circuit opening
- Statistical significance requirement
- Reduces false positives

## Integration with Retry Pattern

The circuit breaker wraps the retry template for layered resilience:

```
Request → Circuit Breaker → Retry Template → External Service
          ↓ (if open)
          Fallback
```

**Benefits:**
- Circuit breaker provides fast fail when service is down
- Retry handles transient failures
- Prevents unnecessary retry attempts when circuit is open
- Reduces overall latency during outages

## State Transition Flow

```
CLOSED ──[failure rate > threshold]──> OPEN
  ↑                                      ↓
  │                            [timeout elapsed]
  │                                      ↓
  └──[success count >= threshold]── HALF_OPEN
                                         ↓
                                [any failure]
                                         ↓
                                       OPEN
```

## Testing Strategy

### 1. Unit Tests
- Test state transitions
- Verify threshold calculations
- Check thread safety
- Validate metrics accuracy

### 2. Integration Tests
- Test with actual service calls
- Verify fallback execution
- Check timeout behavior
- Validate recovery process

### 3. Load Tests
- Concurrent request handling
- State consistency under load
- Performance impact measurement

## Common Scenarios

### Scenario 1: Service Degradation
1. External service starts returning errors
2. After minimum calls, failure rate exceeds threshold
3. Circuit opens → Fast fail with fallback
4. Service recovers during timeout
5. Circuit moves to HALF_OPEN
6. Test calls succeed
7. Circuit closes → Normal operation

### Scenario 2: Temporary Network Issue
1. Network blip causes few failures
2. Failure count below minimum threshold
3. Circuit remains CLOSED
4. Retry template handles recovery
5. Service continues normally

### Scenario 3: Complete Service Outage
1. Service completely down
2. Circuit opens quickly
3. All requests fail fast with fallback
4. No resource waste on retries
5. Periodic recovery attempts via HALF_OPEN
6. Automatic recovery when service returns

## Performance Considerations

- **Low Overhead**: Atomic operations are lightweight
- **No Locks**: Lock-free design prevents contention
- **Fast Fail**: Sub-millisecond rejection in OPEN state
- **Memory Efficient**: Minimal state maintenance

## Troubleshooting

### Circuit Opens Too Frequently
- Increase `failure-threshold` percentage
- Increase `minimum-number-of-calls`
- Check if service has genuine issues

### Circuit Doesn't Open When It Should
- Decrease `failure-threshold` percentage
- Decrease `minimum-number-of-calls`
- Verify error handling propagates exceptions

### Slow Recovery
- Decrease `timeout` value
- Decrease `success-threshold` count

### False Positives
- Increase `minimum-number-of-calls`
- Review failure detection logic
- Check for transient vs permanent failures

## Metrics and Alerts

### Recommended Alerts

1. **Circuit Open Alert**
   - Trigger: Circuit state = OPEN for > 5 minutes
   - Action: Investigate downstream service

2. **High Failure Rate**
   - Trigger: Failure rate > 25% for > 2 minutes
   - Action: Check service health

3. **High Rejection Rate**
   - Trigger: Rejected calls > 100/minute
   - Action: Service is degraded

## Extensions

### Future Enhancements
- Sliding window for failure rate calculation
- Per-endpoint circuit breakers
- Distributed circuit breaker state
- Adaptive timeout based on historical data
- Integration with service mesh
- Custom health check predicates

## References

- Martin Fowler's Circuit Breaker Pattern
- Release It! by Michael Nygard
- Spring Cloud Circuit Breaker
- Resilience4j Documentation
