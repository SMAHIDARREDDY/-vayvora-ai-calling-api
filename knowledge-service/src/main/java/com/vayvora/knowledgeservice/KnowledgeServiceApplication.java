package com.vayvora.knowledgeservice;

import com.vayvora.shared.web.Web;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Knowledge base ingestion and retrieval (spec §7).
 */
@SpringBootApplication(scanBasePackages = {
        "com.vayvora.knowledgeservice",
        "com.vayvora.shared.web"
})
@EntityScan(basePackages = "com.vayvora.shared.entity")
@EnableJpaRepositories(basePackages = "com.vayvora.shared.repository")
public class KnowledgeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeServiceApplication.class, args);
    }

    @Bean
    public Web.TenantFilter tenantFilter() {
        return new Web.TenantFilter();
    }
}
