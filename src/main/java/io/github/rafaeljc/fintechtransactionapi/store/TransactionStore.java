package io.github.rafaeljc.fintechtransactionapi.store;

import io.github.rafaeljc.fintechtransactionapi.dto.StatisticsResponse;
import io.github.rafaeljc.fintechtransactionapi.model.Transaction;
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

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private volatile TreeMap<OffsetDateTime, List<BigDecimal>> window = new TreeMap<>();
    private volatile TreeMap<BigDecimal, Integer> amounts = new TreeMap<>();
    private long count = 0;
    private BigDecimal sum = BigDecimal.ZERO;

    public void add(Transaction t) {
        lock.writeLock().lock();
        try {
            evict();
            window.computeIfAbsent(t.dateTime(), k -> new ArrayList<>()).add(t.amount());
            count++;
            sum = sum.add(t.amount());
            amounts.merge(t.amount(), 1, Integer::sum);
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
            window = new TreeMap<>();
            amounts = new TreeMap<>();
            count = 0;
            sum = BigDecimal.ZERO;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void evict() {
        lock.writeLock().lock();
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(60);
            Map<OffsetDateTime, List<BigDecimal>> expired = window.headMap(cutoff);
            for (Map.Entry<OffsetDateTime, List<BigDecimal>> entry : expired.entrySet()) {
                for (BigDecimal amount : entry.getValue()) {
                    count--;
                    sum = sum.subtract(amount);
                    amounts.computeIfPresent(amount, (k, v) -> v == 1 ? null : v - 1);
                }
            }
            expired.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
