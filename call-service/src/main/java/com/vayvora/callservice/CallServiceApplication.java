package com.vayvora.callservice;

import com.vayvora.shared.web.Web;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Call lifecycle, transcripts and call intelligence (spec §10, §14, §15).
 */
@SpringBootApplication(scanBasePackages = {
        "com.vayvora.callservice",
        "com.vayvora.shared.web"
})
@EntityScan(basePackages = "com.vayvora.shared.entity")
@EnableJpaRepositories(basePackages = "com.vayvora.shared.repository")
public class CallServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallServiceApplication.class, args);
    }

    @Bean
    public Web.TenantFilter tenantFilter() {
        return new Web.TenantFilter();
    }
}
