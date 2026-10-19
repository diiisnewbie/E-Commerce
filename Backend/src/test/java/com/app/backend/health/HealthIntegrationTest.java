package com.app.backend.health;

import com.app.backend.common.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class HealthIntegrationTest extends BaseIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Test
    void TC_HEALTH_01_health_returns_200_with_standard_format() {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange("/api/v1/health", HttpMethod.GET, null, MAP_TYPE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(response.getHeaders().getContentType()).toString())
                .contains("application/json");

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("errorCode")).isNull();
        assertThat(body.get("message")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("status")).isEqualTo("UP");
        assertThat(data.get("service")).isEqualTo("ecommerce-backend");

        String timestamp = (String) body.get("timestamp");
        assertThat(timestamp).isNotBlank();
        Instant parsed = Instant.parse(timestamp);
        assertThat(Duration.between(Instant.now(), parsed).abs().toSeconds()).isLessThanOrEqualTo(5);
    }

    @Test
    void TC_HEALTH_02_health_requires_no_authentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void TC_HEALTH_03_flyway_migration_applied() {
        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                Integer.class,
                "flyway_schema_history");
        assertThat(historyCount).isEqualTo(1);

        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = ?",
                Boolean.class,
                "1");
        assertThat(success).isTrue();
    }

    @Test
    void TC_HEALTH_04_redis_is_reachable() {
        String ping = redisConnectionFactory.getConnection().ping();

        assertThat(ping).isNotNull();
        assertThat(ping.toUpperCase()).contains("PONG");
    }

    @Test
    void TC_CORS_01_preflight_allows_frontend_origin() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:5173");
        headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/health", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:5173");

        List<String> allowMethods = response.getHeaders().get("Access-Control-Allow-Methods");
        assertThat(allowMethods).isNotNull();
        assertThat(String.join(",", allowMethods)).contains("GET");
    }
}
