package com.wifiin.newsay.ai.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Collections;

/**
 * Common Beans Configuration
 * Centralizes all DataSource, Redis, and web configuration
 * Note: MyBatis Plus auto-configuration is enabled via spring-boot-autoconfigure
 */
@Configuration
public class CommonBeans {

    private static final Logger logger = LogManager.getLogger(CommonBeans.class);

    // ==================== DataSource Configuration ====================

    @Value("${spring.datasource.mysql.jdbcUrl}")
    private String dbUrl;

    @Value("${spring.datasource.mysql.username}")
    private String dbUsername;

    @Value("${spring.datasource.mysql.password}")
    private String dbPassword;

    @Value("${spring.datasource.mysql.driver-class-name}")
    private String dbDriverClass;

    @Value("${spring.datasource.druid.initial-size}")
    private int druidInitialSize;

    @Value("${spring.datasource.druid.min-idle}")
    private int druidMinIdle;

    @Value("${spring.datasource.druid.max-active}")
    private int druidMaxActive;

    @Value("${spring.datasource.mysql.connectionTimeout}")
    private int mysqlConnectionTimeout;
    @Value("${spring.datasource.mysql.connectionInitSql}")
    private String mysqlTestSql;
    @Value("${spring.datasource.mysql.validationTimeout}")
    private int validationTimeout;


    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Initializing Druid DataSource with URL: {}", dbUrl);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        dataSource.setDriverClassName(dbDriverClass);
        dataSource.setInitialSize(druidInitialSize);
        dataSource.setMinIdle(druidMinIdle);
        dataSource.setMaxActive(druidMaxActive);
        dataSource.setConnectTimeout(mysqlConnectionTimeout);
        dataSource.setConnectionInitSqls(Collections.singletonList(mysqlTestSql));
        dataSource.setValidationQueryTimeout(validationTimeout);
        return dataSource;
    }

    // ==================== Redis Configuration ====================

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:root}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Initializing Redis ConnectionFactory with host: {}, port: {}", redisHost, redisPort);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    // ==================== Redisson Configuration ====================

    @Bean
    public RedissonClient redissonClient() {
        logger.info("Initializing Redisson Client with host: {}, port: {}", redisHost, redisPort);
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }

    // ==================== Web MVC Configuration ====================

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
