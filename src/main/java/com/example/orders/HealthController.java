package com.example.orders;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final HealthEndpoint health;

    public HealthController(HealthEndpoint health) {
        this.health = health;
    }

    @GetMapping("/healthz/live")
    public ResponseEntity<HealthComponent> live() {
        return response("liveness");
    }

    @GetMapping("/healthz/ready")
    public ResponseEntity<HealthComponent> ready() {
        return response("readiness");
    }

    private ResponseEntity<HealthComponent> response(String group) {
        HealthComponent result = health.healthForPath(group);
        return ResponseEntity.status(Status.UP.equals(result.getStatus())
                ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }
}
