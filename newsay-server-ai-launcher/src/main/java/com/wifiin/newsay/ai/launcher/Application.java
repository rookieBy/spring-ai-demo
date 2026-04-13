package com.wifiin.newsay.ai.launcher;

import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


/**
 * Spring Boot Application Entry Point
 * 简化版：只启用 LLM 调用功能，禁用数据库和缓存
 */
@SpringBootApplication(scanBasePackages = {"com.wifiin.newsay.**"},
        exclude = {RedissonAutoConfigurationV2.class})
@EnableAspectJAutoProxy
public class Application {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        SpringApplication.run(Application.class, args);
        System.out.println("====== application start elapse: " + (System.currentTimeMillis() - startTime) + "ms======");
    }
}
