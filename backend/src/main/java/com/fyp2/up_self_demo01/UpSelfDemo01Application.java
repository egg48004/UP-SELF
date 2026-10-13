package com.fyp2.up_self_demo01;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fyp2.up_self_demo01.mapper")
public class UpSelfDemo01Application {

	public static void main(String[] args) {
		SpringApplication.run(UpSelfDemo01Application.class, args);
	}

}
