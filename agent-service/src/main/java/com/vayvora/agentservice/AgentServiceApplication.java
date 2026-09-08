package com.vayvora.agentservice;

import com.vayvora.shared.web.Web;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * AI agent configuration, versioning and publishing (spec §6, §36).
 */
@SpringBootApplication(scanBasePackages = {
        "com.vayvora.agentservice",
        "com.vayvora.shared.web"
})
@EntityScan(basePackages = "com.vayvora.shared.entity")
@EnableJpaRepositories(basePackages = "com.vayvora.shared.repository")
public class AgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }

    /** Establishes tenant context from the gateway's identity headers. */
    @Bean
    public Web.TenantFilter tenantFilter() {
        return new Web.TenantFilter();
    }
}
