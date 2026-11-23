package com.example.orders;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = WorkerApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkerFlowTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RabbitTemplate template;
    @Autowired MockMvc mvc;
    @MockitoSpyBean OrderWorker worker;

    @Test
    void consumesAndAcknowledgesDuplicateEvents() throws Exception {
        mvc.perform(get("/api/catalog")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound());
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO orders (id, product_id, quantity, unit_price_cents, status) VALUES (?, 1, 1, 1299, 'PENDING')", id);
        template.convertAndSend(Messaging.EXCHANGE, Messaging.ROUTING_KEY, id.toString());
        template.convertAndSend(Messaging.EXCHANGE, Messaging.ROUTING_KEY, id.toString());
        verify(worker, timeout(20000).times(2)).process(id.toString());
        RabbitAdmin admin = new RabbitAdmin(template);
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, id))
                    .isEqualTo("PROCESSED");
            assertThat(admin.getQueueInfo(Messaging.QUEUE)).isNotNull();
            assertThat(admin.getQueueInfo(Messaging.QUEUE).getMessageCount()).isZero();
        });
    }
}
