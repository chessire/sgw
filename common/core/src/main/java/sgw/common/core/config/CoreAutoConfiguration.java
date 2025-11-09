package sgw.common.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 공통 컴포넌트 스캔과 핵심 유틸리티 빈을 제공하는 루트 구성 클래스입니다.
 */
@Configuration
@ComponentScan(basePackages = "sgw.common.core")
public class CoreAutoConfiguration {
}

