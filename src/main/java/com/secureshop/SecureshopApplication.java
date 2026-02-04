package com.secureshop;

import com.secureshop.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class SecureshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecureshopApplication.class, args);
	}

}
