package sgw.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 분산 추적 기본 설정. 외부 구현이 없을 경우 빈 껍데기 트레이서를 제공한다.
 */
@Configuration
public class TracingConfiguration {

    public interface SimpleTracer {
        void record(String message);
    }

    private static final SimpleTracer NOOP_TRACER = new SimpleTracer() {
        @Override
        public void record(String message) {
            // no-op
        }
    };

    @Bean
    public SimpleTracer tracer() {
        return NOOP_TRACER;
    }
}

