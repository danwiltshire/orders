package com.example.orders;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final JdbcTemplate jdbc;

    public CatalogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<CatalogItem> list() {
        return jdbc.query("SELECT id, name, price_cents FROM catalog_items ORDER BY id",
                (rs, row) -> new CatalogItem(rs.getLong("id"), rs.getString("name"), rs.getInt("price_cents")));
    }

    public record CatalogItem(long id, String name, int priceCents) {}
}
