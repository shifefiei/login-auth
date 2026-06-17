package com.example.jwt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 认证相关请求/响应 DTO 集合。
 */
public class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 64, message = "用户名长度必须在2到64之间") String username,
            @NotBlank @Size(min = 6, max = 72, message = "密码长度必须在6到72之间") String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64, message = "用户名超长") String username,
            @NotBlank @Size(max = 72, message = "密码超长") String password
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
