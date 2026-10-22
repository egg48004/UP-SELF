package com.upself.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.upself")
public class UpSelfApplication {
    public static void main(String[] args) {
        SpringApplication.run(UpSelfApplication.class, args);
    }
}
