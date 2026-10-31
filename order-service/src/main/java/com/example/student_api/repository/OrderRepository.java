package com.example.student_api.repository;

import com.example.student_api.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, Integer> {

    Order findTopByOrderByIdDesc();
}