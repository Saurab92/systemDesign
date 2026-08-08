package com.systemdesign.demo.systemdesign.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {
    
    private CircuitBreaker circuitBreaker;
    
    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker("test-service", 50, 2, 1000, 3);
    }
    
    @Test
    void testInitialStateIsClosed() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void testSuccessfulCallIncrementsMetrics() throws Exception {
        Callable<String> operation = () -> "success";
        
        String result = circuitBreaker.execute(operation);
        
        assertEquals("success", result);
        assertEquals(1, circuitBreaker.getMetrics().getSuccessCount());
        assertEquals(0, circuitBreaker.getMetrics().getFailureCount());
    }
    
    @Test
    void testFailedCallIncrementsFailureMetrics() {
        Callable<String> operation = () -> {
            throw new RuntimeException("Service error");
        };
        
        assertThrows(RuntimeException.class, () -> circuitBreaker.execute(operation));
        
        assertEquals(0, circuitBreaker.getMetrics().getSuccessCount());
        assertEquals(1, circuitBreaker.getMetrics().getFailureCount());
    }
    
    @Test
    void testCircuitOpensAfterFailureThresholdExceeded() throws Exception {
        Callable<String> failingOperation = () -> {
            throw new RuntimeException("Service error");
        };
        
        // Execute enough calls to meet minimum threshold
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.execute(failingOperation);
            } catch (Exception e) {
                // Expected
            }
        }
        
        // Circuit should be open now (100% failure rate > 50% threshold)
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitBlocksCallsWhenOpen() throws Exception {
        // Force circuit to open
        Callable<String> failingOperation = () -> {
            throw new RuntimeException("Service error");
        };
        
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(failingOperation);
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Get current rejection count
        int rejectedBefore = circuitBreaker.getMetrics().getRejectedCount();
        
        // Next call should be rejected
        assertThrows(CircuitBreakerException.class, 
            () -> circuitBreaker.execute(() -> "test"));
        
        // Verify rejection count increased
        assertTrue(circuitBreaker.getMetrics().getRejectedCount() > rejectedBefore);
    }
    
    @Test
    void testFallbackExecutedWhenCircuitOpen() throws Exception {
        // Force circuit to open
        Callable<String> failingOperation = () -> {
            throw new RuntimeException("Service error");
        };
        
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(failingOperation);
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Execute with fallback
        String result = circuitBreaker.execute(() -> "primary", () -> "fallback");
        
        assertEquals("fallback", result);
    }
    
    @Test
    void testCircuitTransitionsToHalfOpenAfterTimeout() throws Exception {
        // Force circuit to open
        Callable<String> failingOperation = () -> {
            throw new RuntimeException("Service error");
        };
        
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(failingOperation);
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Wait for timeout
        Thread.sleep(1100);
        
        // Next call should transition to HALF_OPEN
        try {
            circuitBreaker.execute(() -> "success");
        } catch (Exception e) {
            // May fail, but state should change
        }
        
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitClosesAfterSuccessfulCallsInHalfOpen() throws Exception {
        // Force circuit to open
        Callable<String> failingOperation = () -> {
            throw new RuntimeException("Service error");
        };
        
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(failingOperation);
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Wait for timeout
        Thread.sleep(1100);
        
        // Execute successful calls (need 2 successes based on threshold)
        circuitBreaker.execute(() -> "success");
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());
        
        circuitBreaker.execute(() -> "success");
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }
    
    @Test
    void testCircuitReopensOnFailureInHalfOpen() throws Exception {
        // Force circuit to open
        Callable<String> failingOperation = () -> {
            throw new RuntimeException("Service error");
        };
        
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(failingOperation);
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Wait for timeout
        Thread.sleep(1100);
        
        // Execute a failed call in HALF_OPEN
        try {
            circuitBreaker.execute(failingOperation);
        } catch (Exception e) {
            // Expected
        }
        
        // Should reopen
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }
    
    @Test
    void testMinimumCallsRequirementPreventsEarlyOpening() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("test", 50, 2, 1000, 10);
        
        // Execute only 2 failing calls (less than minimum 10)
        for (int i = 0; i < 2; i++) {
            try {
                cb.execute(() -> {
                    throw new RuntimeException("error");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        // Circuit should still be closed
        assertEquals(CircuitBreakerState.CLOSED, cb.getState());
    }
    
    @Test
    void testFailureRateCalculation() throws Exception {
        // Execute 3 successes and 2 failures
        for (int i = 0; i < 3; i++) {
            circuitBreaker.execute(() -> "success");
        }
        
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("error");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        // Failure rate should be 40% (2/5)
        assertEquals(40.0, circuitBreaker.getMetrics().getFailureRate(), 0.1);
    }
    
    @Test
    void testManualReset() throws Exception {
        // Force circuit to open
        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("error");
                });
            } catch (Exception e) {
                // Expected
            }
        }
        
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        
        // Manual reset
        circuitBreaker.reset();
        
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getMetrics().getSuccessCount());
        assertEquals(0, circuitBreaker.getMetrics().getFailureCount());
    }
    
    @Test
    void testConcurrentAccess() throws Exception {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // Execute concurrent operations
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    if (index % 2 == 0) {
                        circuitBreaker.execute(() -> "success");
                        successCount.incrementAndGet();
                    } else {
                        circuitBreaker.execute(() -> {
                            throw new RuntimeException("error");
                        });
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify metrics are consistent
        int totalCalls = circuitBreaker.getMetrics().getTotalCount();
        assertTrue(totalCalls > 0);
    }
}
