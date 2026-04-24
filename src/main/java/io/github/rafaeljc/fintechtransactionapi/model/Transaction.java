package io.github.rafaeljc.fintechtransactionapi.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Transaction(BigDecimal amount, OffsetDateTime dateTime) {}
