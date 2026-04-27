package io.github.rafaeljc.fintechtransactionapi.service;

import io.github.rafaeljc.fintechtransactionapi.dto.TransactionRequest;
import io.github.rafaeljc.fintechtransactionapi.model.Transaction;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionStore store;

    public TransactionService(TransactionStore store) {
        this.store = store;
    }

    public void add(TransactionRequest request) {
        store.add(new Transaction(request.amount(), request.dateTime()));
    }

    public void clear() {
        store.clear();
    }
}
