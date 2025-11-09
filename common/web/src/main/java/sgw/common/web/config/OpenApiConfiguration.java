package sgw.common.web.config;

import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc 기반 OpenAPI 설정은 Spring Boot 3 전용이므로, 전자정부프레임워크 3.8 환경에서는 비활성화합니다.
 */
@Configuration
public class OpenApiConfiguration {
    // TODO: 필요 시 Swagger 2.x 또는 Spring 4 호환 문서화 도구로 대체
}

