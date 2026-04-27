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
    void validationValidRequestNoViolations() {
        var request = new TransactionRequest(
                new BigDecimal("10.00"),
                OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validationZeroAmountNoViolations() {
        var request = new TransactionRequest(
                BigDecimal.ZERO,
                OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validationPresentDateTimeNoViolations() {
        var request = new TransactionRequest(new BigDecimal("10.00"), OffsetDateTime.now());

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validationNullAmountViolatesAmount() {
        var request = new TransactionRequest(
                null, OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void validationNegativeAmountViolatesAmount() {
        var request = new TransactionRequest(
                new BigDecimal("-0.01"),
                OffsetDateTime.parse("2024-06-01T10:00:00+00:00"));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void validationNullDateTimeViolatesDateTime() {
        var request = new TransactionRequest(new BigDecimal("10.00"), null);

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("dateTime");
    }

    @Test
    void validationFutureDateTimeViolatesDateTime() {
        var request = new TransactionRequest(
                new BigDecimal("10.00"),
                OffsetDateTime.now().plusSeconds(1));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("dateTime");
    }
}
