package com.vayvora.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for the platform API (spec §30).
 *
 * <p>Validates the bearer token once, resolves the caller's identity, and
 * forwards it downstream as trusted headers so each service does not repeat
 * the work.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
