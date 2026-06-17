package com.example.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 排除 UserDetailsServiceAutoConfiguration：
 * 本模块引入 Spring Security 仅为做 CSRF 防护，登录认证由 AuthInterceptor + Session + 数据库 user 表负责，
 * 不需要 Spring Security 的默认内存用户。排除后不再创建默认 user 账号、不再打印随机密码，
 * 也不会出现 "Global AuthenticationManager configured with ... inMemoryUserDetailsManager" 日志。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LoginSessionApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoginSessionApplication.class, args);
    }
}
