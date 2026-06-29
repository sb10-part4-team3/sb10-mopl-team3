package com.example.sb10_MoPl_team3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Sb10MoPlTeam3Application {

	public static void main(String[] args) {
		SpringApplication.run(Sb10MoPlTeam3Application.class, args);
	}

}
