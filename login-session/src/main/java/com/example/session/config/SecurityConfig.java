package com.example.session.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 引入 Spring Security 仅用于 CSRF 防护（登录认证仍由 AuthInterceptor + Session 负责，故所有请求 permitAll）。
 *
 * CSRF 方案：双提交 Cookie
 * - 服务端把随机 CSRF Token 写入非 HttpOnly 的 XSRF-TOKEN cookie，前端 JS 可读；
 * - 前端每次写操作（POST/PUT/DELETE）把该 token 放进 X-XSRF-TOKEN 请求头回传；
 * - 服务端校验 cookie 与请求头中的 token 是否一致，攻击者的跨站页面因同源策略读不到 cookie，伪造请求被拒。
 * - 与 SameSite Cookie（见 SessionConfig）形成双重防护：XSRF-TOKEN 的 SameSite/Secure 跟随 app.cookie 配置保持一致。
 */
@Configuration
public class SecurityConfig {

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // XSRF-TOKEN cookie 的 SameSite / Secure 与会话 Cookie 保持一致，确保跨站场景也能正确携带
        tokenRepository.setCookieCustomizer(cookie -> cookie.sameSite(sameSite).secure(cookieSecure).path("/"));

        // SPA 用法：前端发送 cookie 中的原始 token 值，使用普通 Handler（非 XOR）按原值比对
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(tokenRepository)
                        .csrfTokenRequestHandler(requestHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        // 强制每个请求生成并下发 XSRF-TOKEN cookie（规避 Security 6 延迟加载导致首个请求拿不到 token）
        http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 读取一次 CsrfToken，触发 CookieCsrfTokenRepository 把 XSRF-TOKEN cookie 写入响应。
     */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                        @NonNull FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
