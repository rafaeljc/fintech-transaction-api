package io.github.rafaeljc.fintechtransactionapi.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PositiveDurationValidatorTest {

    private final PositiveDurationValidator validator = new PositiveDurationValidator();

    @Test
    void positiveDurationIsValid() {
        assertThat(validator.isValid(Duration.ofSeconds(30), null)).isTrue();
    }

    @Test
    void zeroDurationIsInvalid() {
        assertThat(validator.isValid(Duration.ZERO, null)).isFalse();
    }

    @Test
    void negativeDurationIsInvalid() {
        assertThat(validator.isValid(Duration.ofSeconds(-1), null)).isFalse();
    }

    @Test
    void nullIsValid() {
        // null handling is delegated to @NotNull on the annotated element
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
