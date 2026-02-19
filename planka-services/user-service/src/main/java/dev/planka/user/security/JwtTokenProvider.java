package dev.planka.user.security;

import dev.planka.user.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 提供者
 * <p>
 * 本系统使用双 Token 机制进行身份认证：
 * <p>
 * <b>Access Token（访问令牌）</b>
 * <ul>
 *   <li>用途：访问 API 资源的凭证，每次请求都需要携带</li>
 *   <li>有效期：较短（默认 2 小时），减少泄露风险</li>
 *   <li>存储位置：仅存储在前端（localStorage），后端不保存</li>
 *   <li>携带信息：用户基本信息（userId、email）+ 组织上下文（orgId、memberCardId、role）</li>
 *   <li>验证方式：网关通过签名验证，无需查询数据库</li>
 * </ul>
 * <p>
 * <b>Refresh Token（刷新令牌）</b>
 * <ul>
 *   <li>用途：当 Access Token 过期时，用于获取新的 Access Token，避免用户重新登录</li>
 *   <li>有效期：较长（默认 7 天），提供更好的用户体验</li>
 *   <li>存储位置：前端（localStorage）+ 后端数据库（sys_refresh_token 表，存储哈希值）</li>
 *   <li>携带信息：仅包含 userId，组织上下文存储在数据库中</li>
 *   <li>验证方式：需要查询数据库验证哈希值，可以主动撤销</li>
 * </ul>
 * <p>
 * <b>为什么需要两个 Token？</b>
 * <ul>
 *   <li>Access Token 短期有效，即使泄露影响也有限</li>
 *   <li>Refresh Token 长期有效但仅用于刷新，降低暴露风险</li>
 *   <li>Refresh Token 存储在数据库，支持主动撤销（如登出、切换组织）</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 生成 Access Token（不含组织信息，用于登录）
     */
    public String generateAccessToken(UserEntity user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);

        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .claim("superAdmin", user.isSuperAdmin())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成带组织信息的 Access Token（用于切换组织）
     *
     * @param user         用户实体
     * @param orgId        组织ID
     * @param memberCardId 成员卡片ID
     * @param role         用户在组织中的角色
     * @return JWT Token
     */
    public String generateAccessTokenWithOrg(UserEntity user, String orgId, String memberCardId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration * 1000);

        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .claim("superAdmin", user.isSuperAdmin())
                .claim("orgId", orgId)
                .claim("memberCardId", memberCardId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 Refresh Token（返回原始token，需要存储哈希值）
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration * 1000);

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并验证 Token
     */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户ID
     */
    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 检查 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * 获取 Access Token 过期时间（秒）
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 获取 Refresh Token 过期时间（秒）
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
