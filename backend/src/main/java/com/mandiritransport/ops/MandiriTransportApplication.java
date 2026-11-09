package com.mandiritransport.ops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.mandiritransport.ops.**.mapper")
public class MandiriTransportApplication {

    public static void main(String[] args) {
        SpringApplication.run(MandiriTransportApplication.class, args);
    }
}

