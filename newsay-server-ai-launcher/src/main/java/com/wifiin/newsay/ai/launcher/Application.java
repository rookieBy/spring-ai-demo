package com.wifiin.newsay.ai.launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


/**
 * Spring Boot Application Entry Point
 * 简化版：只启用 LLM 调用功能，禁用数据库和缓存
 */
@SpringBootApplication(scanBasePackages = {"com.wifiin.newsay.**"}, exclude = {
        DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@EnableAspectJAutoProxy
public class Application {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        SpringApplication.run(Application.class, args);
        System.out.println("====== application start elapse: " + (System.currentTimeMillis() - startTime) + "ms======");
    }
}
