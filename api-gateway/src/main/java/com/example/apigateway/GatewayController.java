package com.example.apigateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@RestController
public class GatewayController {

    private final RestClient restClient;
    private final GatewayConfig gatewayConfig;

    public GatewayController(RestClient restClient, GatewayConfig gatewayConfig) {
        this.restClient = restClient;
        this.gatewayConfig = gatewayConfig;
    }

    // ---------------------------------------------------------
    // HEALTH CHECK
    // ---------------------------------------------------------

    @GetMapping("/health")
    public ResponseEntity<String> health() {

        System.out.println("GET /health -> gateway -> 200");

        return ResponseEntity.ok("""
                {"status":"UP"}
                """);
    }

    // ---------------------------------------------------------
    // USER SERVICE
    // ---------------------------------------------------------

    @RequestMapping(
            value = {"/users", "/users/{id}"},
            method = RequestMethod.GET
    )
    public ResponseEntity<String> getUsers(
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String target = gatewayConfig.getUserServiceUrl() + path;

        return proxy(
                request,
                target,
                HttpMethod.GET,
                null,
                "User Service"
        );
    }

    @PostMapping("/users")
    public ResponseEntity<String> createUser(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getUserServiceUrl() + "/users";

        return proxy(
                request,
                target,
                HttpMethod.POST,
                body,
                "User Service"
        );
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable String id,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getUserServiceUrl()
                        + "/users/"
                        + id;

        return proxy(
                request,
                target,
                HttpMethod.PUT,
                body,
                "User Service"
        );
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable String id,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getUserServiceUrl()
                        + "/users/"
                        + id;

        return proxy(
                request,
                target,
                HttpMethod.DELETE,
                null,
                "User Service"
        );
    }

    // ---------------------------------------------------------
    // PRODUCT SERVICE
    // ---------------------------------------------------------

    @RequestMapping(
            value = {"/products", "/products/{id}"},
            method = RequestMethod.GET
    )
    public ResponseEntity<String> getProducts(
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String target =
                gatewayConfig.getProductServiceUrl() + path;

        return proxy(
                request,
                target,
                HttpMethod.GET,
                null,
                "Product Service"
        );
    }

    @PostMapping("/products")
    public ResponseEntity<String> createProduct(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getProductServiceUrl() + "/products";

        return proxy(
                request,
                target,
                HttpMethod.POST,
                body,
                "Product Service"
        );
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<String> updateProduct(
            @PathVariable String id,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getProductServiceUrl()
                        + "/products/"
                        + id;

        return proxy(
                request,
                target,
                HttpMethod.PUT,
                body,
                "Product Service"
        );
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable String id,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getProductServiceUrl()
                        + "/products/"
                        + id;

        return proxy(
                request,
                target,
                HttpMethod.DELETE,
                null,
                "Product Service"
        );
    }

    // ---------------------------------------------------------
    // ORDER SERVICE
    // ---------------------------------------------------------

    @RequestMapping(
            value = {"/orders", "/orders/{id}"},
            method = RequestMethod.GET
    )
    public ResponseEntity<String> getOrders(
            HttpServletRequest request) {

        String path = request.getRequestURI();
        String target =
                gatewayConfig.getOrderServiceUrl() + path;

        return proxy(
                request,
                target,
                HttpMethod.GET,
                null,
                "Order Service"
        );
    }

    @PostMapping("/orders")
    public ResponseEntity<String> createOrder(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        String target =
                gatewayConfig.getOrderServiceUrl() + "/orders";

        return proxy(
                request,
                target,
                HttpMethod.POST,
                body,
                "Order Service"
        );
    }

    // ---------------------------------------------------------
    // COMMON PROXY METHOD
    // ---------------------------------------------------------

    private ResponseEntity<String> proxy(
            HttpServletRequest request,
            String targetUrl,
            HttpMethod method,
            String body,
            String targetService) {

        String path = request.getRequestURI();

        try {

            RestClient.RequestBodySpec requestSpec =
                    restClient
                            .method(method)
                            .uri(targetUrl);

            String contentType =
                    request.getHeader(HttpHeaders.CONTENT_TYPE);

            if (contentType != null) {
                requestSpec.header(
                        HttpHeaders.CONTENT_TYPE,
                        contentType
                );
            }

            if (body != null && !body.isBlank()) {
                requestSpec.body(body);
            }

            ResponseEntity<String> response =
                    requestSpec
                            .retrieve()
                            .toEntity(String.class);

            System.out.println(
                    method + " "
                            + path
                            + " -> "
                            + targetService
                            + " -> "
                            + response.getStatusCode().value()
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(copyHeaders(response.getHeaders()))
                    .body(response.getBody());

        }

        // -----------------------------------------------------
        // Preserve backend status codes such as 404
        // -----------------------------------------------------

        catch (RestClientResponseException ex) {

            System.out.println(
                    method + " "
                            + path
                            + " -> "
                            + targetService
                            + " -> "
                            + ex.getStatusCode().value()
            );

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ex.getResponseBodyAsString());
        }

        // -----------------------------------------------------
        // Target service unavailable -> 503
        // -----------------------------------------------------

        catch (ResourceAccessException ex) {

            System.out.println(
                    method + " "
                            + path
                            + " -> "
                            + targetService
                            + " -> 503"
            );

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("""
                            {"error":"Target service is unavailable"}
                            """);
        }

        // -----------------------------------------------------
        // Other gateway/proxy error -> 502
        // -----------------------------------------------------

        catch (Exception ex) {

            System.out.println(
                    method + " "
                            + path
                            + " -> "
                            + targetService
                            + " -> 502"
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("""
                            {"error":"Gateway could not reach target service"}
                            """);
        }
    }

    // ---------------------------------------------------------
    // COPY SAFE RESPONSE HEADERS
    // ---------------------------------------------------------

    private HttpHeaders copyHeaders(HttpHeaders source) {

        HttpHeaders headers = new HttpHeaders();

        if (source != null) {

            source.forEach((key, values) -> {

                if (!key.equalsIgnoreCase(
                            HttpHeaders.TRANSFER_ENCODING)
                        && !key.equalsIgnoreCase(
                            HttpHeaders.CONNECTION)) {

                    headers.put(key, values);
                }
            });
        }

        return headers;
    }
}