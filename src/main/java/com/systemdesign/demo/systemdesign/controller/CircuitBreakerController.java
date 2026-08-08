package com.systemdesign.demo.systemdesign.controller;

import com.systemdesign.demo.systemdesign.circuitbreaker.CircuitBreaker;
import com.systemdesign.demo.systemdesign.circuitbreaker.CircuitBreakerState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/circuit-breaker")
public class CircuitBreakerController {
    
    private final CircuitBreaker circuitBreaker;
    
    public CircuitBreakerController(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("name", circuitBreaker.getName());
        status.put("state", circuitBreaker.getState().toString());
        status.put("metrics", buildMetricsMap());
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        circuitBreaker.reset();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Circuit breaker reset successfully");
        response.put("state", circuitBreaker.getState().toString());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        CircuitBreakerState state = circuitBreaker.getState();
        
        health.put("status", state == CircuitBreakerState.CLOSED ? "UP" : "DOWN");
        health.put("circuitBreakerState", state.toString());
        health.put("details", buildMetricsMap());
        
        return state == CircuitBreakerState.CLOSED ? 
            ResponseEntity.ok(health) : 
            ResponseEntity.status(503).body(health);
    }
    
    private Map<String, Object> buildMetricsMap() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("successCount", circuitBreaker.getMetrics().getSuccessCount());
        metrics.put("failureCount", circuitBreaker.getMetrics().getFailureCount());
        metrics.put("rejectedCount", circuitBreaker.getMetrics().getRejectedCount());
        metrics.put("totalCount", circuitBreaker.getMetrics().getTotalCount());
        metrics.put("failureRate", String.format("%.2f%%", circuitBreaker.getMetrics().getFailureRate()));
        return metrics;
    }
}
