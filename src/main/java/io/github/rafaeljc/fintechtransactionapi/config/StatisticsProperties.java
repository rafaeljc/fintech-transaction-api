package io.github.rafaeljc.fintechtransactionapi.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "statistics")
public record StatisticsProperties(
        @DefaultValue("60s") @NotNull @PositiveDuration Duration lookbackDuration) {
}
