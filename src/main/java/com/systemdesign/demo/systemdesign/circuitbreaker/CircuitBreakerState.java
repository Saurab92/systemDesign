package com.systemdesign.demo.systemdesign.circuitbreaker;

public enum CircuitBreakerState {
    CLOSED,      // Normal operation, requests pass through
    OPEN,        // Circuit is open, requests fail fast
    HALF_OPEN    // Testing if service recovered, limited requests allowed
}
