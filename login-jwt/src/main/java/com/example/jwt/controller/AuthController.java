package com.example.jwt.controller;

import com.example.jwt.common.Result;
import com.example.jwt.config.JwtProperties;
import com.example.jwt.dto.AuthDtos.LoginRequest;
import com.example.jwt.dto.AuthDtos.RefreshRequest;
import com.example.jwt.dto.AuthDtos.RegisterRequest;
import com.example.jwt.dto.AuthDtos.TokenResponse;
import com.example.jwt.entity.User;
import com.example.jwt.service.RefreshTokenService;
import com.example.jwt.service.UserService;
import com.example.jwt.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthController(UserService userService, JwtUtil jwtUtil,
                         RefreshTokenService refreshTokenService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 注册即登录：创建用户后直接签发双 Token。
     */
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.existsByUsername(req.username())) {
            return Result.error(1001, "用户名已存在");
        }
        User user = userService.register(req.username(), req.password());
        return Result.ok(issueTokens(user));
    }

    /**
     * 登录：校验密码，成功签发双 Token。
     */
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        Optional<User> user = userService.authenticate(req.username(), req.password());
        if (user.isEmpty()) {
            return Result.error(1002, "用户名或密码错误");
        }
        return Result.ok(issueTokens(user.get()));
    }

    /**
     * 刷新：用 Refresh Token 换新的 Access Token（同时轮换 Refresh Token，防重放）。
     */
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        Long userId = refreshTokenService.consume(req.refreshToken());
        if (userId == null) {
            return Result.error(1003, "Refresh Token 无效或已过期，请重新登录");
        }
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return Result.error(1003, "用户不存在");
        }
        // Refresh Token Rotation：旧 token 已被原子消费，这里只发新的
        return Result.ok(issueTokens(user.get()));
    }

    /**
     * 登出：删除 Redis 中的 Refresh Token，Access Token 过期后会话彻底结束。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        refreshTokenService.revoke(refreshToken);
        return Result.ok();
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = refreshTokenService.create(user.getId());
        return new TokenResponse(accessToken, refreshToken, jwtProperties.getAccessTokenTtl());
    }
}
