package sgw.common.persistence.config;

import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class MyBatisConfiguration {

    @Value("${mybatis.mapper-locations:classpath*:mapper/**/*.xml}")
    private String mapperLocations;

    @Value("${mybatis.type-aliases-package:}")
    private String typeAliasesPackage;

    @Autowired(required = false)
    private List<TypeHandler<?>> typeHandlers;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocations));
        if (typeAliasesPackage != null && !typeAliasesPackage.isEmpty()) {
            factoryBean.setTypeAliasesPackage(typeAliasesPackage);
        }
        if (typeHandlers != null && !typeHandlers.isEmpty()) {
            factoryBean.setTypeHandlers(typeHandlers.toArray(new TypeHandler<?>[0]));
        }
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("sgw.common.persistence.mapper");
        return configurer;
    }
}

