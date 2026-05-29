package com.ashish.saas.multitanantsaasapp.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight health-check controller.
 * <p>
 * Exposes a public {@code /api/v1/health} endpoint that returns the current
 * application status along with database connectivity and uptime metadata.
 * Useful for load-balancer probes, container orchestrators, and monitoring dashboards.
 */
@RestController
@RequestMapping("api/v1/health")
@Tag(name = "Health", description = "Application health and readiness checks")
public class HealthController {

    private static final Instant STARTUP_TIME = Instant.now();

    private final DataSource dataSource;
    private final Optional<BuildProperties> buildProperties;

    public HealthController(DataSource dataSource,
                            Optional<BuildProperties> buildProperties) {
        this.dataSource = dataSource;
        this.buildProperties = buildProperties;
    }

    @GetMapping
    @Operation(
            summary = "Application health check",
            description = """
                    Returns the current health status of the application including
                    database connectivity, uptime, and build version metadata.
                    This endpoint is unauthenticated and safe to use with load-balancer probes."""
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        body.put("uptime", formatUptime());
        body.put("database", checkDatabase());
        body.put("version", buildProperties.map(BuildProperties::getVersion).orElse("dev"));
        return ResponseEntity.ok(body);
    }

    // ── internal helpers ──────────────────────────────────────────────────

    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            db.put("status", "UP");
            db.put("database", conn.getMetaData().getDatabaseProductName());
            db.put("url", conn.getMetaData().getURL());
        } catch (Exception ex) {
            db.put("status", "DOWN");
            db.put("error", ex.getMessage());
        }
        return db;
    }

    private String formatUptime() {
        long seconds = Instant.now().getEpochSecond() - STARTUP_TIME.getEpochSecond();
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%dh %dm %ds", hrs, mins, secs);
    }
}
