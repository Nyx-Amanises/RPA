package com.rpa.manage.security;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录会话服务。
 *
 * <p>虽然项目使用了 JWT，但这里仍然额外把 token 记录到 Redis，
 * 相当于在“无状态 token”外面再套一层可控会话。
 *
 * <p>这样做的好处是：
 * 1. 可以判断 token 是否仍然有效；
 * 2. 可以在请求活跃时刷新过期时间；
 * 3. 后续如果要做强制下线，也更容易扩展。
 */
@Component
@RequiredArgsConstructor
public class LoginSessionService {

    /**
     * Redis 中登录 token 的统一前缀。
     */
    private static final String TOKEN_PREFIX = "rpa:login:token:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 会话有效期，和 JWT 过期时间保持一致。
     */
    @Value("${app.jwt.expiration-millis}")
    private long expirationMillis;

    /**
     * 保存登录会话。
     *
     * @param token 登录成功后生成的 token
     * @param loginUser 登录用户对象
     */
    public void save(String token, LoginUser loginUser) {
        stringRedisTemplate.opsForValue().set(
                buildKey(token),
                String.valueOf(loginUser.getUserId()),
                Duration.ofMillis(expirationMillis)
        );
    }

    /**
     * 判断某个 token 是否仍然存在于会话存储中。
     *
     * @param token 待检查的 token
     * @return true 表示仍然有效
     */
    public boolean exists(String token) {
        Boolean exists = stringRedisTemplate.hasKey(buildKey(token));
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 刷新 token 对应会话的过期时间。
     *
     * @param token 当前请求携带的 token
     */
    public void refresh(String token) {
        stringRedisTemplate.expire(buildKey(token), Duration.ofMillis(expirationMillis));
    }

    /**
     * 拼出 Redis 中的完整 key。
     *
     * @param token token 值
     * @return Redis key
     */
    private String buildKey(String token) {
        return TOKEN_PREFIX + token;
    }
}
