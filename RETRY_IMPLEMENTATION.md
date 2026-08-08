# Retry Mechanism Implementation

## Overview
Implemented automatic retry logic with exponential backoff for external API calls using Spring Retry framework with externalized configuration.

## What Was Implemented

### 1. Dependencies Added (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
    <version>2.0.4</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

### 2. Externalized Configuration (application.properties)
```properties
# Retry Configuration
api.retry.max-attempts=3
api.retry.initial-delay=1000
api.retry.multiplier=2.0
api.retry.max-delay=5000
```

**Benefits**:
- ✅ No hardcoded values in code
- ✅ Easy to adjust per environment
- ✅ No recompilation needed for changes
- ✅ Environment-specific overrides (dev/staging/prod)

### 3. Configuration Classes

#### RetryProperties.java
Binds retry configuration from application.properties using `@ConfigurationProperties`:
```java
@Component
@ConfigurationProperties(prefix = "api.retry")
public class RetryProperties {
    private int maxAttempts = 3;
    private long initialDelay = 1000;
    private double multiplier = 2.0;
    private long maxDelay = 5000;
    // getters and setters
}
```

#### RetryConfig.java
Creates and configures the RetryTemplate bean:
```java
@Configuration
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        // Configures retry policy and exponential backoff
        // based on values from RetryProperties
    }
}
```

### 4. Service Layer Implementation

Updated `ProductService` to use RetryTemplate instead of annotations:

**Before** (annotation-based):
```java
@Retryable(
    retryFor = {RestClientException.class, ResourceAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000)
)
public List<Product> getAllProducts() { ... }
```

**After** (template-based with externalized config):
```java
public List<Product> getAllProducts() {
    return retryTemplate.execute(context -> {
        // Main execution logic
    }, context -> {
        // Recovery callback
    });
}
```

### 5. Configuration Details

#### Retry Policy
- **Max Attempts**: From `api.retry.max-attempts`
- **Initial Delay**: From `api.retry.initial-delay` (milliseconds)
- **Backoff Multiplier**: From `api.retry.multiplier`
- **Max Delay**: From `api.retry.max-delay` (milliseconds)

#### Retryable Exceptions
- `RestClientException` - General REST client errors
- `ResourceAccessException` - Network/timeout errors

### 6. Logging Enhancement
Now includes attempt count in logs:
```
INFO: Attempting to fetch all products from external API (attempt: 1)
INFO: Attempting to fetch all products from external API (attempt: 2)
ERROR: Failed to fetch all products after 3 attempts: Connection refused
```

## Configuration Examples

### Default Configuration (Current)
```properties
api.retry.max-attempts=3
api.retry.initial-delay=1000
api.retry.multiplier=2.0
api.retry.max-delay=5000
```
**Result**: 1s → 2s → 4s

### Aggressive Retry (Fast Recovery)
```properties
api.retry.max-attempts=5
api.retry.initial-delay=500
api.retry.multiplier=1.5
api.retry.max-delay=3000
```
**Result**: 500ms → 750ms → 1125ms → 1687ms → 2531ms

### Conservative Retry (Slow, Steady)
```properties
api.retry.max-attempts=2
api.retry.initial-delay=2000
api.retry.multiplier=3.0
api.retry.max-delay=10000
```
**Result**: 2s → 6s

### Environment-Specific Configuration

**application-dev.properties** (Development):
```properties
api.retry.max-attempts=2
api.retry.initial-delay=500
api.retry.multiplier=2.0
api.retry.max-delay=2000
```

**application-prod.properties** (Production):
```properties
api.retry.max-attempts=5
api.retry.initial-delay=1000
api.retry.multiplier=2.0
api.retry.max-delay=10000
```

## When Retries Occur

- Network timeouts
- Connection refused
- Temporary service unavailability
- HTTP 5xx errors (server errors)

## When Retries DON'T Occur

- HTTP 4xx errors (client errors like 404, 401)
- Successful responses (2xx)
- Non-retriable exceptions

## Testing the Implementation

To test retry behavior:

1. **Simulate network failure**:
   - Stop the external API temporarily
   - Make a request to the application
   - Check logs to see retry attempts

2. **Check logs**:
   ```bash
   ./mvnw spring-boot:run
   # In another terminal:
   curl http://localhost:8083/api/products
   ```

3. **Expected log output**:
   ```
   INFO: Attempting to fetch all products from external API
   INFO: Attempting to fetch all products from external API (after 1s)
   INFO: Attempting to fetch all products from external API (after 2s)
   ERROR: Failed to fetch all products after retries: ...
   ```

## Production Considerations

### Next Steps for Full Production Readiness:

1. **Circuit Breaker** (Resilience4j)
   - Prevent cascading failures
   - Open circuit after repeated failures
   
2. **Metrics & Monitoring**
   - Track retry rates
   - Monitor success/failure ratios
   - Alert on high retry rates

3. **Configurable Settings**
   - Move retry config to application.properties
   - Allow different retry strategies per endpoint

4. **Rate Limiting**
   - Respect external API limits
   - Implement token bucket or leaky bucket

## Code Changes Summary

### Files Modified:
1. `pom.xml` - Added Spring Retry dependencies
2. `application.properties` - Added retry configuration properties
3. `ProductService.java` - Refactored to use RetryTemplate instead of annotations

### Files Created:
1. `RetryProperties.java` - Configuration properties binding
2. `RetryConfig.java` - RetryTemplate bean configuration
3. `RETRY_IMPLEMENTATION.md` - This documentation

### Files Removed:
- `@EnableRetry` from SystemdesignApplication.java (not needed with RetryTemplate)

## Verification

✅ Build successful  
✅ No compilation errors  
✅ Dependencies resolved  
✅ Configuration externalized  
✅ RetryTemplate properly configured  
✅ Recovery callbacks in place  
✅ Enhanced logging with attempt counts  
✅ Type-safe configuration binding  

## Migration Path (Annotation → Template)

If you prefer annotation-based retry, you can revert to the simpler approach:

1. Keep `@EnableRetry` on SystemdesignApplication
2. Use `@Retryable` with SpEL expressions to read properties:
```java
@Retryable(
    retryFor = {RestClientException.class},
    maxAttemptsExpression = "${api.retry.max-attempts}",
    backoff = @Backoff(
        delayExpression = "${api.retry.initial-delay}",
        multiplierExpression = "${api.retry.multiplier}",
        maxDelayExpression = "${api.retry.max-delay}"
    )
)
```

**Current implementation uses RetryTemplate for more control and testability.**
