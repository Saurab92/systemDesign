package com.systemdesign.demo.systemdesign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class SystemdesignApplication {

	public static void main(String[] args) {
		SpringApplication.run(SystemdesignApplication.class, args);
	}

}
