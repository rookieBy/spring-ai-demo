package com.wifiin.newsay.ai.launcher;

import org.apache.commons.lang3.time.StopWatch;
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
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        SpringApplication.run(Application.class, args);
        System.out.println("====== application start elapse: " + stopWatch.getTime() + "======");
    }
}
