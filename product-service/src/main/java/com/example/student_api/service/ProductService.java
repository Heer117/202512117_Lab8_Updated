package com.example.student_api.service;

import com.example.student_api.model.Product;
import com.example.student_api.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {

        if (productRepository.existsByName(product.getName())) {
            throw new IllegalArgumentException("Product name already exists");
        }

        Product highestIdProduct = productRepository.findTopByOrderByIdDesc();

        int nextId = (highestIdProduct == null)
                ? 1
                : highestIdProduct.getId() + 1;

        product.setId(nextId);

        return productRepository.save(product);
    }

    public Optional<Product> updateProduct(Integer id, Product product) {

        return productRepository.findById(id).map(existingProduct -> {

            existingProduct.setName(product.getName());
            existingProduct.setPrice(product.getPrice());

            return productRepository.save(existingProduct);
        });
    }

    public boolean deleteProduct(Integer id) {

        if (!productRepository.existsById(id)) {
            return false;
        }

        productRepository.deleteById(id);
        return true;
    }
}