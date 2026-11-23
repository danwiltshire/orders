package com.example.orders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class OrderWorkerTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final OrderWorker worker = new OrderWorker(jdbc);

    @Test
    void logsProcessingAndUpdate(CapturedOutput output) {
        UUID id = UUID.randomUUID();
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.eq(id))).thenReturn(1);

        worker.process(id.toString());

        assertThat(output).contains("Processing order event for order " + id);
        assertThat(output).contains("Order " + id + " updated to PROCESSED; awaiting transaction commit");
    }

    @Test
    void logsDuplicateWithoutClaimingAnUpdate(CapturedOutput output) {
        UUID id = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq(id))).thenReturn(1);

        worker.process(id.toString());

        assertThat(output).contains("Order " + id + " already processed; skipping duplicate event");
        assertThat(output).doesNotContain("updated to PROCESSED");
    }

    @Test
    void missingOrderStillFailsWithoutClaimingSuccess(CapturedOutput output) {
        UUID id = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq(id))).thenReturn(0);

        assertThatThrownBy(() -> worker.process(id.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(id.toString());
        assertThat(output).contains("Processing order event for order " + id);
        assertThat(output).doesNotContain("updated to PROCESSED", "already processed");
    }
}
