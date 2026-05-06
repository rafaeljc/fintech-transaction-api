package io.github.rafaeljc.fintechtransactionapi.store;

import io.github.rafaeljc.fintechtransactionapi.config.StatisticsProperties;
import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStoreTest {

    private TransactionStore store;

    @BeforeEach
    void setUp() {
        store = new TransactionStore(new StatisticsProperties(Duration.ofSeconds(60)));
    }

    @Test
    void statisticsEmptyStoreReturnsAllZeros() {
        StatisticsResponse stats = store.statistics();

        assertThat(stats.count()).isEqualTo(0L);
        assertThat(stats.sum()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.avg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.min()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.max()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void statisticsTransactionsWithinWindowReturnsCorrectAggregates() {
        OffsetDateTime now = OffsetDateTime.now();
        store.add(new Transaction(new BigDecimal("10.00"), now));
        store.add(new Transaction(new BigDecimal("20.00"), now));
        store.add(new Transaction(new BigDecimal("30.00"), now));

        StatisticsResponse stats = store.statistics();

        assertThat(stats.count()).isEqualTo(3L);
        assertThat(stats.sum()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(stats.avg()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(stats.min()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(stats.max()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void statisticsExpiredTransactionsExcludedFromAggregates() {
        OffsetDateTime expired = OffsetDateTime.now().minusSeconds(61);
        store.add(new Transaction(new BigDecimal("100.00"), expired));
        store.add(new Transaction(new BigDecimal("5.00"), OffsetDateTime.now()));

        StatisticsResponse stats = store.statistics();

        assertThat(stats.count()).isEqualTo(1L);
        assertThat(stats.sum()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(stats.avg()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(stats.min()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(stats.max()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void statisticsAfterClearReturnsAllZeros() {
        store.add(new Transaction(new BigDecimal("50.00"), OffsetDateTime.now()));
        store.clear();

        StatisticsResponse stats = store.statistics();

        assertThat(stats.count()).isEqualTo(0L);
        assertThat(stats.sum()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.avg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.min()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.max()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void addSameAmountFrequencyTrackedCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        store.add(new Transaction(new BigDecimal("10.00"), now));
        store.add(new Transaction(new BigDecimal("10.00"), now));

        StatisticsResponse stats = store.statistics();

        assertThat(stats.count()).isEqualTo(2L);
        assertThat(stats.sum()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(stats.avg()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(stats.min()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(stats.max()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void evictionRemovesAmountKeyWhenFrequencyReachesZero() throws InterruptedException {
        store.add(new Transaction(new BigDecimal("99.00"), OffsetDateTime.now().minusSeconds(55)));
        store.add(new Transaction(new BigDecimal("5.00"), OffsetDateTime.now()));

        StatisticsResponse before = store.statistics();
        assertThat(before.count()).isEqualTo(2L);
        assertThat(before.min()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(before.max()).isEqualByComparingTo(new BigDecimal("99.00"));

        Thread.sleep(6_000);

        StatisticsResponse after = store.statistics();
        assertThat(after.count()).isEqualTo(1L);
        assertThat(after.min()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(after.max()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void concurrentOperationsDoNotCorruptState() throws InterruptedException {
        int adderThreads = 5;
        int readerThreads = 5;
        int totalThreads = adderThreads + readerThreads + 1;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicBoolean inconsistencyDetected = new AtomicBoolean(false);

        for (int i = 0; i < adderThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        store.add(new Transaction(new BigDecimal("1.00"), OffsetDateTime.now()));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < readerThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        StatisticsResponse stats = store.statistics();
                        if (stats.count() < 0 || stats.sum().compareTo(BigDecimal.ZERO) < 0) {
                            inconsistencyDetected.set(true);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        executor.submit(() -> {
            try {
                store.clear();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        assertThat(inconsistencyDetected.get()).isFalse();
        StatisticsResponse finalStats = store.statistics();
        assertThat(finalStats.count()).isGreaterThanOrEqualTo(0L);
        assertThat(finalStats.sum()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
}
