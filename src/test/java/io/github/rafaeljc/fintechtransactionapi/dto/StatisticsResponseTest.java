package io.github.rafaeljc.fintechtransactionapi.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsResponseTest {

    @Test
    void accessorsReturnConstructedValues() {
        var response = new StatisticsResponse(5L, new BigDecimal("50.00"),
                new BigDecimal("10.00"), new BigDecimal("2.00"), new BigDecimal("20.00"));

        assertThat(response.count()).isEqualTo(5L);
        assertThat(response.sum()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.avg()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.min()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(response.max()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void emptyReturnsAllZeros() {
        StatisticsResponse response = StatisticsResponse.empty();

        assertThat(response.count()).isEqualTo(0L);
        assertThat(response.sum()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.avg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.min()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.max()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
