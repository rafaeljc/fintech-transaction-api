package io.github.rafaeljc.fintechtransactionapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIT {

    @LocalServerPort
    private int port;

    @Test
    void openapiYamlReturns200() {
        ResponseEntity<String> response = RestClient.create()
            .get()
            .uri("http://localhost:" + port + "/api/v1/openapi.yaml")
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void swaggerUiIsAccessible() {
        ResponseEntity<String> response = RestClient.create()
            .get()
            .uri("http://localhost:" + port + "/api/v1/swagger-ui")
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
