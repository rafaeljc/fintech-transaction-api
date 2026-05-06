package io.github.rafaeljc.fintechtransactionapi.store;

import io.github.rafaeljc.fintechtransactionapi.config.StatisticsProperties;
import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class TransactionStore {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionStore.class);

    // every method that reads/writes instance data must acquire this lock;
    // reentrant to prevent deadlock when a locked method calls another that also requires the lock;
    // read lock can only be used if no other locked method is called —
    //   acquiring a write lock while holding a read lock deadlocks;
    // fair mode (true) prevents writer starvation
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final StatisticsProperties properties;
    private volatile TreeMap<OffsetDateTime, List<BigDecimal>> window = new TreeMap<>();
    private volatile TreeMap<BigDecimal, Integer> amounts = new TreeMap<>();
    private long count = 0;
    private BigDecimal sum = BigDecimal.ZERO;

    public TransactionStore(StatisticsProperties properties) {
        this.properties = properties;
    }

    public void add(Transaction t) {
        lock.writeLock().lock();
        try {
            evict();
            window.computeIfAbsent(t.dateTime(), k -> new ArrayList<>()).add(t.amount());
            count++;
            sum = sum.add(t.amount());
            amounts.merge(t.amount(), 1, Integer::sum);
            LOG.debug("Transaction added: amount={}, windowSize={}", t.amount(), count);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public StatisticsResponse statistics() {
        lock.writeLock().lock();
        try {
            evict();
            if (count == 0) {
                return StatisticsResponse.empty();
            }
            BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            return new StatisticsResponse(count, sum, avg, amounts.firstKey(), amounts.lastKey());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            long removed = count;
            // assigning a new instance is O(1); .clear() is O(n) — let GC reclaim the old data
            window = new TreeMap<>();
            amounts = new TreeMap<>();
            count = 0;
            sum = BigDecimal.ZERO;
            LOG.debug("Transaction store cleared: removedCount={}", removed);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // must be called at the start of any public method that reads/writes instance data
    // for lazy eviction
    private void evict() {
        lock.writeLock().lock();
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minus(properties.lookbackDuration());
            Map<OffsetDateTime, List<BigDecimal>> expired = window.headMap(cutoff);
            long before = count;
            for (Map.Entry<OffsetDateTime, List<BigDecimal>> entry : expired.entrySet()) {
                for (BigDecimal amount : entry.getValue()) {
                    count--;
                    sum = sum.subtract(amount);
                    amounts.computeIfPresent(amount, (k, v) -> v == 1 ? null : v - 1);
                }
            }
            expired.clear();
            if (count < before) {
                LOG.debug("Evicted expired transactions: evictedCount={}", before - count);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
