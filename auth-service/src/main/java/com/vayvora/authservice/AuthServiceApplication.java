package com.vayvora.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Identity, login and organization onboarding (spec §5, §30).
 *
 * <p>Entity and repository scanning is pointed at the shared module, which owns
 * the schema for the whole platform.
 */
@SpringBootApplication(scanBasePackages = {
        "com.vayvora.authservice",
        "com.vayvora.shared.web"
})
@EntityScan(basePackages = "com.vayvora.shared.entity")
@EnableJpaRepositories(basePackages = "com.vayvora.shared.repository")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
