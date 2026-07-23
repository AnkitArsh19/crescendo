package com.crescendo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableAsync
@EnableJpaRepositories
// Application entry point.
public class CrescendoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrescendoApplication.class, args);
	}

}
