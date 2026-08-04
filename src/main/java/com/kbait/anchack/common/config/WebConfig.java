package com.kbait.anchack.common.config;

import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.Collections;

public class WebConfig
        extends AbstractAnnotationConfigDispatcherServletInitializer {

    /**
     * Service, Repository, DataSource 등
     * 공통 애플리케이션 설정을 등록한다.
     */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{
                RootConfig.class
        };
    }

    /**
     * Controller, ViewResolver 등
     * Spring MVC 관련 설정을 등록한다.
     */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{
                ServletConfig.class
        };
    }

    /**
     * DispatcherServlet이 모든 요청을 처리하도록 설정한다.
     */
    @Override
    protected String[] getServletMappings() {
        return new String[]{
                "/"
        };
    }

    /**
     * 필터 실행 순서:
     *
     * 1. 문자 인코딩
     * 2. CORS 처리
     * 3. JWT 인증
     *
     * CORS 필터를 JWT 필터보다 먼저 실행해야
     * JWT 필터에서 401을 반환하더라도 CORS 응답 헤더가 포함된다.
     */
    @Override
    protected Filter[] getServletFilters() {
        return new Filter[]{
                createCharacterEncodingFilter(),
                createCorsFilter(),
                new JwtAuthenticationFilter()
        };
    }

    /**
     * UTF-8 인코딩 필터를 생성한다.
     */
    private CharacterEncodingFilter createCharacterEncodingFilter() {
        CharacterEncodingFilter filter =
                new CharacterEncodingFilter();

        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        return filter;
    }

    /**
     * 프론트엔드의 API 요청을 허용하는 CORS 필터를 생성한다.
     */
    private CorsFilter createCorsFilter() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        // Vue 개발 서버 주소
        configuration.setAllowedOrigins(
                Collections.singletonList("http://localhost:5173")
        );

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(
                Arrays.asList(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Authorization, Content-Type 등 모든 요청 헤더 허용
        configuration.setAllowedHeaders(
                Collections.singletonList("*")
        );

        // 프론트에서 접근할 수 있는 응답 헤더
        configuration.setExposedHeaders(
                Collections.singletonList("Authorization")
        );

        // 쿠키 및 인증 정보 전송 허용
        configuration.setAllowCredentials(true);

        // Preflight 요청 결과 캐시 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // 모든 요청에 CORS 설정 적용
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return new CorsFilter(source);
    }

    /**
     * 파일 업로드 설정.
     */
    @Override
    protected void customizeRegistration(
            ServletRegistration.Dynamic registration
    ) {
        MultipartConfigElement multipartConfig =
                new MultipartConfigElement(
                        null,
                        10 * 1024 * 1024,
                        20 * 1024 * 1024,
                        0
                );

        registration.setMultipartConfig(multipartConfig);
    }
}