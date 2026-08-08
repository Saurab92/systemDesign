package com.systemdesign.demo.systemdesign.config;

import com.systemdesign.demo.systemdesign.circuitbreaker.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker productServiceCircuitBreaker(CircuitBreakerProperties properties) {
        return new CircuitBreaker(
            "product-service",
            properties.getFailureThreshold(),
            properties.getSuccessThreshold(),
            properties.getTimeout(),
            properties.getMinimumNumberOfCalls()
        );
    }
}
