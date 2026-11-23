package com.example.orders;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {
    private final JdbcTemplate jdbc;

    public OrderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Order create(long productId, int quantity) {
        var prices = jdbc.query("SELECT price_cents FROM catalog_items WHERE id = ?",
                (rs, row) -> rs.getInt(1), productId);
        if (prices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog item not found");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO orders (id, product_id, quantity, unit_price_cents, status) VALUES (?, ?, ?, ?, 'PENDING')",
                id, productId, quantity, prices.getFirst());
        jdbc.update("INSERT INTO order_outbox (order_id) VALUES (?)", id);
        return get(id);
    }

    public Order get(UUID id) {
        return jdbc.query("SELECT id, product_id, quantity, unit_price_cents, status, created_at FROM orders WHERE id = ?",
                (rs, row) -> new Order(rs.getObject("id", UUID.class), rs.getLong("product_id"),
                        rs.getInt("quantity"), rs.getInt("unit_price_cents"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant()), id)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public record Order(UUID id, long productId, int quantity, int unitPriceCents, String status, Instant createdAt) {}
}
