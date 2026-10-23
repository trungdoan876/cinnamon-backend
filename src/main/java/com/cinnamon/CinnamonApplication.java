package com.cinnamon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CinnamonApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinnamonApplication.class, args);
	}

}
