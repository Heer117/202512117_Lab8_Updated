package com.example.student_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public String userServiceUrl(
            @Value("${USER_SERVICE_URL}") String url) {
        return url;
    }

    @Bean
    public String productServiceUrl(
            @Value("${PRODUCT_SERVICE_URL}") String url) {
        return url;
    }
}