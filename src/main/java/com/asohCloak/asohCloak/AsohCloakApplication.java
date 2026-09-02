package com.asohCloak.asohCloak;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsohCloakApplication {

	static void main(String[] args) {
		SpringApplication.run(AsohCloakApplication.class, args);
	}

}
