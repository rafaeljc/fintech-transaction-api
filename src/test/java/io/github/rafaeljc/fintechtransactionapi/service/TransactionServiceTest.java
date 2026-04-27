package io.github.rafaeljc.fintechtransactionapi.service;

import io.github.rafaeljc.fintechtransactionapi.dto.TransactionRequest;
import io.github.rafaeljc.fintechtransactionapi.model.Transaction;
import io.github.rafaeljc.fintechtransactionapi.store.TransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionStore store;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(store);
    }

    @Test
    void addDelegatesToStore() {
        BigDecimal amount = new BigDecimal("42.00");
        OffsetDateTime dateTime = OffsetDateTime.now();
        var request = new TransactionRequest(amount, dateTime);

        service.add(request);

        verify(store).add(new Transaction(amount, dateTime));
    }

    @Test
    void clearDelegatesToStore() {
        service.clear();

        verify(store).clear();
    }
}
