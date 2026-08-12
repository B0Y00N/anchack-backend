package com.kbait.anchack.common.security;

import com.kbait.anchack.common.exception.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;

/**
 * JwtAuthenticationFilter가 request attribute에 저장한 사용자 ID를 꺼내는
 * 공통 로직을 제공한다.
 *
 * Auth, User, Review 등 여러 Controller가 동일한 방식으로 사용자 ID를
 * 조회해야 해서 common.security에 둔다.
 */
public final class AuthenticatedUserResolver {

    private AuthenticatedUserResolver() {
    }

    /**
     * 로그인한 사용자 ID를 조회한다. 로그인하지 않았으면 예외를 던진다.
     */
    public static Long requireUserId(HttpServletRequest request) {
        Long userId = resolveUserId(request);

        if (userId == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return userId;
    }

    /**
     * 로그인한 사용자 ID를 조회한다. 로그인하지 않았으면 null을 반환한다.
     *
     * 비로그인 사용자도 접근 가능한 공개 API에서, 로그인했다면 부가 정보를
     * 채워주기 위해 사용한다(예: 리뷰 목록의 내 반응 표시).
     */
    public static Long resolveUserId(HttpServletRequest request) {
        Object userIdAttribute =
            request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (userIdAttribute instanceof Long) {
            return (Long) userIdAttribute;
        }

        if (userIdAttribute instanceof Number) {
            return ((Number) userIdAttribute).longValue();
        }

        return null;
    }
}
