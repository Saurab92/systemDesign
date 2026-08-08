package com.systemdesign.demo.systemdesign.circuitbreaker;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CircuitBreakerMetrics {
    
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger rejectedCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(0);
    private final AtomicLong stateTransitionTime = new AtomicLong(System.currentTimeMillis());
    
    public void recordSuccess() {
        successCount.incrementAndGet();
        lastSuccessTime.set(System.currentTimeMillis());
    }
    
    public void recordFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
    }
    
    public void recordRejection() {
        rejectedCount.incrementAndGet();
    }
    
    public void reset() {
        successCount.set(0);
        failureCount.set(0);
        rejectedCount.set(0);
    }
    
    public void recordStateTransition() {
        stateTransitionTime.set(System.currentTimeMillis());
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public int getRejectedCount() {
        return rejectedCount.get();
    }
    
    public int getTotalCount() {
        return successCount.get() + failureCount.get();
    }
    
    public double getFailureRate() {
        int total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) failureCount.get() / total * 100;
    }
    
    public long getLastFailureTime() {
        return lastFailureTime.get();
    }
    
    public long getLastSuccessTime() {
        return lastSuccessTime.get();
    }
    
    public long getStateTransitionTime() {
        return stateTransitionTime.get();
    }
    
    @Override
    public String toString() {
        return String.format(
            "Metrics{success=%d, failure=%d, rejected=%d, failureRate=%.2f%%, lastStateTransition=%s}",
            successCount.get(),
            failureCount.get(),
            rejectedCount.get(),
            getFailureRate(),
            Instant.ofEpochMilli(stateTransitionTime.get())
        );
    }
}
