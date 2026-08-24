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

public class WebConfig
    extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{
            RootConfig.class
        };
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{
            ServletConfig.class
        };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{
            "/"
        };
    }

    @Override
    protected Filter[] getServletFilters() {
        return new Filter[]{
            createCharacterEncodingFilter(),
            createCorsFilter(),
            new JwtAuthenticationFilter()
        };
    }

    private CharacterEncodingFilter createCharacterEncodingFilter() {
        CharacterEncodingFilter filter =
            new CharacterEncodingFilter();

        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        return filter;
    }

    private CorsFilter createCorsFilter() {
        CorsConfiguration configuration =
            new CorsConfiguration();

        configuration.setAllowedOrigins(
            Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
            )
        );

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

        configuration.setAllowedHeaders(
            Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
            )
        );

        configuration.setExposedHeaders(
            Arrays.asList(
                "Authorization",
                "Content-Type"
            )
        );

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return new CorsFilter(source);
    }

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
