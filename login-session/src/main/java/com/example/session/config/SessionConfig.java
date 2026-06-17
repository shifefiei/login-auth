package com.example.session.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.util.StringUtils;

/**
 * Spring Session + Redis 配置。
 * 不使用 @EnableRedisHttpSession 注解，改由 Spring Boot 自动配置接管，
 * 这样 application.yml 中的 spring.session.redis.namespace、spring.session.timeout 等属性才会生效。
 */
@Configuration
public class SessionConfig {

    /**
     * 是否启用 Secure Cookie：
     * HTTPS 环境设 true，本地 HTTP 开发必须设 false。否则浏览器不会回传 Cookie，导致登录态丢失。
     */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * SameSite 策略：控制"跨站请求"是否携带 Cookie，用于防 CSRF。
     * - Lax：同站（含子域名）共享，普通场景，安全与体验的平衡点；
     * - None：前后端分离、真正跨站调用时使用，且必须配合 Secure=true（HTTPS）。
     * 同站/跨站按"主域名"判断，常见场景如下：
     * 1. 完全同域名：前端 shop.com，后端 shop.com/api —— 同站，用 Lax；
     * 2. 同主域、不同子域：前端 www.shop.com，后端 api.shop.com —— 同站，用 Lax；
     * 3. 不同主域名：前端 web.aaa.com，后端 api.bbb.com —— 跨站，用 None + Secure（HTTPS）。
     */
    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    /**
     * 子域名共享的域名匹配规则
     * 1. 单域名：shop.com 或 localhsot，留空
     * 2. 多子域名：shop.com, buyer.shop.com, seller.shop.com，配 ^.+?\.(\w+\.[a-z]+)$
     */
    @Value("${app.cookie.domain-pattern:}")
    private String domainPattern;

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION_ID");
        serializer.setCookiePath("/");
        serializer.setUseSecureCookie(cookieSecure);
        // 防止 JS 获取 Cookie，抵御 XSS 攻击
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite(sameSite);
        if (StringUtils.hasText(domainPattern)) {
            serializer.setDomainNamePattern(domainPattern);
        }
        return serializer;
    }

    /**
     * Session 数据的序列化方式：默认是 JDK 二进制（Redis 里看是乱码），
     * 这里换成 JSON，存进 Redis 的内容可读、便于排查。
     * Bean 名必须为 springSessionDefaultRedisSerializer，Spring Session 才会识别。
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

}
