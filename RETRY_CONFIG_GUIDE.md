# Retry Configuration Quick Reference

## Overview
Retry mechanism is now fully externalized and configurable via `application.properties`.

## Configuration Properties

| Property | Description | Default | Example |
|----------|-------------|---------|---------|
| `api.retry.max-attempts` | Maximum number of retry attempts | 3 | 5 |
| `api.retry.initial-delay` | Initial delay in milliseconds | 1000 | 500 |
| `api.retry.multiplier` | Exponential backoff multiplier | 2.0 | 1.5 |
| `api.retry.max-delay` | Maximum delay between retries (ms) | 5000 | 10000 |

## Quick Examples

### 1. Change Retry Attempts
```properties
api.retry.max-attempts=5
```

### 2. Faster Retries
```properties
api.retry.initial-delay=500
api.retry.multiplier=1.5
```

### 3. Slower, More Patient Retries
```properties
api.retry.initial-delay=2000
api.retry.multiplier=3.0
api.retry.max-delay=10000
```

### 4. Disable Retry (Not Recommended)
```properties
api.retry.max-attempts=1
```

## Environment-Specific Configuration

### Using Spring Profiles

**application.properties** (base):
```properties
api.retry.max-attempts=3
api.retry.initial-delay=1000
api.retry.multiplier=2.0
api.retry.max-delay=5000
```

**application-dev.properties** (development override):
```properties
api.retry.max-attempts=2
api.retry.initial-delay=500
```

**application-prod.properties** (production override):
```properties
api.retry.max-attempts=5
api.retry.initial-delay=1000
api.retry.max-delay=10000
```

Run with profile:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

## Environment Variables Override

You can also override via environment variables:
```bash
export API_RETRY_MAX_ATTEMPTS=5
export API_RETRY_INITIAL_DELAY=2000
./mvnw spring-boot:run
```

## Architecture

```
application.properties
        ↓
   RetryProperties (@ConfigurationProperties)
        ↓
   RetryConfig (creates RetryTemplate bean)
        ↓
   ProductService (uses RetryTemplate)
```

## Key Classes

1. **RetryProperties** - Binds properties from application.properties
   - Location: `config/RetryProperties.java`
   - Prefix: `api.retry`

2. **RetryConfig** - Creates RetryTemplate bean
   - Location: `config/RetryConfig.java`
   - Configures retry policy and backoff strategy

3. **ProductService** - Uses RetryTemplate
   - Location: `service/ProductService.java`
   - Executes API calls with retry logic

## Testing Configuration Changes

### 1. Modify application.properties
```properties
api.retry.max-attempts=2
api.retry.initial-delay=500
```

### 2. Run application
```bash
./mvnw spring-boot:run
```

### 3. Test with curl
```bash
# In another terminal, if API is down, you'll see 2 attempts with 500ms delay
curl http://localhost:8082/api/products
```

### 4. Check logs
```
INFO: Attempting to fetch all products from external API (attempt: 1)
INFO: Attempting to fetch all products from external API (attempt: 2)
ERROR: Failed to fetch all products after 2 attempts: ...
```

## Retry Timing Calculator

| Attempts | Initial | Multiplier | Pattern |
|----------|---------|------------|---------|
| 3 | 1000ms | 2.0 | 1s → 2s → 4s |
| 5 | 500ms | 1.5 | 500ms → 750ms → 1125ms → 1687ms → 2531ms |
| 4 | 2000ms | 2.5 | 2s → 5s → 12.5s (capped by max-delay) |
| 2 | 1500ms | 2.0 | 1.5s → 3s |

## Common Use Cases

### High-Volume APIs (minimize delay)
```properties
api.retry.max-attempts=2
api.retry.initial-delay=500
api.retry.multiplier=1.5
api.retry.max-delay=2000
```

### Critical APIs (maximize success)
```properties
api.retry.max-attempts=5
api.retry.initial-delay=1000
api.retry.multiplier=2.0
api.retry.max-delay=10000
```

### Rate-Limited APIs (respect limits)
```properties
api.retry.max-attempts=3
api.retry.initial-delay=2000
api.retry.multiplier=3.0
api.retry.max-delay=15000
```

## Monitoring

Watch for these log patterns:

**Success after retry**:
```
INFO: Attempting... (attempt: 1)
INFO: Attempting... (attempt: 2)
# Success on attempt 2
```

**All retries exhausted**:
```
INFO: Attempting... (attempt: 1)
INFO: Attempting... (attempt: 2)
INFO: Attempting... (attempt: 3)
ERROR: Failed... after 3 attempts: Connection refused
```

## Best Practices

1. ✅ Start conservative (fewer attempts, longer delays)
2. ✅ Monitor retry rates and adjust
3. ✅ Use different configs per environment
4. ✅ Consider external API rate limits
5. ✅ Set reasonable max-delay to prevent indefinite waits
6. ❌ Don't set max-attempts too high (increases latency)
7. ❌ Don't set delays too short (may overwhelm failing services)

## Troubleshooting

**Problem**: Too many retries causing slow response times  
**Solution**: Reduce `max-attempts` or increase `initial-delay`

**Problem**: Not enough retries, missing transient failures  
**Solution**: Increase `max-attempts` to 4-5

**Problem**: Overwhelming external API  
**Solution**: Increase `initial-delay` and `multiplier`

**Problem**: Changes not taking effect  
**Solution**: Restart application (properties are loaded at startup)
