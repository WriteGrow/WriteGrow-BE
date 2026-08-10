package com.example.writegrow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WritegrowApplication {

	public static void main(String[] args) {
		SpringApplication.run(WritegrowApplication.class, args);
	}

}
