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

    public static final String USER_ID_ATTRIBUTE =
        "AUTH_USER_ID";

    // JWT 인증 없이 접근할 수 있는 경로
    private static final List<String> WHITELIST =
        Arrays.asList(
            "/",
            "/api/health",
            "/api/auth/kakao/callback"
        );

    private JwtTokenProvider jwtTokenProvider;

    @Override
    public void init(
        FilterConfig filterConfig
    ) throws ServletException {

        // Spring이 관리하는 JwtTokenProvider Bean을 가져온다.
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

        // CORS 사전 요청은 JWT 인증 없이 통과시킨다.
        // CORS 응답 헤더는 WebConfig의 CorsFilter가 처리한다.
        if ("OPTIONS".equalsIgnoreCase(
            request.getMethod()
        )) {
            chain.doFilter(request, response);
            return;
        }

        String requestPath =
            getRequestPath(request);

        // 공개 경로는 JWT 인증 없이 Controller로 전달한다.
        if (isWhitelisted(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        String authorizationHeader =
            request.getHeader("Authorization");

        // Authorization 헤더가 없거나 Bearer 형식이 아니면
        // 인증 실패 응답을 반환한다.
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
            // JWT가 유효한지 검사한다.
            if (!jwtTokenProvider.validateToken(token)) {
                sendUnauthorizedResponse(
                    response,
                    "유효하지 않은 인증 토큰입니다."
                );
                return;
            }

            // JWT에서 사용자 PK를 가져온다.
            userId =
                jwtTokenProvider.getUserId(token);

        } catch (Exception e) {
            sendUnauthorizedResponse(
                response,
                "만료되었거나 유효하지 않은 인증 토큰입니다."
            );
            return;
        }

        // Controller에서 사용할 수 있도록
        // 인증된 사용자 PK를 request에 저장한다.
        request.setAttribute(
            USER_ID_ATTRIBUTE,
            userId
        );

        chain.doFilter(request, response);
    }

    // Context Path를 제외한 실제 요청 경로를 반환한다.
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

    // JWT 인증 없이 접근할 수 있는 경로인지 확인한다.
    private boolean isWhitelisted(
        String requestPath
    ) {

        return WHITELIST.contains(requestPath);
    }

    // JWT 인증 실패 응답을 반환한다.
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
