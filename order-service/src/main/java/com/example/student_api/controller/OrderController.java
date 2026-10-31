package com.example.student_api.controller;

import com.example.student_api.model.Order;
import com.example.student_api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody Order order,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return validationErrorResponse(bindingResult);
        }

        try {
            Order createdOrder = orderService.createOrder(order);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createdOrder);

        } catch (OrderService.DependencyNotFoundException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));

        } catch (OrderService.DependencyUnavailableException e) {

            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer id) {

        Optional<Order> order = orderService.getOrderById(id);

        if (order.isPresent()) {
            return ResponseEntity.ok(order.get());
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Order not found"));
    }

    private ResponseEntity<?> validationErrorResponse(
            BindingResult bindingResult) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Validation failed",
                        "details", errors
                ));
    }
}