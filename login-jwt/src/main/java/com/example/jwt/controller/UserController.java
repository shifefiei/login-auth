package com.example.jwt.controller;

import com.example.jwt.common.Result;
import com.example.jwt.entity.User;
import com.example.jwt.interceptor.UserContext;
import com.example.jwt.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 受保护接口，必须带有效 Access Token 才能访问。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Long userId = UserContext.get();
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return Result.error(1004, "用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.get().getId());
        data.put("username", user.get().getUsername());
        data.put("createdAt", user.get().getCreatedAt());
        return Result.ok(data);
    }
}
