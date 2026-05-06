package io.github.rafaeljc.fintechtransactionapi.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsPropertiesTest {

    @SpringBootTest
    @Nested
    class WhenNoPropertySet {

        @Autowired
        StatisticsProperties props;

        @Test
        void defaultValueIs60Seconds() {
            assertThat(props.lookbackDuration()).isEqualTo(Duration.ofSeconds(60));
        }
    }

    @SpringBootTest
    @TestPropertySource(properties = "statistics.lookback-duration=30s")
    @Nested
    class WhenCustomValueSet {

        @Autowired
        StatisticsProperties props;

        @Test
        void customValidValueIsAccepted() {
            assertThat(props.lookbackDuration()).isEqualTo(Duration.ofSeconds(30));
        }
    }

    // ApplicationContextRunner is used here instead of @SpringBootTest to avoid starting a full
    // application context for expected startup failures, keeping these tests fast.
    @Nested
    class WhenInvalidValueSet {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfig.class);

        @Configuration
        @EnableConfigurationProperties(StatisticsProperties.class)
        static class PropertiesConfig {
        }

        @Test
        void zeroDurationFailsStartup() {
            contextRunner
                    .withPropertyValues("statistics.lookback-duration=0s")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void negativeDurationFailsStartup() {
            contextRunner
                    .withPropertyValues("statistics.lookback-duration=-1s")
                    .run(context -> assertThat(context).hasFailed());
        }
    }
}
