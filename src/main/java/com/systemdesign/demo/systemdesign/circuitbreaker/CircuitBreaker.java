package com.systemdesign.demo.systemdesign.circuitbreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CircuitBreaker {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    private final String name;
    private final int failureThreshold;
    private final int successThreshold;
    private final long timeout;
    private final int minimumNumberOfCalls;
    private final AtomicReference<CircuitBreakerState> state;
    private final CircuitBreakerMetrics metrics;
    private final AtomicInteger halfOpenSuccessCount;
    
    public CircuitBreaker(String name, int failureThreshold, int successThreshold, 
                         long timeout, int minimumNumberOfCalls) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.timeout = timeout;
        this.minimumNumberOfCalls = minimumNumberOfCalls;
        this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        this.metrics = new CircuitBreakerMetrics();
        this.halfOpenSuccessCount = new AtomicInteger(0);
        
        logger.info("Circuit breaker '{}' initialized with failureThreshold={}%, successThreshold={}, " +
                   "timeout={}ms, minimumCalls={}", 
                   name, failureThreshold, successThreshold, timeout, minimumNumberOfCalls);
    }
    
    public <T> T execute(Callable<T> operation) throws Exception {
        return execute(operation, null);
    }
    
    public <T> T execute(Callable<T> operation, Supplier<T> fallback) throws Exception {
        CircuitBreakerState currentState = state.get();
        
        if (!canAttemptCall()) {
            metrics.recordRejection();
            String message = String.format(
                "Circuit breaker '%s' is %s - rejecting call (failures: %d, rate: %.2f%%)",
                name, currentState, metrics.getFailureCount(), metrics.getFailureRate()
            );
            logger.warn(message);
            
            if (fallback != null) {
                logger.info("Circuit breaker '{}' executing fallback", name);
                return fallback.get();
            }
            throw new CircuitBreakerException(message, currentState);
        }
        
        try {
            T result = operation.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    private boolean canAttemptCall() {
        CircuitBreakerState currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (System.currentTimeMillis() - metrics.getStateTransitionTime() >= timeout) {
                    logger.info("Circuit breaker '{}' timeout elapsed, transitioning to HALF_OPEN", name);
                    transitionToHalfOpen();
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }
    
    private void onSuccess() {
        metrics.recordSuccess();
        CircuitBreakerState currentState = state.get();
        
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            int successCount = halfOpenSuccessCount.incrementAndGet();
            logger.debug("Circuit breaker '{}' in HALF_OPEN: success {} of {}", 
                        name, successCount, successThreshold);
            
            if (successCount >= successThreshold) {
                transitionToClosed();
            }
        }
        
        logger.debug("Circuit breaker '{}' call succeeded. State: {}, Metrics: {}", 
                    name, state.get(), metrics);
    }
    
    private void onFailure() {
        metrics.recordFailure();
        CircuitBreakerState currentState = state.get();
        
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            logger.warn("Circuit breaker '{}' call failed in HALF_OPEN state, reopening circuit", name);
            transitionToOpen();
        } else if (currentState == CircuitBreakerState.CLOSED) {
            if (shouldOpenCircuit()) {
                transitionToOpen();
            }
        }
        
        logger.debug("Circuit breaker '{}' call failed. State: {}, Metrics: {}", 
                    name, state.get(), metrics);
    }
    
    private boolean shouldOpenCircuit() {
        int totalCalls = metrics.getTotalCount();
        
        if (totalCalls < minimumNumberOfCalls) {
            logger.debug("Circuit breaker '{}' has only {} calls, minimum {} required", 
                        name, totalCalls, minimumNumberOfCalls);
            return false;
        }
        
        double failureRate = metrics.getFailureRate();
        boolean shouldOpen = failureRate >= failureThreshold;
        
        if (shouldOpen) {
            logger.warn("Circuit breaker '{}' failure rate {:.2f}% exceeds threshold {}%", 
                       name, failureRate, failureThreshold);
        }
        
        return shouldOpen;
    }
    
    private void transitionToOpen() {
        if (state.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN) ||
            state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN)) {
            metrics.recordStateTransition();
            logger.error("Circuit breaker '{}' transitioned to OPEN. Metrics: {}", name, metrics);
        }
    }
    
    private void transitionToHalfOpen() {
        if (state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN)) {
            metrics.recordStateTransition();
            halfOpenSuccessCount.set(0);
            logger.info("Circuit breaker '{}' transitioned to HALF_OPEN. Metrics: {}", name, metrics);
        }
    }
    
    private void transitionToClosed() {
        if (state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED)) {
            metrics.recordStateTransition();
            metrics.reset();
            halfOpenSuccessCount.set(0);
            logger.info("Circuit breaker '{}' transitioned to CLOSED. Service recovered.", name);
        }
    }
    
    public CircuitBreakerState getState() {
        return state.get();
    }
    
    public CircuitBreakerMetrics getMetrics() {
        return metrics;
    }
    
    public String getName() {
        return name;
    }
    
    public void reset() {
        state.set(CircuitBreakerState.CLOSED);
        metrics.reset();
        halfOpenSuccessCount.set(0);
        logger.info("Circuit breaker '{}' manually reset to CLOSED", name);
    }
}
