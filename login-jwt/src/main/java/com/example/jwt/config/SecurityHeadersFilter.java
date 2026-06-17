package com.example.jwt.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器。
 * JWT 模式下 Token 存于前端（localStorage）并通过 Authorization 头手动携带、不写入 Cookie，
 * 浏览器不会自动带上，天然规避 CSRF；因此本模块的安全重点是防 XSS（XSS 可窃取 localStorage 中的 Token）。
 * 这里统一补充一组安全响应头来降低 XSS / 点击劫持风险。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        // CSP：限制脚本/资源来源，是防 XSS 的核心。演示页用到 jsdelivr CDN 和内联脚本，
        // 故放行了 cdn.jsdelivr.net 与 'unsafe-inline'；生产应自托管脚本、去掉 'unsafe-inline'，改用 nonce/hash。
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                        + "script-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; "
                        + "connect-src 'self'");
        // 禁止浏览器 MIME 类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 禁止被 iframe 嵌套，防点击劫持
        response.setHeader("X-Frame-Options", "DENY");
        // 控制 Referer 泄露
        response.setHeader("Referrer-Policy", "no-referrer");
        chain.doFilter(req, res);
    }
}
