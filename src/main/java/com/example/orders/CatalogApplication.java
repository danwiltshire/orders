package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({HealthController.class, CatalogController.class})
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }

    @Bean("dependencies")
    DependenciesHealth dependencies(JdbcTemplate jdbc) {
        return new DependenciesHealth(jdbc);
    }
}
