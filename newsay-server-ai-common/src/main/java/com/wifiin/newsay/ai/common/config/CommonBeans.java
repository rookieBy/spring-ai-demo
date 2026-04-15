package com.wifiin.newsay.ai.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

import com.wifiin.newsay.ai.llm.service.ChatMemoryService;
import com.wifiin.newsay.ai.llm.service.impl.RedisChatMemoryServiceImpl;

import javax.sql.DataSource;

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

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int hikariMinIdle;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int hikariMaxActive;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long hikariConnectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long hikariIdleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long hikariMaxLifetime;

    @Value("${spring.datasource.mysql.connectionInitSql:select 1}")
    private String mysqlTestSql;


    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Initializing HikariCP DataSource with URL: {}", dbUrl);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName(dbDriverClass);
        config.setMinimumIdle(hikariMinIdle);
        config.setMaximumPoolSize(hikariMaxActive);
        config.setConnectionTimeout(hikariConnectionTimeout);
        config.setIdleTimeout(hikariIdleTimeout);
        config.setMaxLifetime(hikariMaxLifetime);
        config.setConnectionTestQuery(mysqlTestSql);
        return new HikariDataSource(config);
    }

    // ==================== Redis Configuration ====================

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:redis}")
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
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
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
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        return Redisson.create(config);
    }

    // ==================== Web MVC Configuration ====================

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // ==================== Chat Memory Configuration ====================

    @Bean
    public ChatMemoryService chatMemoryService(StringRedisTemplate stringRedisTemplate) {
        return new RedisChatMemoryServiceImpl(
                stringRedisTemplate,
                20,  // slidingWindowSize
                3600 // ttlSeconds
        );
    }
}
