package com.kbait.anchack.common.security;

import com.kbait.anchack.auth.domain.AuthUser;
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
 * - 이후 /api/** 요청은 JwtAuthenticationFilter가 이 클래스로 토큰을 검증한다.
 */
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * 토큰 만료 시간(밀리초).
     * application.properties의 jwt.expiration-ms 값을 사용한다.
     */
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private volatile Key signingKey;

    /**
     * 서명 키를 최초 사용 시점에 생성한다.
     */
    private Key getSigningKey() {

        Key key = signingKey;

        if (key == null) {
            synchronized (this) {
                key = signingKey;

                if (key == null) {
                    validateSecret();

                    byte[] keyBytes =
                            secret.getBytes(
                                    StandardCharsets.UTF_8
                            );

                    key = Keys.hmacShaKeyFor(
                            keyBytes
                    );

                    signingKey = key;
                }
            }
        }

        return key;
    }

    /**
     * JWT Secret 값을 검증한다.
     */
    private void validateSecret() {

        if (secret == null
                || secret.trim().isEmpty()) {

            throw new IllegalStateException(
                    "JWT_SECRET 환경변수가 설정되지 않았습니다."
            );
        }

        byte[] keyBytes =
                secret.getBytes(
                        StandardCharsets.UTF_8
                );

        /*
         * HS256은 최소 256bit, 즉 32byte 이상의 키가 필요하다.
         */
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET은 최소 32바이트(256bit) 이상이어야 합니다. "
                            + "현재 길이: "
                            + keyBytes.length
                            + "byte"
            );
        }
    }

    /**
     * 로그인한 인증 사용자를 기반으로 JWT Access Token을 발급한다.
     *
     * subject에는 users 테이블의 PK인 user_id를 저장한다.
     */
    public String generateToken(
            AuthUser authUser
    ) {
        validateAuthUser(authUser);

        Date now =
                new Date();

        Date expiry =
                new Date(
                        now.getTime()
                                + expirationMs
                );

        return Jwts.builder()
                .setSubject(
                        String.valueOf(
                                authUser.getId()
                        )
                )
                .claim(
                        "provider",
                        authUser.getProvider()
                )
                .claim(
                        "providerId",
                        authUser.getProviderId()
                )
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(
                        getSigningKey(),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    /**
     * 토큰 생성에 필요한 AuthUser 필수값을 검증한다.
     */
    private void validateAuthUser(
            AuthUser authUser
    ) {
        if (authUser == null) {
            throw new IllegalArgumentException(
                    "JWT를 발급할 인증 사용자 정보가 없습니다."
            );
        }

        if (authUser.getId() == null) {
            throw new IllegalArgumentException(
                    "JWT를 발급할 사용자 ID가 없습니다."
            );
        }

        if (expirationMs <= 0) {
            throw new IllegalStateException(
                    "jwt.expiration-ms는 0보다 커야 합니다."
            );
        }
    }

    /**
     * 토큰이 유효한지 검사한다.
     *
     * 검사 항목:
     * - 서명 유효성
     * - JWT 형식
     * - 만료 여부
     */
    public boolean validateToken(
            String token
    ) {
        if (token == null
                || token.trim().isEmpty()) {

            return false;
        }

        try {
            parseClaims(
                    token.trim()
            );

            return true;

        } catch (ExpiredJwtException e) {
            System.err.println(
                    "[JWT] 토큰 만료: "
                            + e.getMessage()
            );

        } catch (JwtException
                 | IllegalArgumentException e) {

            System.err.println(
                    "[JWT] 토큰 검증 실패: "
                            + e.getMessage()
            );
        }

        return false;
    }

    /**
     * 토큰에서 사용자 PK를 추출한다.
     *
     * subject에 저장된 users.user_id 값을 Long으로 변환한다.
     */
    public Long getUserId(
            String token
    ) {
        if (token == null
                || token.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "JWT가 없습니다."
            );
        }

        Claims claims =
                parseClaims(
                        token.trim()
                );

        String subject =
                claims.getSubject();

        if (subject == null
                || subject.trim().isEmpty()) {

            throw new JwtException(
                    "JWT subject에 사용자 ID가 없습니다."
            );
        }

        try {
            return Long.valueOf(
                    subject.trim()
            );

        } catch (NumberFormatException e) {
            throw new JwtException(
                    "JWT subject의 사용자 ID 형식이 올바르지 않습니다.",
                    e
            );
        }
    }

    /**
     * JWT Claims를 파싱한다.
     */
    private Claims parseClaims(
            String token
    ) {
        return Jwts.parserBuilder()
                .setSigningKey(
                        getSigningKey()
                )
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
