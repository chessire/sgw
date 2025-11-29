package com.cas.common.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Arrays;
import java.util.List;

/**
 * Web MVC 설정
 * 보안 강화: RCE 취약점 방어를 위한 안전한 Jackson Converter 사용
 */
@Configuration
@EnableWebMvc
@SuppressWarnings({"deprecation", "null"})
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private ObjectMapper secureObjectMapper;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * 보안 강화된 Jackson Message Converter 설정
     * 
     * 보안 조치:
     * 1. SecureObjectMapperConfig의 보안 강화된 ObjectMapper 사용
     * 2. application/json만 허용 (Java Serialization 차단)
     * 3. Polymorphic Deserialization 화이트리스트 적용
     */
    @Override
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        
        // 보안 강화된 ObjectMapper 사용
        converter.setObjectMapper(secureObjectMapper);
        
        // Content-Type 제한: application/json만 허용
        // (Java Serialization 등 위험한 Content-Type 차단)
        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON
        ));
        
        converters.add(converter);
        super.configureMessageConverters(converters);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/");
        
        // SpringDoc OpenAPI UI 리소스
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");
        
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}

