package io.github.rafaeljc.fintechtransactionapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0"
)
class HealthCheckIT {

    @LocalManagementPort
    private int managementPort;

    @Test
    void healthReturns200() {
        ResponseEntity<String> response = RestClient.create()
            .get()
            .uri("http://localhost:" + managementPort + "/actuator/health")
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void healthBodyContainsStatusUp() {
        ResponseEntity<String> response = RestClient.create()
            .get()
            .uri("http://localhost:" + managementPort + "/actuator/health")
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
