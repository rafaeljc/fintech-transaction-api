package io.github.rafaeljc.fintechtransactionapi.dto;

import java.math.BigDecimal;

public record StatisticsResponse(
        long count, BigDecimal sum, BigDecimal avg, BigDecimal min, BigDecimal max) {

    public static StatisticsResponse empty() {
        return new StatisticsResponse(
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
