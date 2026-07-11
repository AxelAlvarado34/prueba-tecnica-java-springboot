package com.axel.alvarado.coworking_service;

import org.springframework.boot.SpringApplication;

public class TestCoworkingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoworkingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
