package com.cas.web.config;

import com.cas.common.web.config.SecureObjectMapperConfig;
import com.cas.common.web.filter.LoggingFilter;
import com.cas.common.web.filter.SecurityHeadersFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Web Application Context 설정
 * Spring 5.x + Thymeleaf 3.1.x
 * 
 * 보안 강화:
 * - SecureObjectMapperConfig: RCE 취약점 방어
 * - SecurityHeadersFilter: 보안 헤더 및 위험한 Content-Type 차단
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.cas.common.web",
    "com.cas.web.controller"
})
@Import(SecureObjectMapperConfig.class)
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Thymeleaf Template Resolver 설정
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false); // 개발 시 false, 프로덕션에서는 true
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    /**
     * Thymeleaf Template Engine 설정
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    /**
     * Thymeleaf View Resolver 설정
     */
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");
        viewResolver.setOrder(1);
        return viewResolver;
    }

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }
    
    /**
     * 보안 헤더 필터 (RCE 방어)
     */
    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("/images/");
    }
}

