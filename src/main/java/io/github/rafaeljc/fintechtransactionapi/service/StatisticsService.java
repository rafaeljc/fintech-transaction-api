package io.github.rafaeljc.fintechtransactionapi.service;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private final TransactionStore store;

    public StatisticsService(TransactionStore store) {
        this.store = store;
    }

    public StatisticsResponse get() {
        return store.statistics();
    }
}
