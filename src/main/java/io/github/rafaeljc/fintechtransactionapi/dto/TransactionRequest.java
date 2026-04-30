package io.github.rafaeljc.fintechtransactionapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionRequest(
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull @PastOrPresent OffsetDateTime dateTime
) {}
