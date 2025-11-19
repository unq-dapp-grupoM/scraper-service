package com.dapp.scraper_service.service;

import com.dapp.scraper_service.model.User;
import com.dapp.scraper_service.model.Role;
import com.dapp.scraper_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User registerUser(String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User already exists with email: " + email);
        }

        User user = User.builder()
                .email(email)
                .password(password)
                .role(role)
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean validateUser(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> user.getPassword().equals(password)) // Comparación directa
                .orElse(false);
    }
}