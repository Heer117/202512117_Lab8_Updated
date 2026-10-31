package com.example.student_api.service;

import com.example.student_api.model.User;
import com.example.student_api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User highestIdUser = userRepository.findTopByOrderByIdDesc();

        int nextId = (highestIdUser == null)
                ? 1
                : highestIdUser.getId() + 1;

        user.setId(nextId);

        return userRepository.save(user);
    }

    public Optional<User> updateUser(Integer id, User user) {

        return userRepository.findById(id).map(existingUser -> {

            existingUser.setName(user.getName());
            existingUser.setEmail(user.getEmail());

            return userRepository.save(existingUser);
        });
    }

    public boolean deleteUser(Integer id) {

        if (!userRepository.existsById(id)) {
            return false;
        }

        userRepository.deleteById(id);
        return true;
    }
}