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

    private static final List<String> WHITELIST =
        Arrays.asList(
            "/",
            "/api/health",
            "/api/auth/kakao/callback"
        );

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

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String requestPath = getRequestPath(request);

        if (isWhitelisted(requestPath, request.getMethod())) {
            trySetOptionalAuthenticatedUser(request);
            chain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null
            || !authorizationHeader.startsWith("Bearer ")) {

            sendUnauthorizedResponse(
                response,
                "인증 토큰이 없습니다."
            );
            return;
        }

        String token = authorizationHeader.substring(7).trim();

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

        request.setAttribute(USER_ID_ATTRIBUTE, userId);

        chain.doFilter(request, response);
    }

    private void trySetOptionalAuthenticatedUser(
        HttpServletRequest request
    ) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null
            || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            return;
        }

        try {
            if (jwtTokenProvider.validateToken(token)) {
                request.setAttribute(
                    USER_ID_ATTRIBUTE,
                    jwtTokenProvider.getUserId(token)
                );
            }
        } catch (Exception ignored) {
            // 공개 API이므로 잘못된 토큰은 비로그인 사용자로 처리한다.
        }
    }

    private String getRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null
            && !contextPath.isEmpty()
            && requestUri.startsWith(contextPath)) {

            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    private boolean isWhitelisted(
        String requestPath,
        String method
    ) {
        if (WHITELIST.contains(requestPath)) {
            return true;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }

        if (requestPath.equals("/api/admin-dongs")
            || requestPath.startsWith("/api/admin-dongs/")) {
            return true;
        }

        return requestPath.equals("/api/reviews")
            || REVIEW_DETAIL_PATH.matcher(requestPath).matches()
            || requestPath.equals("/api/review-categories");
    }

    private void sendUnauthorizedResponse(
        HttpServletResponse response,
        String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String escapedMessage = message.replace("\"", "\\\"");

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
