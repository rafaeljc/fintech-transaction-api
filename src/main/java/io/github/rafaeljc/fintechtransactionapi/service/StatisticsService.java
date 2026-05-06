package io.github.rafaeljc.fintechtransactionapi.service;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsService.class);

    private final TransactionStore store;

    public StatisticsService(TransactionStore store) {
        this.store = store;
    }

    public StatisticsResponse get() {
        long start = System.nanoTime();
        StatisticsResponse response = store.statistics();
        long durationMicros = (System.nanoTime() - start) / 1_000;
        LOG.debug("Statistics calculated: durationMicros={}", durationMicros);
        return response;
    }
}
