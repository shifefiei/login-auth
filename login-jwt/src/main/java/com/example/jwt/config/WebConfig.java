package com.example.jwt.config;

import com.example.jwt.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Objects;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * CORS 允许的前端 Origin（跨源白名单，逗号分隔）。
     * CORS 控制的是"浏览器允不允许 JS 发起跨源请求并读取响应"，按 Origin（协议 + 域名 + 端口）是否完全相同判断。
     * 是否需要配置，常见场景如下：
     * 1. 完全同源：前端 https://shop.com，后端 https://shop.com/api —— 同源，留空（不触发 CORS）；
     * 2. 端口不同：前端 http://localhost:3000，后端 http://localhost:8081 —— 跨源，需配 http://localhost:3000；
     * 3. 协议不同：前端 https://shop.com，后端 http://shop.com —— 跨源，需配 https://shop.com；
     * 4. 域名不同：前端 https://web.aaa.com，后端 https://api.bbb.com —— 跨源，需配 https://web.aaa.com。
     * 本模块是 JWT(Authorization 头)认证、不带 Cookie，故无需 allowCredentials，用 allowedHeaders("*") 放行 Authorization 头即可。
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    public WebConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(Objects.requireNonNull(jwtAuthInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout"
                );
    }

    /**
     * CORS 配置：仅当配置了 allowed-origins 时才开启（同源部署无需）。
     * JWT 用 Authorization 头传 Token、不依赖 Cookie，故无需 allowCredentials；
     * 用 allowedHeaders("*") 放行 Authorization 等自定义头。
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (!StringUtils.hasText(allowedOrigins)) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(splitTrim(allowedOrigins))
                .allowedMethods(splitTrim(allowedMethods))
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @NonNull
    private String[] splitTrim(String value) {
        String[] parts = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return Objects.requireNonNull(parts);
    }
}
