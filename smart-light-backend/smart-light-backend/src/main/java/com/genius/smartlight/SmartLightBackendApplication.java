package com.genius.smartlight;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.genius.smartlight.dal.mysql")
@SpringBootApplication
public class SmartLightBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLightBackendApplication.class, args);
    }
}