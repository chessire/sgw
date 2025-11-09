package sgw.common.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local-noinfra")
@Import({
    DatabaseConfiguration.class,
    MyBatisConfiguration.class
})
public class PersistenceAutoConfiguration {
}

