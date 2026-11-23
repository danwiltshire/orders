package com.example.orders;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({HealthController.class, Messaging.class, OrderWorker.class})
public class WorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }

    @Bean("dependencies")
    DependenciesHealth dependencies(JdbcTemplate jdbc, ConnectionFactory rabbit) {
        return new DependenciesHealth(jdbc, rabbit);
    }
}
