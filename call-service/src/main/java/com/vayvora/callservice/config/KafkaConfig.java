package com.vayvora.callservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {
    // Kafka configuration is handled by Spring Boot auto-configuration
    // based on application.yml settings
}
