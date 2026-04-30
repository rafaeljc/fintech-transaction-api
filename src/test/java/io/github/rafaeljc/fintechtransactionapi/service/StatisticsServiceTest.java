package io.github.rafaeljc.fintechtransactionapi.service;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private TransactionStore store;

    private StatisticsService service;

    @BeforeEach
    void setUp() {
        service = new StatisticsService(store);
    }

    @Test
    void getReturnsStoreStatistics() {
        StatisticsResponse expected = new StatisticsResponse(
                3, new BigDecimal("30.00"), new BigDecimal("10.00"),
                new BigDecimal("5.00"), new BigDecimal("15.00"));
        when(store.statistics()).thenReturn(expected);

        StatisticsResponse result = service.get();

        assertThat(result).isEqualTo(expected);
        verify(store).statistics();
    }
}
