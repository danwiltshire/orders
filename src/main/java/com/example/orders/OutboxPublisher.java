package com.example.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxTransaction transaction;

    public OutboxPublisher(OutboxTransaction transaction) {
        this.transaction = transaction;
    }

    @Scheduled(fixedDelay = 1000)
    public void publish() {
        try {
            transaction.publishOne();
        } catch (RuntimeException ex) {
            log.error("Outbox publication failed; will retry", ex);
        }
    }
}

@Component
class OutboxTransaction {
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    OutboxTransaction(JdbcTemplate jdbc, RabbitTemplate rabbit) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
    }

    @Transactional
    public void publishOne() {
        var pending = jdbc.query("SELECT order_id FROM order_outbox WHERE published_at IS NULL " +
                        "ORDER BY created_at, order_id LIMIT 1 FOR UPDATE SKIP LOCKED",
                (rs, row) -> rs.getObject(1, UUID.class));
        if (pending.isEmpty()) {
            return;
        }
        UUID id = pending.getFirst();
        CorrelationData correlation = new CorrelationData(id.toString());
        rabbit.convertAndSend(Messaging.EXCHANGE, Messaging.ROUTING_KEY, id.toString(), message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(id.toString());
            return message;
        }, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("Order event was not routed/confirmed: " + id + " " + confirm.getReason());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for publisher confirm", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Publisher confirm failed for " + id, ex);
        }
        jdbc.update("UPDATE order_outbox SET published_at = now() WHERE order_id = ?", id);
    }
}
