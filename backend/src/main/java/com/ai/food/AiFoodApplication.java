package com.ai.food;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiFoodApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFoodApplication.class, args);
    }
}