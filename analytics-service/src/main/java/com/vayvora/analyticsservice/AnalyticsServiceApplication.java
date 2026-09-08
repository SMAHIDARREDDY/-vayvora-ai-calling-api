package com.vayvora.analyticsservice;

import com.vayvora.shared.web.Web;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Dashboard metrics, call analytics and usage rollups (spec §18, §22).
 */
@SpringBootApplication(scanBasePackages = {
        "com.vayvora.analyticsservice",
        "com.vayvora.shared.web"
})
@EntityScan(basePackages = "com.vayvora.shared.entity")
@EnableJpaRepositories(basePackages = "com.vayvora.shared.repository")
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }

    @Bean
    public Web.TenantFilter tenantFilter() {
        return new Web.TenantFilter();
    }
}
