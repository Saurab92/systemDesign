package com.systemdesign.demo.systemdesign.circuitbreaker;

public class CircuitBreakerException extends RuntimeException {
    
    private final CircuitBreakerState state;
    
    public CircuitBreakerException(String message, CircuitBreakerState state) {
        super(message);
        this.state = state;
    }
    
    public CircuitBreakerState getState() {
        return state;
    }
}
