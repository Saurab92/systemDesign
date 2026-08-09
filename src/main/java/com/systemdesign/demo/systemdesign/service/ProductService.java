package com.systemdesign.demo.systemdesign.service;

import com.systemdesign.demo.systemdesign.dto.Product;
import com.systemdesign.demo.systemdesign.dto.ProductsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final RestClient restClient;

    public ProductService(RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getAllProductsFallback")
    @Retry(name = "productService")
    public List<Product> getAllProducts() {
        logger.info("Fetching all products from external API");
        
        ProductsResponse response = restClient.get()
                .uri("/products")
                .retrieve()
                .body(ProductsResponse.class);

        return response != null ? response.getProducts() : List.of();
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "productService")
    public Product getProductById(Long id) {
        logger.info("Fetching product with id: {}", id);
        
        return restClient.get()
                .uri("/products/{id}", id)
                .retrieve()
                .body(Product.class);
    }

    // Fallback methods
    private List<Product> getAllProductsFallback(Exception e) {
        logger.warn("Circuit breaker fallback: returning empty product list. Reason: {}", 
                   e.getMessage());
        return List.of();
    }

    private Product getProductByIdFallback(Long id, Exception e) {
        logger.warn("Circuit breaker fallback: returning null for product id {}. Reason: {}", 
                   id, e.getMessage());
        return null;
    }
}

