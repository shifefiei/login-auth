package com.example.jwt.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 认证相关请求/响应 DTO 集合。
 */
public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresIn
    ) {
    }
}
