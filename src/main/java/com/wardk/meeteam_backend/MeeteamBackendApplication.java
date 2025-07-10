package com.wardk.meeteam_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnableJpaAuditing
@SpringBootApplication
public class MeeteamBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeeteamBackendApplication.class, args);
	}



}
