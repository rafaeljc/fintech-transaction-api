package io.github.rafaeljc.fintechtransactionapi.controller;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class StatisticsControllerTest {

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

    private String nowMinus(int seconds) {
        return OffsetDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
                .minusSeconds(seconds)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Test
    void getReturns200WithAllZerosWhenStoreIsEmpty() {
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

    @Test
    void getReturnsCorrectValuesAfterInsertingTransactions() {
        restTemplate.postForEntity(
                "/transactions",
                jsonBody("{\"amount\":\"10.00\",\"dateTime\":\"" + nowMinus(2) + "\"}"),
                Void.class);
        restTemplate.postForEntity(
                "/transactions",
                jsonBody("{\"amount\":\"20.00\",\"dateTime\":\"" + nowMinus(1) + "\"}"),
                Void.class);

        ResponseEntity<StatisticsResponse> response = restTemplate.getForEntity(
                "/statistics", StatisticsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StatisticsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.count()).isEqualTo(2L);
        assertThat(body.sum()).isEqualByComparingTo("30.00");
        assertThat(body.avg()).isEqualByComparingTo("15.00");
        assertThat(body.min()).isEqualByComparingTo("10.00");
        assertThat(body.max()).isEqualByComparingTo("20.00");
    }

    @Test
    void getExcludesTransactionsOutsideWindow() {
        String oldDateTime = nowMinus(61);
        restTemplate.postForEntity(
                "/transactions",
                jsonBody("{\"amount\":\"42.00\",\"dateTime\":\"" + oldDateTime + "\"}"),
                Void.class);

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

    @Test
    void getResponseJsonContainsExactlyExpectedFields() {
        restTemplate.postForEntity(
                "/transactions",
                jsonBody("{\"amount\":\"5.00\",\"dateTime\":\"" + nowMinus(1) + "\"}"),
                Void.class);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/statistics", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().keySet())
                .containsExactlyInAnyOrder("count", "sum", "avg", "min", "max");
    }
}
