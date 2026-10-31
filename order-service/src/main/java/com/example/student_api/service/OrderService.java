package com.example.student_api.service;

import com.example.student_api.model.Order;
import com.example.student_api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient restClient;
    private final String userServiceUrl;
    private final String productServiceUrl;

    public OrderService(
            OrderRepository orderRepository,
            RestClient.Builder restClientBuilder,
            String userServiceUrl,
            String productServiceUrl) {

        this.orderRepository = orderRepository;
        this.restClient = restClientBuilder.build();
        this.userServiceUrl = userServiceUrl;
        this.productServiceUrl = productServiceUrl;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Integer id) {
        return orderRepository.findById(id);
    }

    public Order createOrder(Order order) {

        verifyUser(order.getUserId());
        verifyProduct(order.getProductId());

        Order highestIdOrder = orderRepository.findTopByOrderByIdDesc();

        int nextId = (highestIdOrder == null)
                ? 1
                : highestIdOrder.getId() + 1;

        order.setId(nextId);

        return orderRepository.save(order);
    }

    private void verifyUser(Integer userId) {

        try {
            restClient.get()
                    .uri(userServiceUrl + "/users/" + userId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {

            throw new DependencyNotFoundException(
                    "User with ID " + userId + " not found");

        } catch (RestClientException e) {

            throw new DependencyUnavailableException(
                    "User Service is unavailable");
        }
    }

    private void verifyProduct(Integer productId) {

        try {
            restClient.get()
                    .uri(productServiceUrl + "/products/" + productId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {

            throw new DependencyNotFoundException(
                    "Product with ID " + productId + " not found");

        } catch (RestClientException e) {

            throw new DependencyUnavailableException(
                    "Product Service is unavailable");
        }
    }

    public static class DependencyNotFoundException
            extends RuntimeException {

        public DependencyNotFoundException(String message) {
            super(message);
        }
    }

    public static class DependencyUnavailableException
            extends RuntimeException {

        public DependencyUnavailableException(String message) {
            super(message);
        }
    }
}