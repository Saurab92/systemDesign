package com.systemdesign.demo.systemdesign.service;

import com.systemdesign.demo.systemdesign.circuitbreaker.CircuitBreaker;
import com.systemdesign.demo.systemdesign.dto.Product;
import com.systemdesign.demo.systemdesign.dto.ProductsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final RestClient restClient;
    private final RetryTemplate retryTemplate;
    private final CircuitBreaker circuitBreaker;

    public ProductService(RestClient restClient, RetryTemplate retryTemplate, CircuitBreaker circuitBreaker) {
        this.restClient = restClient;
        this.retryTemplate = retryTemplate;
        this.circuitBreaker = circuitBreaker;
    }

    public List<Product> getAllProducts() {
        try {
            return circuitBreaker.execute(() -> {
                return retryTemplate.execute(context -> {
                    logger.info("Attempting to fetch all products from external API (attempt: {})", 
                            context.getRetryCount() + 1);
                    
                    ProductsResponse response = restClient.get()
                            .uri("/products")
                            .retrieve()
                            .body(ProductsResponse.class);

                    return response != null ? response.getProducts() : List.of();
                }, context -> {
                    logger.error("Failed to fetch all products after {} attempts: {}", 
                            context.getRetryCount(), 
                            context.getLastThrowable().getMessage());
                    throw new RuntimeException("Failed to fetch products", context.getLastThrowable());
                });
            }, () -> {
                logger.warn("Circuit breaker fallback: returning empty product list");
                return List.of();
            });
        } catch (Exception e) {
            logger.error("Error fetching all products: {}", e.getMessage());
            return List.of();
        }
    }

    public Product getProductById(Long id) {
        try {
            return circuitBreaker.execute(() -> {
                return retryTemplate.execute(context -> {
                    logger.info("Attempting to fetch product with id: {} (attempt: {})", 
                            id, context.getRetryCount() + 1);
                    
                    return restClient.get()
                            .uri("/products/{id}", id)
                            .retrieve()
                            .body(Product.class);
                }, context -> {
                    logger.error("Failed to fetch product with id {} after {} attempts: {}", 
                            id, 
                            context.getRetryCount(), 
                            context.getLastThrowable().getMessage());
                    throw new RuntimeException("Failed to fetch product", context.getLastThrowable());
                });
            }, () -> {
                logger.warn("Circuit breaker fallback: returning null for product id {}", id);
                return null;
            });
        } catch (Exception e) {
            logger.error("Error fetching product with id {}: {}", id, e.getMessage());
            return null;
        }
    }
}

