package com.lazyapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LazyAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(LazyAppApplication.class, args);
    }
}
