package com.axel.alvarado.coworking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableCaching
public class CoworkingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoworkingServiceApplication.class, args);
	}

}
