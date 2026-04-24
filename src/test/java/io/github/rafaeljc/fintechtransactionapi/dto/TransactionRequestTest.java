package io.github.rafaeljc.fintechtransactionapi.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validation_validRequest_noViolations() {
        var request = new TransactionRequest(
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validation_zeroAmount_noViolations() {
        var request = new TransactionRequest(
                BigDecimal.ZERO,
                OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validation_presentDateTime_noViolations() {
        var request = new TransactionRequest(new BigDecimal("10.00"), OffsetDateTime.now());

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validation_nullAmount_violatesAmount() {
        var request = new TransactionRequest(null, OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void validation_negativeAmount_violatesAmount() {
        var request = new TransactionRequest(
                new BigDecimal("-0.01"),
                OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void validation_nullDateTime_violatesDateTime() {
        var request = new TransactionRequest(new BigDecimal("10.00"), null);

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("dateTime");
    }

    @Test
    void validation_futureDateTime_violatesDateTime() {
        var request = new TransactionRequest(
                new BigDecimal("10.00"),
                OffsetDateTime.now().plusSeconds(1));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("dateTime");
    }
}
