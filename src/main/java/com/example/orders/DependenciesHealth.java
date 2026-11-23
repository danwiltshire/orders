package com.example.orders;

import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;

public class DependenciesHealth implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final ConnectionFactory rabbit;

    public DependenciesHealth(JdbcTemplate jdbc) {
        this(jdbc, null);
    }

    public DependenciesHealth(JdbcTemplate jdbc, ConnectionFactory rabbit) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
    }

    @Override
    public Health health() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            if (rabbit != null) {
                try (Connection connection = rabbit.createConnection()) {
                    if (!connection.isOpen()) {
                        throw new IllegalStateException("RabbitMQ connection is closed");
                    }
                }
            }
            return Health.up().build();
        } catch (RuntimeException ex) {
            return Health.down(ex).build();
        }
    }
}
