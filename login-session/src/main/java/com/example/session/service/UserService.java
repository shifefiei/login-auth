package com.example.session.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.session.entity.User;
import com.example.session.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean existsByUsername(String username) {
        return userRepository.exists(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    public User register(String username, String rawPassword, String phone, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.insert(user);
        return user;
    }

    public Optional<User> authenticate(String username, String rawPassword) {
        User user = userRepository.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userRepository.selectById(id));
    }
}
