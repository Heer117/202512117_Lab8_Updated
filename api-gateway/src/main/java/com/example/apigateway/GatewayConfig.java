package com.example.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayConfig {

    @Value("${gateway.user-service-url}")
    private String userServiceUrl;

    @Value("${gateway.product-service-url}")
    private String productServiceUrl;

    @Value("${gateway.order-service-url}")
    private String orderServiceUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    public String getUserServiceUrl() {
        return userServiceUrl;
    }

    public String getProductServiceUrl() {
        return productServiceUrl;
    }

    public String getOrderServiceUrl() {
        return orderServiceUrl;
    }
}   