package com.smart_api_gateway;

import com.smart_api_gateway.service.RateLimiterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SentinelGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentinelGatewayApplication.class, args);
	}



}
