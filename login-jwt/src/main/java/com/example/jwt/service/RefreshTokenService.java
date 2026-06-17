package com.example.jwt.service;

import com.example.jwt.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Refresh Token 存 Redis：key = refresh:{token}，value = userId。
 * 登出时删除 key，即可让后续刷新失败，实现可控注销。
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties properties;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 生成 Refresh Token 并写入 Redis，返回 token 字符串。
     */
    public String create(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                String.valueOf(userId),
                Duration.ofMillis(properties.getRefreshTokenTtl())
        );
        return token;
    }

    /**
     * 校验 Refresh Token，返回对应 userId；不存在返回 null。
     */
    public Long resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        return userId == null ? null : Long.valueOf(userId);
    }

    /**
     * 删除 Refresh Token（登出 / 轮换旧 token）。
     */
    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(KEY_PREFIX + token);
        }
    }
}
