package com.kbait.anchack.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Spring MVC 설정 클래스
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "com.kbait.anchack.common.*",
        "com.kbait.anchack.*.controller"
})
public class ServletConfig implements WebMvcConfigurer {
}
