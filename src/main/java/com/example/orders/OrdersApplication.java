package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@Import({HealthController.class, OrderController.class, OrderService.class,
        OutboxPublisher.class, OutboxTransaction.class})
public class OrdersApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }

    @Bean("dependencies")
    DependenciesHealth dependencies(JdbcTemplate jdbc, ConnectionFactory rabbit) {
        return new DependenciesHealth(jdbc, rabbit);
    }
}
