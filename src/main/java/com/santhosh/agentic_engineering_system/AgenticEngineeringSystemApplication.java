package com.santhosh.agentic_engineering_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgenticEngineeringSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgenticEngineeringSystemApplication.class, args);
	}

}
