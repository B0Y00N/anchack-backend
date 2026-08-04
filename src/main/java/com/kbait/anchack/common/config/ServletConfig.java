package com.kbait.anchack.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS는 WebConfig의 CorsFilter에서 전역으로 한 번만 처리한다.
 * 여기(WebMvcConfigurer.addCorsMappings)나 컨트롤러의 @CrossOrigin과 중복 등록하면
 * 응답 헤더에 Access-Control-Allow-Origin이 두 번 들어가 브라우저에서
 * CORS 오류가 발생할 수 있으므로 추가하지 않는다.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "com.kbait.anchack.auth.controller",
        "com.kbait.anchack.user.controller",
        "com.kbait.anchack.common.exception"
})
public class ServletConfig implements WebMvcConfigurer {
}