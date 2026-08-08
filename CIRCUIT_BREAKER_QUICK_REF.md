# Circuit Breaker Quick Reference

## Configuration (application.properties)
```properties
circuit-breaker.failure-threshold=50        # % failure rate to open circuit
circuit-breaker.success-threshold=2         # consecutive successes to close
circuit-breaker.timeout=60000              # ms before HALF_OPEN attempt
circuit-breaker.minimum-number-of-calls=5  # min calls before evaluation
```

## States
- **CLOSED**: Normal operation → Opens at failure threshold
- **OPEN**: Fast fail mode → Transitions to HALF_OPEN after timeout
- **HALF_OPEN**: Testing recovery → Closes on success threshold OR reopens on failure

## Monitoring Endpoints

### Check Status
```bash
curl http://localhost:8082/api/circuit-breaker/status
```

### Health Check
```bash
curl http://localhost:8082/api/circuit-breaker/health
```

### Manual Reset
```bash
curl -X POST http://localhost:8082/api/circuit-breaker/reset
```

## Usage in Code

### With Fallback
```java
circuitBreaker.execute(
    () -> externalService.call(),  // Primary
    () -> getCachedData()           // Fallback
);
```

### Without Fallback (throws exception)
```java
circuitBreaker.execute(() -> externalService.call());
```

## Metrics Available
- `successCount` - Total successful calls
- `failureCount` - Total failed calls
- `rejectedCount` - Calls rejected when circuit OPEN
- `failureRate` - Percentage of failed calls
- `totalCount` - Total calls (success + failure)

## Key Features
✅ Thread-safe (atomic operations)
✅ Lock-free concurrent access
✅ Automatic recovery
✅ Configurable thresholds
✅ Fallback support
✅ Comprehensive monitoring
✅ Integration with retry mechanism

## Test Results
✅ 13/13 tests passing
✅ Thread safety verified
✅ State transitions validated
✅ Metrics accuracy confirmed

## Build & Run
```bash
# Compile
./mvnw clean compile

# Run tests
./mvnw test

# Build package
./mvnw clean package

# Run application
./mvnw spring-boot:run
```

## Files Created
1. CircuitBreaker.java - Core implementation
2. CircuitBreakerState.java - State enum
3. CircuitBreakerException.java - Custom exception
4. CircuitBreakerMetrics.java - Metrics tracking
5. CircuitBreakerProperties.java - Configuration
6. CircuitBreakerConfig.java - Bean config
7. CircuitBreakerController.java - Monitoring endpoints
8. CircuitBreakerTest.java - Unit tests
9. CIRCUIT_BREAKER_GUIDE.md - Detailed documentation
10. CIRCUIT_BREAKER_SUMMARY.md - Implementation summary

## Troubleshooting

**Circuit opens too often?**
- ↑ Increase `failure-threshold`
- ↑ Increase `minimum-number-of-calls`

**Circuit doesn't open when it should?**
- ↓ Decrease `failure-threshold`
- ↓ Decrease `minimum-number-of-calls`

**Slow recovery?**
- ↓ Decrease `timeout`
- ↓ Decrease `success-threshold`

**Frequent state changes (flapping)?**
- ↑ Increase `minimum-number-of-calls`
- ↑ Increase `timeout`
- Review underlying service issues
