package com.example.student_api.repository;

import com.example.student_api.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, Integer> {

    Product findTopByOrderByIdDesc();

    boolean existsByName(String name);
}