package com.sky;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication//启动整个 Spring Boot 项目
@EnableTransactionManagement //开启注解方式的事务管理
@EnableCaching        //开启缓存 Spring是如何知道启用缓存，通过@Cacheable注解
@EnableScheduling//请开启定时任务功能
@Slf4j//日志写上之后自动生成log对象 直接使用log.info
public class SkyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkyApplication.class, args);
        log.info("server started");
    }
}
