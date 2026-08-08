package com.systemdesign.demo.systemdesign.service;

import com.systemdesign.demo.systemdesign.dto.Product;
import com.systemdesign.demo.systemdesign.dto.ProductsResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ProductService {

    private final RestClient restClient;

    public ProductService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Product> getAllProducts() {
        ProductsResponse response = restClient.get()
                .uri("/products")
                .retrieve()
                .body(ProductsResponse.class);

        return response != null ? response.getProducts() : List.of();
    }

    public Product getProductById(Long id) {
        return restClient.get()
                .uri("/products/{id}", id)
                .retrieve()
                .body(Product.class);
    }
}
