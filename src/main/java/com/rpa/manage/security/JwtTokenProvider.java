package com.rpa.manage.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 令牌提供者。
 *
 * <p>这个类专门负责两件事：
 * 1. 根据登录用户信息生成 token；
 * 2. 在请求进来时解析 token。
 *
 * <p>它不负责校验用户是否存在、权限是否变化，这些工作由过滤器和权限服务完成。
 */
@Component
public class JwtTokenProvider {

    /**
     * 用于签名和验签 JWT 的密钥。
     */
    private final SecretKey secretKey;
    /**
     * token 有效期，单位毫秒。
     */
    private final long expirationMillis;
    /**
     * token 签发者标识。
     */
    private final String issuer;

    /**
     * 初始化 JWT 组件。
     *
     * @param secret 配置文件中的密钥字符串
     * @param expirationMillis token 有效期
     * @param issuer 签发者
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-millis}") long expirationMillis,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
        this.issuer = issuer;
    }

    /**
     * 为登录用户生成 JWT。
     *
     * <p>当前 token 中保存了用户 ID、用户名和主角色信息，
     * 这些字段足够支持后续请求完成基础身份识别。
     *
     * @param loginUser 登录用户对象
     * @return 生成好的 JWT 字符串
     */
    public String generateToken(LoginUser loginUser) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .claims(Map.of(
                        "username", loginUser.getUsername(),
                        "roleId", loginUser.getRoleId() == null ? 0L : loginUser.getRoleId(),
                        "roleCode", loginUser.getRoleCode() == null ? "" : loginUser.getRoleCode(),
                        "roleName", loginUser.getRoleName() == null ? "" : loginUser.getRoleName()
                ))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 JWT 并返回载荷。
     *
     * @param token 待解析的 token
     * @return token 中的 Claims 数据
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
