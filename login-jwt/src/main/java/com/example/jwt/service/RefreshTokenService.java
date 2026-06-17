package com.example.jwt.service;

import com.example.jwt.config.JwtProperties;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh Token 存 Redis：key = refresh:{token}，value = userId。
 * 登出时删除 key，即可让后续刷新失败，实现可控注销。
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";
    private static final RedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]); "
                    + "if v then redis.call('DEL', KEYS[1]); end; "
                    + "return v",
            String.class
    );

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
        String userIdValue = String.valueOf(Objects.requireNonNull(userId));
        Duration ttl = Duration.ofMillis(properties.getRefreshTokenTtl());
        redisTemplate.opsForValue().set(
                Objects.requireNonNull(KEY_PREFIX + token),
                Objects.requireNonNull(userIdValue),
                Objects.requireNonNull(ttl)
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
     * 原子消费 Refresh Token：存在则读取 userId 并删除，不存在返回 null。
     * 用于 Refresh Token Rotation，避免并发刷新同时拿到同一个旧 token。
     */
    public Long consume(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String userId = redisTemplate.execute(
                Objects.requireNonNull(CONSUME_SCRIPT),
                Objects.requireNonNull(List.of(KEY_PREFIX + token))
        );
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
