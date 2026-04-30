package io.github.rafaeljc.fintechtransactionapi.controller;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TransactionControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionStore store;

    @BeforeEach
    void clearStore() {
        store.clear();
    }

    private HttpEntity<String> jsonBody(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void postWithValidBodyReturns201() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":\"42.00\",\"dateTime\":\"2024-01-15T10:00:00Z\"}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void postWithNullAmountReturns422() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":null,\"dateTime\":\"2024-01-15T10:00:00Z\"}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void postWithNullDateTimeReturns422() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":\"42.00\",\"dateTime\":null}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void postWithFutureDateTimeReturns422() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":\"42.00\",\"dateTime\":\"2099-01-15T10:00:00Z\"}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void postWithNegativeAmountReturns422() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":\"-1.00\",\"dateTime\":\"2024-01-15T10:00:00Z\"}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void postWithZeroAmountReturns201() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":\"0.00\",\"dateTime\":\"2024-01-15T10:00:00Z\"}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void postWithMalformedJsonReturns400() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/transactions",
            jsonBody("{invalid}"),
            Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deleteReturns200() {
        ResponseEntity<Void> response = restTemplate.exchange(
            "/transactions", HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void afterDeleteGetStatisticsReturnsAllZeros() {
        assertThat(restTemplate.postForEntity(
            "/transactions",
            jsonBody("{\"amount\":\"42.00\",\"dateTime\":\"2024-01-15T10:00:00Z\"}"),
            Void.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        restTemplate.exchange("/transactions", HttpMethod.DELETE, null, Void.class);

        ResponseEntity<StatisticsResponse> response = restTemplate.getForEntity(
            "/statistics", StatisticsResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StatisticsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.count()).isZero();
        assertThat(body.sum()).isEqualByComparingTo("0");
        assertThat(body.avg()).isEqualByComparingTo("0");
        assertThat(body.min()).isEqualByComparingTo("0");
        assertThat(body.max()).isEqualByComparingTo("0");
    }
}
