# ADR-0001: In-Memory Transaction Store Design

## Status

Accepted

## Context

The challenge requires an in-memory REST API that:

- Accepts transactions (amount + timestamp) via `POST /transactions`
- Returns statistics (count, sum, avg, min, max) for the **last 60 seconds** via `GET /statistics`
- Deletes all stored data via `DELETE /transactions`
- **Must not** use any database or cache system

Four forces shaped the design:

1. **Runtime** — both `POST /transactions` and `GET /statistics` must be fast; the read/write distribution is unknown, so neither endpoint can be favored at the expense of the other
2. **Memory** — retained data must not grow unboundedly; only what is necessary to answer `GET /statistics` for the active 60-second window needs to be kept
3. **Correctness** — statistics must be exact for the precise 60-second window at the moment of the request
4. **Ordering** — transaction timestamps reflect when a transaction *occurred*, not when it *arrived*; network routing and concurrent request handling mean arrival order and timestamp order can diverge, so the store must handle out-of-order insertions correctly

## Alternatives Considered

### Alternative 1: `LinkedList` + full scan on each read

A `LinkedList` is a natural fit for a sliding time window: evict from the head when the front element falls outside the 60-second cutoff, keeping only the live tail. Eviction would be O(evicted) and `GET /statistics` O(k), where k is the active window size.

This breaks under out-of-order arrivals. A transaction timestamped 90 seconds ago can arrive *after* a transaction timestamped 30 seconds ago due to network routing or concurrent threads. If an older transaction is inserted anywhere other than the head, head-only eviction misses it. Correct eviction then requires a full O(n) scan of the entire list, eliminating the structural advantage.

Beyond eviction, `GET /statistics` must also scan the whole list to filter, aggregate, and find min/max — O(n) per read regardless of window size.

**Rejected** because both eviction and statistics computation are O(n) in total transactions posted once out-of-order arrivals are accounted for.

---

### Alternative 2: Bucket list (time-bucketed aggregation)

Pre-aggregate transactions into fixed-size time buckets (e.g., one per second). Each bucket stores partial statistics `(count, sum, min, max)`. `GET /statistics` merges the 60 most recent buckets.

**Runtime:** Both `POST /transactions` and `GET /statistics` are O(1).

**Memory:** Bounded to a fixed number of buckets.

**Limitation:** The bucket at the window boundary aggregates all transactions that fall within that bucket's time range — including transactions that occurred before the exact 60-second cutoff. Once individual timestamps are discarded, there is no way to exclude them. This makes the statistics incorrect for any request whose cutoff does not align with a bucket boundary.

**Rejected** because it cannot guarantee correct statistics for a given time window.

---

## Decision

A dual-`TreeMap` design with incremental running totals:

```
TreeMap<OffsetDateTime, List<BigDecimal>> window   // time-sorted, for eviction
TreeMap<BigDecimal, Integer>             amounts   // sorted frequency map, for min/max
long                                     count     // running total
BigDecimal                               sum       // running total
```

The store does not retain raw transaction records. It retains only what is required to compute statistics over the active window and to evict expired data.

**Why `TreeMap` for `window`:**
Transactions are sorted by timestamp on insertion, regardless of arrival order. `headMap(cutoff)` correctly isolates all expired entries in O(log k) without scanning live data. The view is backed by the map, so calling `.clear()` on it removes expired entries in place. Each transaction is evicted at most once, making eviction O(log k + evicted) amortized.

**Why `TreeMap` for `amounts`:**
Maintaining a sorted frequency map makes `firstKey()` = min and `lastKey()` = max, both O(1). Without it, computing min/max at read time would require an O(k) scan of the window on every `GET /statistics`. Entries are removed when their frequency count reaches zero, keeping the map tight.

**Why running `count` and `sum`:**
Incrementally updated on every `add()` and eviction. At read time, avg = sum / count, min, max, count, and sum are all available in O(1), eliminating the need to re-aggregate on every `GET /statistics`.

## Consequences

| Operation             | Complexity                   | Notes                                                                          |
|-----------------------|------------------------------|--------------------------------------------------------------------------------|
| `POST /transactions`  | O(log k + evicted) amortized | Eviction runs before insert; k = transactions in the active window             |
| `GET /statistics`     | O(log k + evicted) amortized | Eviction runs first; aggregation reads are O(1)                                |
| `DELETE /transaction` | O(1) assignment              | New map instances are assigned; old data is reclaimed by the garbage collector |

- Memory is bounded to the active 60-second window
- Statistics are always exact for the precise cutoff at request time
- Sorted insertion handles out-of-order arrivals correctly
- All four structures must be kept in sync on every write and eviction — this is the main maintenance burden of the design
