package com.systemdesign.demo.systemdesign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "circuit-breaker")
public class CircuitBreakerProperties {
    
    private int failureThreshold = 50;
    private int successThreshold = 2;
    private long timeout = 60000;
    private int minimumNumberOfCalls = 5;
    
    public int getFailureThreshold() {
        return failureThreshold;
    }
    
    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }
    
    public int getSuccessThreshold() {
        return successThreshold;
    }
    
    public void setSuccessThreshold(int successThreshold) {
        this.successThreshold = successThreshold;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }
    
    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
        this.minimumNumberOfCalls = minimumNumberOfCalls;
    }
}
