package com.evernight;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.evernight.base.mapper")
public class EvernightApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvernightApplication.class, args);
    }

}
