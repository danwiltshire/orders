package com.example.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OrderWorker {
    private static final Logger log = LoggerFactory.getLogger(OrderWorker.class);
    private final JdbcTemplate jdbc;

    public OrderWorker(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @RabbitListener(queues = Messaging.QUEUE)
    @Transactional
    public void process(String body) {
        UUID id = UUID.fromString(body);
        log.info("Processing order event for order {}", id);
        int updated = jdbc.update("UPDATE orders SET status = 'PROCESSED' WHERE id = ? AND status = 'PENDING'", id);
        if (updated == 0) {
            var exists = jdbc.queryForObject("SELECT count(*) FROM orders WHERE id = ?", Integer.class, id);
            if (exists == null || exists == 0) {
                throw new IllegalStateException("Order event refers to missing order: " + id);
            }
            log.info("Order {} already processed; skipping duplicate event", id);
        } else {
            log.info("Order {} updated to PROCESSED; awaiting transaction commit", id);
        }
    }
}
