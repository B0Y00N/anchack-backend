package com.kbait.anchack.common.security;

import com.kbait.anchack.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT Access Token 발급 및 검증을 담당한다.
 *
 * - 로그인 성공 시 AuthController에서 generateToken()을 호출해 토큰을 발급한다.
 * - 이후 모든 /api/** 요청은 JwtAuthenticationFilter가 이 클래스로 토큰을 검증한다.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * 토큰 만료 시간 (밀리초). application.properties의 jwt.expiration-ms 참고.
     */
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private volatile Key signingKey;

    /**
     * 서명 키를 최초 사용 시점에 생성한다(지연 초기화).
     * 빈 생성 시점이 아니라 실제 토큰 생성/검증 시점에 secret 값이
     * Environment에서 완전히 주입된 뒤 초기화되도록 한다.
     */
    private Key getSigningKey() {

        Key key = signingKey;

        if (key == null) {
            synchronized (this) {
                key = signingKey;

                if (key == null) {

                    if (secret == null || secret.trim().isEmpty()) {
                        throw new IllegalStateException(
                                "JWT_SECRET 환경변수가 설정되지 않았습니다."
                        );
                    }

                    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

                    // HS256은 최소 256bit(32byte) 이상의 키를 요구한다.
                    if (keyBytes.length < 32) {
                        throw new IllegalStateException(
                                "JWT_SECRET은 최소 32바이트(256bit) 이상이어야 합니다. "
                                        + "현재 길이: " + keyBytes.length + "byte"
                        );
                    }

                    key = Keys.hmacShaKeyFor(keyBytes);
                    signingKey = key;
                }
            }
        }

        return key;
    }

    /**
     * 로그인한 사용자를 기반으로 Access Token을 발급한다.
     * subject(sub)에는 DB PK(user.id)를 담아서, 이후 필터에서 바로 사용자 조회에 쓸 수 있게 한다.
     */
    public String generateToken(User user) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("provider", user.getProvider())
                .claim("providerId", user.getProviderId())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰이 유효한지(서명 위조 없음 + 만료 전) 검사한다.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            System.err.println("[JWT] 토큰 만료: " + e.getMessage());

        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("[JWT] 토큰 검증 실패: " + e.getMessage());
        }

        return false;
    }

    /**
     * 토큰에서 사용자 PK(user.id)를 추출한다.
     * validateToken()으로 유효성 확인 후 호출해야 한다.
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
