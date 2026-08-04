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

public class JwtAuthenticationFilter implements Filter {

    public static final String USER_ID_ATTRIBUTE = "AUTH_USER_ID";

    private static final List<String> WHITELIST = Arrays.asList(
            "/",
            "/api/auth/kakao/callback"
    );

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

        String method = request.getMethod();
        String uri = getRequestPath(request);

        /*
         * CORS 사전 요청은 토큰 검사 없이 통과
         */
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        /*
         * 로그인 콜백 등 공개 경로 통과
         */
        if (WHITELIST.contains(uri)) {
            chain.doFilter(req, res);
            return;
        }

        /*
         * /api/** 이외 요청은 JWT 검사 대상이 아님
         */
        if (!uri.startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            sendUnauthorized(
                    response,
                    "AUTH_HEADER_MISSING",
                    "Authorization 헤더가 없습니다."
            );

            return;
        }

        String token =
                authHeader.substring("Bearer ".length()).trim();

        if (token.isEmpty()) {
            sendUnauthorized(
                    response,
                    "TOKEN_MISSING",
                    "Access Token이 없습니다."
            );

            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                sendUnauthorized(
                        response,
                        "INVALID_TOKEN",
                        "유효하지 않거나 만료된 토큰입니다."
                );

                return;
            }

            Long userId =
                    jwtTokenProvider.getUserId(token);

            if (userId == null) {
                sendUnauthorized(
                        response,
                        "INVALID_TOKEN_USER",
                        "토큰의 사용자 정보가 유효하지 않습니다."
                );

                return;
            }

            request.setAttribute(
                    USER_ID_ATTRIBUTE,
                    userId
            );

            chain.doFilter(req, res);

        } catch (Exception e) {
            sendUnauthorized(
                    response,
                    "TOKEN_PROCESS_ERROR",
                    "토큰 처리 중 오류가 발생했습니다."
            );
        }
    }

    private String getRequestPath(
            HttpServletRequest request
    ) {
        String requestUri =
                request.getRequestURI();

        String contextPath =
                request.getContextPath();

        if (contextPath == null
                || contextPath.isEmpty()) {

            return requestUri;
        }

        return requestUri.substring(
                contextPath.length()
        );
    }

    private void sendUnauthorized(
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setCharacterEncoding("UTF-8");

        response.setContentType(
                "application/json;charset=UTF-8"
        );

        response.getWriter().write(
                "{"
                        + "\"code\":\"" + code + "\","
                        + "\"message\":\"" + message + "\""
                        + "}"
        );
    }
}