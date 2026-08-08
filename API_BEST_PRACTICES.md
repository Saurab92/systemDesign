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

## Project Structure

```
src/main/java/com/systemdesign/demo/systemdesign/
├── SystemdesignApplication.java
├── config/
│   └── RestClientConfig.java         # RestClient bean configuration
├── controller/
│   ├── HolaController.java
│   └── ProductController.java        # REST endpoints
├── dto/
│   ├── Product.java                  # Product DTO
│   └── ProductsResponse.java         # API response wrapper
├── exception/
│   └── GlobalExceptionHandler.java   # Centralized error handling
└── service/
    └── ProductService.java           # Business logic & API calls
```

## Additional Best Practices to Consider

### For Production Applications:

1. **Timeouts & Resilience**
   - Configure connection and read timeouts
   - Implement retry logic with exponential backoff
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
