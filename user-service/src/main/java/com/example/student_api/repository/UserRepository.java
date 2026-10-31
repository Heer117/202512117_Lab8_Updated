package com.example.student_api.repository;

import com.example.student_api.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, Integer> {

    User findTopByOrderByIdDesc();

    boolean existsByEmail(String email);
}