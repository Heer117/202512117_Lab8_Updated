package com.example.student_api.controller;

import com.example.student_api.model.Product;
import com.example.student_api.service.ProductService;
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
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            @Valid @RequestBody Product product,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return validationErrorResponse(bindingResult);
        }

        Product createdProduct = productService.createProduct(product);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProduct);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {

        Optional<Product> product = productService.getProductById(id);

        if (product.isPresent()) {
            return ResponseEntity.ok(product.get());
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Product not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody Product product,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return validationErrorResponse(bindingResult);
        }

        Optional<Product> updatedProduct =
                productService.updateProduct(id, product);

        if (updatedProduct.isPresent()) {
            return ResponseEntity.ok(updatedProduct.get());
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Product not found"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {

        if (productService.deleteProduct(id)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Product not found"));
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }
}