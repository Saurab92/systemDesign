package com.systemdesign.demo.systemdesign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${api.dummyjson.base-url}")
    private String baseUrl;

    @Value("${api.dummyjson.connect-timeout}")
    private int connectTimeout;

    @Value("${api.dummyjson.read-timeout}")
    private int readTimeout;

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout * 1000); // Convert to milliseconds
        factory.setReadTimeout(readTimeout * 1000); // Convert to milliseconds
        
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
