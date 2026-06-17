package com.example.session.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 64, message = "用户名长度必须在2到64之间") String username,
            @NotBlank @Size(min = 6, max = 72, message = "密码长度必须在6到72之间") String password,
            @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确") String phone,
            @Email(message = "邮箱格式不正确") @Size(max = 128) String email
    ) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64, message = "用户名超长") String username,
            @NotBlank @Size(max = 72, message = "密码超长") String password
    ) {
    }
}
