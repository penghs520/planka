package dev.planka.gateway.filter;

import dev.planka.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>
 * 验证请求中的 JWT Token，并将用户信息传递给下游服务
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final SecretKey secretKey;
    private final List<String> whiteList;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.whiteList = jwtProperties.getWhiteList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 提取 Token
        String token = extractToken(request);
        if (token == null) {
            return unauthorized(exchange, "AUTH_011", "缺少认证令牌");
        }

        try {
            // 验证 Token
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            Boolean superAdmin = claims.get("superAdmin", Boolean.class);
            // 从 Token 中提取组织相关信息（切换组织后的 Token 才有）
            String orgId = claims.get("orgId", String.class);
            String memberCardId = claims.get("memberCardId", String.class);
            String role = claims.get("role", String.class);

            // 检查超管权限（/api/v1/admin/** 路径）
            if (path.startsWith("/api/v1/admin/") && (superAdmin == null || !superAdmin)) {
                return forbidden(exchange, "AUTH_012", "需要超级管理员权限");
            }

            // 将用户信息添加到请求头，传递给下游服务
            ServerHttpRequest.Builder requestBuilder = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-Super-Admin", String.valueOf(superAdmin != null && superAdmin));

            // 添加组织相关信息（如果存在）
            if (orgId != null && !orgId.isBlank()) {
                requestBuilder.header("X-Org-Id", orgId);
            }
            if (memberCardId != null && !memberCardId.isBlank()) {
                requestBuilder.header("X-Member-Card-Id", memberCardId);
            }
            if (role != null && !role.isBlank()) {
                requestBuilder.header("X-Role", role);
            }

            ServerHttpRequest modifiedRequest = requestBuilder.build();

            log.debug("JWT authentication successful for user: {}, org: {}", userId, orgId);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired");
            return unauthorized(exchange, "AUTH_013", "令牌已过期");
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return unauthorized(exchange, "AUTH_014", "无效的令牌");
        }
    }

    private boolean isWhiteListed(String path) {
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        return errorResponse(exchange, HttpStatus.UNAUTHORIZED, code, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String code, String message) {
        return errorResponse(exchange, HttpStatus.FORBIDDEN, code, message);
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"success\":false,\"code\":\"%s\",\"message\":\"%s\"}", code, message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }
}
