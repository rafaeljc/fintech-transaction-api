package io.github.rafaeljc.fintechtransactionapi.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private TransactionStore store;

    private StatisticsService service;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger statisticsServiceLogger;

    @BeforeEach
    void setUp() {
        service = new StatisticsService(store);

        statisticsServiceLogger = (Logger) LoggerFactory.getLogger(StatisticsService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        statisticsServiceLogger.setLevel(Level.DEBUG);
        statisticsServiceLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        statisticsServiceLogger.detachAppender(listAppender);
        listAppender.stop();
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

    @Test
    void getEmitsDebugLogLine() {
        when(store.statistics()).thenReturn(new StatisticsResponse(
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        service.get();

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).anyMatch(event ->
                event.getLevel() == Level.DEBUG
                && event.getFormattedMessage().contains("Statistics calculated: durationMicros="));
    }

    @Test
    void getLogsDurationNonNegative() {
        when(store.statistics()).thenReturn(new StatisticsResponse(
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        service.get();

        String message = listAppender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("durationMicros="))
                .findFirst()
                .orElseThrow()
                .getFormattedMessage();

        long durationMicros = Long.parseLong(message.split("durationMicros=")[1]);
        assertThat(durationMicros).isGreaterThanOrEqualTo(0);
    }
}
