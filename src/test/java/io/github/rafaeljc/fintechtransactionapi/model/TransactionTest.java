package io.github.rafaeljc.fintechtransactionapi.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void accessorsReturnConstructedValues() {
        BigDecimal amount = new BigDecimal("99.50");
        OffsetDateTime dateTime = OffsetDateTime.parse("2024-06-01T10:00:00+00:00");

        Transaction transaction = new Transaction(amount, dateTime);

        assertThat(transaction.amount()).isEqualByComparingTo(amount);
        assertThat(transaction.dateTime()).isEqualTo(dateTime);
    }

    @Test
    void equalityBasedOnAmountAndDateTime() {
        BigDecimal amount = new BigDecimal("10.00");
        OffsetDateTime dateTime = OffsetDateTime.parse("2024-01-15T12:00:00+00:00");

        Transaction t1 = new Transaction(amount, dateTime);
        Transaction t2 = new Transaction(amount, dateTime);

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }
}
