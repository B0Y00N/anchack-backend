package com.kbait.anchack.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private Method isWhitelisted;

    @BeforeEach
    void setUp() throws Exception {
        filter = new JwtAuthenticationFilter();

        isWhitelisted =
            JwtAuthenticationFilter.class.getDeclaredMethod(
                "isWhitelisted",
                String.class,
                String.class
            );

        isWhitelisted.setAccessible(true);
    }

    @Test
    void 공개조회경로는Get만허용한다() throws Exception {
        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/admin-dongs",
                "GET"
            )
        ).isEqualTo(true);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/admin-dongs/11680",
                "GET"
            )
        ).isEqualTo(true);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews",
                "GET"
            )
        ).isEqualTo(true);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews/123",
                "GET"
            )
        ).isEqualTo(true);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/review-categories",
                "GET"
            )
        ).isEqualTo(true);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/admin-dongs",
                "POST"
            )
        ).isEqualTo(false);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews",
                "POST"
            )
        ).isEqualTo(false);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews/123",
                "PUT"
            )
        ).isEqualTo(false);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews/123",
                "PATCH"
            )
        ).isEqualTo(false);

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews/123",
                "DELETE"
            )
        ).isEqualTo(false);
    }

    @Test
    void 리뷰Me는리뷰상세공개패턴에포함되지않는다()
        throws Exception {

        assertThat(
            isWhitelisted.invoke(
                filter,
                "/api/reviews/me",
                "GET"
            )
        ).isEqualTo(false);
    }

    @Test
    void 공개되지않은조회경로는인증없이401을반환한다()
        throws Exception {

        HttpServletRequest request =
            Mockito.mock(HttpServletRequest.class);

        HttpServletResponse response =
            Mockito.mock(HttpServletResponse.class);

        FilterChain chain =
            Mockito.mock(FilterChain.class);

        StringWriter stringWriter = new StringWriter();

        when(response.getWriter())
            .thenReturn(new PrintWriter(stringWriter));

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI())
            .thenReturn("/api/reviews/me");
        when(request.getContextPath()).thenReturn("");

        filter.doFilter(request, response, chain);

        verify(response)
            .setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        verifyNoInteractions(chain);
    }

    @Test
    void 공개조회에유효한경로는인증없이통과한다()
        throws Exception {

        HttpServletRequest request =
            Mockito.mock(HttpServletRequest.class);

        HttpServletResponse response =
            Mockito.mock(HttpServletResponse.class);

        FilterChain chain =
            Mockito.mock(FilterChain.class);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI())
            .thenReturn("/api/admin-dongs/11680");
        when(request.getContextPath()).thenReturn("");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
