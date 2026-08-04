package com.kbait.anchack.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@PropertySource(
        value = "classpath:application.properties",
        encoding = "UTF-8"
)
@MapperScan({
        "com.kbait.anchack.auth.mapper",
        "com.kbait.anchack.user.mapper"
})
@ComponentScan(basePackages = {
        "com.kbait.anchack.auth.service",
        "com.kbait.anchack.user.service",
        "com.kbait.anchack.common.security"
})
@EnableTransactionManagement
public class RootConfig {

    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    private final ApplicationContext applicationContext;

    public RootConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * HikariCP DataSource 설정
     */
    @Bean
    public DataSource dataSource() {

        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // 커넥션 풀 설정
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    /**
     * MyBatis SqlSessionFactory 설정
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(
            DataSource dataSource
    ) throws Exception {

        SqlSessionFactoryBean factoryBean =
                new SqlSessionFactoryBean();

        factoryBean.setDataSource(dataSource);

        factoryBean.setConfigLocation(
                applicationContext.getResource(
                        "classpath:mybatis-config.xml"
                )
        );

        factoryBean.setMapperLocations(
                applicationContext.getResources(
                        "classpath*:mapper/**/*.xml"
                )
        );

        return factoryBean.getObject();
    }

    /**
     * 트랜잭션 관리자 설정
     */
    @Bean
    public DataSourceTransactionManager transactionManager(
            DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}