package com.kbait.anchack.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.config.PlaceConfig;
import com.kbait.anchack.recommendation.config.RecommendationConfig;
import com.kbait.anchack.rental.config.MolitRentConfig;
import com.kbait.anchack.rental.config.MolitRentSchedulerConfig;
import com.kbait.anchack.route.config.RouteConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@Configuration
@PropertySource(
    value = "classpath:application.properties",
    encoding = "UTF-8"
)
@MapperScan(basePackages = "com.kbait.anchack.*.mapper")
@ComponentScan(basePackages = {
    "com.kbait.anchack.*.service",
    "com.kbait.anchack.common.security",
    "com.kbait.anchack.rental.client"
})
@Import({
    PlaceConfig.class,
    RecommendationConfig.class,
    MolitRentConfig.class,
    RouteConfig.class,
    SchedulingConfig.class,
    MolitRentSchedulerConfig.class
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

    // HikariCP DataSource 설정
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // k6 브레이크포인트 부하테스트(loadtest/README.md)로 20까지 올려서 비교해봤지만
        // 무너지는 지점(VU~150, p95 3초)이 거의 그대로였다 - Prometheus로 확인해보니 20에서는
        // MySQL threads_connected가 실제로 20까지 늘어나는데도 그랬다. 이 로컬 DB 컨테이너의
        // CPU(물리 4코어)가 그 이상의 동시 쓰기 트랜잭션을 처리할 여력이 없어서, 병목이
        // "커넥션 대기"에서 "DB CPU/락 경합"으로 옮겨갈 뿐 근본적으로 해소되지 않는 것으로
        // 보인다(HikariCP 권장 공식 (코어수*2)+1 ≈ 9로도 10이 이미 이 머신 기준 적정값이었음).
        // 오히려 동시 쓰기 트랜잭션이 늘어나 데드락(DeadlockRetry) 빈도만 커질 수 있어 10으로
        // 되돌림 - 더 올리려면 스코어링 쿼리 캐싱 등 근본 원인을 먼저 봐야 한다.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    // Flyway 데이터베이스 마이그레이션 설정
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .locations("classpath:db/migration")
            .load();
    }

    // Flyway 실행 이후 SqlSessionFactory 생성
    @Bean
    public SqlSessionFactory sqlSessionFactory(
        DataSource dataSource,
        Flyway flyway
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
                "classpath*:mappers/**/*.xml"
            )
        );

        return factoryBean.getObject();
    }

    // 트랜잭션 관리자 설정
    @Bean
    public DataSourceTransactionManager transactionManager(
        DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }

    // 카카오 등 외부 API 호출에 사용하는 공용 RestTemplate
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // 카카오 API 응답(JSON) 파싱에 사용하는 공용 ObjectMapper
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
