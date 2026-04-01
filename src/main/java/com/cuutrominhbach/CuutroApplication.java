package com.cuutrominhbach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CuutroApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuutroApplication.class, args);
    }
}
