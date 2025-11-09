package sgw.common.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sgw.common.infra.condition.ConditionalOnPropertySimple;

@Configuration
@ConditionalOnPropertySimple(prefix = "app.minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioConfiguration {

    @Value("${app.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.minio.accessKey:minioadmin}")
    private String accessKey;

    @Value("${app.minio.secretKey:minioadmin}")
    private String secretKey;

    @Bean
    public MinioSettings minioSettings() {
        return new MinioSettings(endpoint, accessKey, secretKey);
    }

    public static class MinioSettings {
        private final String endpoint;
        private final String accessKey;
        private final String secretKey;

        public MinioSettings(String endpoint, String accessKey, String secretKey) {
            this.endpoint = endpoint;
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }
    }
}

