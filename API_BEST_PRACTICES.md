# External API Integration - Best Practices

This project demonstrates best practices for consuming external REST APIs in Spring Boot.

## Architecture Overview

The implementation follows a clean, layered architecture:

```
Controller Layer → Service Layer → External API
     ↓                 ↓
   DTOs          RestClient Config
     ↓
Exception Handler
```

## Key Best Practices Implemented

### 1. **Layered Architecture**
- **Controller**: Handles HTTP requests/responses (`ProductController`)
- **Service**: Contains business logic (`ProductService`)
- **DTOs**: Data Transfer Objects for API responses (`Product`, `ProductsResponse`)
- **Config**: Centralized configuration (`RestClientConfig`)

### 2. **Dependency Injection**
- Using constructor injection (recommended over field injection)
- Makes testing easier and dependencies explicit

### 3. **Modern RestClient (Spring 6.1+)**
- Using the new `RestClient` instead of deprecated `RestTemplate`
- More fluent API and better performance
- Configured with base URL in a centralized bean

### 4. **DTOs with Jackson Annotations**
- `@JsonIgnoreProperties(ignoreUnknown = true)` - handles extra fields from API
- Prevents breaking changes when external API adds new fields

### 5. **Global Exception Handling**
- `@RestControllerAdvice` for centralized error handling
- Handles `HttpClientErrorException`, `RestClientException`, and generic exceptions
- Returns consistent error response format

### 6. **Proper HTTP Status Codes**
- Using `ResponseEntity` for explicit status code control
- Following REST conventions

### 7. **Clean API Design**
- RESTful endpoints: `/api/products`, `/api/products/{id}`
- Meaningful path variables and resource naming

### 8. **Retry Mechanism with Exponential Backoff** ✅
- Using Spring Retry with RetryTemplate
- Externalized configuration in application.properties
- Configurable retry attempts, delays, and backoff multiplier
- Exponential backoff: 1s → 2s → 4s (max 5s, configurable)
- Graceful fallback with recovery callbacks
- Retries on `RestClientException` and `ResourceAccessException`
- Detailed logging with attempt counts for observability

### 9. **Externalized Configuration** ✅
- All retry settings in application.properties
- Easy to adjust per environment (dev/staging/prod)
- No code changes needed to tune retry behavior
- Type-safe configuration binding with @ConfigurationProperties

## API Endpoints

### Get All Products
```bash
GET http://localhost:8083/api/products
```

### Get Product by ID
```bash
GET http://localhost:8083/api/products/{id}
```

## Running the Application

```bash
# Default port (8080)
./mvnw spring-boot:run

# Custom port
SERVER_PORT=8083 ./mvnw spring-boot:run
```

## Testing the Endpoints

```bash
# Get all products
curl http://localhost:8083/api/products | jq

# Get specific product
curl http://localhost:8083/api/products/5 | jq
```

## Retry Mechanism Details

The application implements automatic retry logic for external API calls using Spring Retry with externalized configuration:

### Configuration

**Dependencies** (pom.xml):
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

**Application Properties** (application.properties):
```properties
# Retry Configuration
api.retry.max-attempts=3
api.retry.initial-delay=1000
api.retry.multiplier=2.0
api.retry.max-delay=5000
```

**Configuration Classes**:
- `RetryProperties` - Binds retry properties from application.properties
- `RetryConfig` - Creates and configures RetryTemplate bean
- `ProductService` - Uses RetryTemplate for all external API calls

### Retry Behavior

- **Max Attempts**: Configurable via `api.retry.max-attempts` (default: 3)
- **Initial Delay**: Configurable via `api.retry.initial-delay` (default: 1000ms)
- **Backoff Multiplier**: Configurable via `api.retry.multiplier` (default: 2.0)
- **Max Delay**: Configurable via `api.retry.max-delay` (default: 5000ms)
- **Retry Pattern**: 1s → 2s → 4s (capped at 5s)

### Exceptions Handled

Retries are triggered for:
- `RestClientException` - General REST client errors
- `ResourceAccessException` - Network/timeout errors

### Recovery Strategy

If all retry attempts fail:
- `getAllProducts()` returns an empty list
- `getProductById()` returns null
- Error is logged with full context and attempt count

### Example Scenario

```
Request: GET /api/products
↓
Attempt 1: Network timeout → Wait 1s
Attempt 2: Connection refused → Wait 2s
Attempt 3: Success → Return products
```

### Observability

Each attempt logs:
```
INFO: Attempting to fetch all products from external API (attempt: 1)
INFO: Attempting to fetch all products from external API (attempt: 2)
INFO: Attempting to fetch all products from external API (attempt: 3)
ERROR: Failed to fetch all products after 3 attempts: Connection refused
```

### Customizing Retry Configuration

To adjust retry behavior, modify `application.properties`:

```properties
# More aggressive retry (5 attempts, faster backoff)
api.retry.max-attempts=5
api.retry.initial-delay=500
api.retry.multiplier=1.5
api.retry.max-delay=3000

# Conservative retry (2 attempts, longer delays)
api.retry.max-attempts=2
api.retry.initial-delay=2000
api.retry.multiplier=3.0
api.retry.max-delay=10000
```

## Project Structure

```
src/main/java/com/systemdesign/demo/systemdesign/
├── SystemdesignApplication.java
├── config/
│   ├── RestClientConfig.java         # RestClient bean configuration
│   ├── RetryConfig.java              # RetryTemplate configuration
│   └── RetryProperties.java          # Retry properties binding
├── controller/
│   ├── HolaController.java
│   └── ProductController.java        # REST endpoints
├── dto/
│   ├── Product.java                  # Product DTO
│   └── ProductsResponse.java         # API response wrapper
├── exception/
│   └── GlobalExceptionHandler.java   # Centralized error handling
└── service/
    └── ProductService.java           # Business logic & API calls with retry

src/main/resources/
└── application.properties            # Configuration (timeouts, retry settings)
```

## Additional Best Practices to Consider

### For Production Applications:

1. **Timeouts & Resilience** ✅
   - ✅ Configure connection and read timeouts (Implemented)
   - ✅ Implement retry logic with exponential backoff (Implemented with Spring Retry)
   - Use circuit breakers (Resilience4j)

2. **Caching**
   - Use `@Cacheable` for frequently accessed data
   - Reduce external API calls

3. **Monitoring & Logging**
   - Add structured logging
   - Monitor API call latency
   - Track error rates

4. **Security**
   - Store API keys in environment variables
   - Use SSL/TLS for external calls
   - Validate and sanitize responses

5. **Testing**
   - Unit tests with MockRestClient
   - Integration tests with WireMock
   - Contract testing with Pact

6. **Rate Limiting**
   - Respect external API rate limits
   - Implement backoff strategies

7. **API Versioning**
   - Version your own APIs (`/api/v1/products`)
   - Handle external API version changes gracefully

## External API Used
- **DummyJSON Products API**: https://dummyjson.com/products
- Free fake API for testing and prototyping
