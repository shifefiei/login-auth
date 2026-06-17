package com.example.jwt.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.jwt.entity.User;
import com.example.jwt.repository.UserRepository;
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

    /**
     * 注册：密码 BCrypt 加密后存库。
     */
    public User register(String username, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.insert(user);
        return user;
    }

    /**
     * 认证：校验用户名密码，成功返回 User，失败返回 empty。
     */
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
