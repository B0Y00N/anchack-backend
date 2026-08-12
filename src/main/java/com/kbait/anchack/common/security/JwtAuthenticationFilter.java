package com.kbait.anchack.common.security;

import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class JwtAuthenticationFilter implements Filter {

    public static final String USER_ID_ATTRIBUTE = "AUTH_USER_ID";

    // 정확히 일치해야 하는 공개 경로 (메서드 무관)
    private static final List<String> WHITELIST =
        Arrays.asList(
            "/",
            "/api/health",
            "/api/auth/kakao/callback"
        );

    // GET으로만 공개되는 리뷰 상세 경로: /api/reviews/{숫자 reviewId}
    // 주의: /api/reviews/me 는 이 패턴에 걸리지 않으므로 인증이 계속 필요하다.
    private static final Pattern REVIEW_DETAIL_PATH =
        Pattern.compile("^/api/reviews/\\d+$");

    private JwtTokenProvider jwtTokenProvider;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        this.jwtTokenProvider =
            WebApplicationContextUtils
                .getRequiredWebApplicationContext(
                    filterConfig.getServletContext()
                )
                .getBean(JwtTokenProvider.class);
    }

    @Override
    public void doFilter(
        ServletRequest req,
        ServletResponse res,
        FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request =
            (HttpServletRequest) req;

        HttpServletResponse response =
            (HttpServletResponse) res;

        // CORS 사전 요청은 인증 없이 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Context Path를 제외한 요청 경로
        String requestPath = getRequestPath(request);

        // 공개 API는 인증 없이 통과
        //
        // [수정] 좋아요/싫어요 기능을 위해, 공개 리뷰 조회(GET /api/reviews,
        // GET /api/reviews/{id})에서도 로그인한 사용자라면 "내가 이 리뷰에
        // 좋아요/싫어요를 눌렀는지" 알아야 한다. 토큰이 없어도 여전히 조회는
        // 되도록 하되(로그인 필수 아님), Authorization 헤더에 유효한 토큰이
        // 있으면 선택적으로 사용자 ID를 채워준다.
        if (isWhitelisted(requestPath, request.getMethod())) {
            trySetOptionalAuthenticatedUser(request);
            chain.doFilter(request, response);
            return;
        }

        String authorizationHeader =
            request.getHeader("Authorization");

        if (authorizationHeader == null
            || !authorizationHeader.startsWith("Bearer ")) {

            sendUnauthorizedResponse(
                response,
                "인증 토큰이 없습니다."
            );
            return;
        }

        String token =
            authorizationHeader
                .substring(7)
                .trim();

        if (token.isEmpty()) {
            sendUnauthorizedResponse(
                response,
                "인증 토큰이 없습니다."
            );
            return;
        }

        Long userId;

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                sendUnauthorizedResponse(
                    response,
                    "유효하지 않은 인증 토큰입니다."
                );
                return;
            }

            userId = jwtTokenProvider.getUserId(token);

        } catch (Exception e) {
            sendUnauthorizedResponse(
                response,
                "만료되었거나 유효하지 않은 인증 토큰입니다."
            );
            return;
        }

        request.setAttribute(
            USER_ID_ATTRIBUTE,
            userId
        );

        chain.doFilter(request, response);
    }

    /**
     * 공개(whitelist) 경로에서, Authorization 헤더에 유효한 Bearer 토큰이
     * 있으면 사용자 ID를 request 속성에 채워준다. 토큰이 없거나 유효하지
     * 않아도 예외를 던지지 않고 조용히 넘어간다(로그인은 선택 사항이므로).
     */
    private void trySetOptionalAuthenticatedUser(
        HttpServletRequest request
    ) {
        String authorizationHeader =
            request.getHeader("Authorization");

        if (authorizationHeader == null
            || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token =
            authorizationHeader
                .substring(7)
                .trim();

        if (token.isEmpty()) {
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                return;
            }

            Long userId = jwtTokenProvider.getUserId(token);

            request.setAttribute(
                USER_ID_ATTRIBUTE,
                userId
            );
        } catch (Exception e) {
            // 선택적 인증이므로 토큰이 잘못돼도 무시하고 비로그인으로 처리한다.
        }
    }

    /**
     * Context Path를 제외한 실제 API 경로를 반환한다.
     *
     * 예:
     * /kakao-login-backend/api/admin-dongs
     * → /api/admin-dongs
     */
    private String getRequestPath(
        HttpServletRequest request
    ) {

        String requestUri =
            request.getRequestURI();

        String contextPath =
            request.getContextPath();

        if (contextPath != null
            && !contextPath.isEmpty()
            && requestUri.startsWith(contextPath)) {

            return requestUri.substring(
                contextPath.length()
            );
        }

        return requestUri;
    }

    /**
     * JWT 인증 없이 접근할 수 있는 경로인지 확인한다.
     *
     * [수정] ReviewController.getReviewsByAdminDong()/getReview()는
     * "공개 리뷰 조회"로 설계되었는데도 이 whitelist에 빠져 있어서
     * 비로그인 사용자가 동네 리뷰를 조회하려 하면 401이 발생하던 문제를 고쳤다.
     * GET /api/reviews, GET /api/reviews/{숫자 id}만 공개하고,
     * GET /api/reviews/me 및 POST/PUT/DELETE /api/reviews**는 계속 인증을 요구한다.
     */
    private boolean isWhitelisted(
        String requestPath,
        String method
    ) {

        // 정확히 일치하는 공개 경로
        if (WHITELIST.contains(requestPath)) {
            return true;
        }

        // 행정동 API와 그 하위 경로 공개
        if (requestPath.equals("/api/admin-dongs")
            || requestPath.startsWith("/api/admin-dongs/")) {
            return true;
        }

        // 리뷰 목록/상세 "조회(GET)"만 공개. /api/reviews/me는 제외된다.
        if ("GET".equalsIgnoreCase(method)) {
            if (requestPath.equals("/api/reviews")) {
                return true;
            }

            if (REVIEW_DETAIL_PATH.matcher(requestPath).matches()) {
                return true;
            }
        }

        return false;
    }

    private void sendUnauthorizedResponse(
        HttpServletResponse response,
        String message
    ) throws IOException {

        response.setStatus(
            HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setCharacterEncoding("UTF-8");

        response.setContentType(
            "application/json;charset=UTF-8"
        );

        String escapedMessage =
            message.replace("\"", "\\\"");

        response.getWriter().write(
            "{\"message\":\""
                + escapedMessage
                + "\"}"
        );
    }

    @Override
    public void destroy() {
    }
}
