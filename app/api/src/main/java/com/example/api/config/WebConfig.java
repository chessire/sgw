package com.example.api.config;

import com.example.common.web.config.WebMvcConfig;
import com.example.common.web.filter.LoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Web Application Context 설정
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
    "com.example.common.web",
    "com.example.api.controller"
})
@Import(WebMvcConfig.class)
public class WebConfig {

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }

    /**
     * Swagger 설정 (개발 환경에서만 활성화)
     */
    @Configuration
    @EnableSwagger2
    @Profile("development")
    public static class SwaggerConfig {
        
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.basePackage("com.example.api.controller"))
                    .paths(PathSelectors.any())
                    .build()
                    .apiInfo(apiInfo());
        }

        private ApiInfo apiInfo() {
            return new ApiInfoBuilder()
                    .title("Backend API Documentation")
                    .description("REST API 명세서")
                    .version("1.0.0")
                    .build();
        }
    }
}

