package com.igirepay.idempotency.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration
 */
@Configuration
public class AppConfig {

    /**
     * ObjectMapper bean for JSON serialization/deserialization
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Automatically find and register available modules (e.g. JavaTimeModule if on classpath)
        mapper.findAndRegisterModules();
        return mapper;
    }
}
