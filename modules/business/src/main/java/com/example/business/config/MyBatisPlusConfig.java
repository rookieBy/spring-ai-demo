package com.example.business.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus Configuration
 */
@Configuration
@MapperScan("com.example.business.mapper")
public class MyBatisPlusConfig {
}
