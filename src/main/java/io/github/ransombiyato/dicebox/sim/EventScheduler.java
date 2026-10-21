package io.github.ransombiyato.dicebox.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.PriorityQueue;

/**
 * A deterministic discrete-event scheduler over a monotonic {@code long} time
 * line (abstract simulation time units; the mapping to real time and to
 * Minecraft ticks is owned by higher layers).
 *
 * <p>Ordering guarantees: events are processed in increasing time; events at
 * the same time are processed in the order they were scheduled (FIFO by an
 * internal sequence number). Given the same sequence of {@code schedule}
 * calls, processing order is fully reproducible. There is no randomness and
 * no use of wall-clock time anywhere in this class.
 *
 * <p>Work is always bounded by the caller: {@link #runUntil} processes at most
 * {@code maxEvents} events, which is how the engine will enforce per-tick
 * simulation budgets instead of ever running an unbounded loop on a server
 * thread.
 *
 * @param <T> event payload type
 */
public final class EventScheduler<T> {
    private static final class Entry<T> implements Comparable<Entry<T>> {
        final long time;
        final long sequence;
        final T payload;

        Entry(long time, long sequence, T payload) {
            this.time = time;
            this.sequence = sequence;
            this.payload = payload;
        }

        @Override
        public int compareTo(Entry<T> other) {
            int byTime = Long.compare(this.time, other.time);
            return byTime != 0 ? byTime : Long.compare(this.sequence, other.sequence);
        }
    }

    private final PriorityQueue<Entry<T>> queue = new PriorityQueue<>();
    private long now = 0L;
    private long nextSequence = 0L;

    /** Creates a scheduler starting at time {@code 0}. */
    public EventScheduler() {
        this(0L);
    }

    /**
     * Creates a scheduler starting at {@code initialTime}. Exists so a
     * simulator snapshot can be restored at the time it was taken; ordinary
     * callers should prefer {@link #EventScheduler()}.
     */
    public EventScheduler(long initialTime) {
        if (initialTime < 0L) {
            throw new IllegalArgumentException("initialTime must be >= 0: " + initialTime);
        }
        this.now = initialTime;
    }

    /** Current simulation time. Never moves backwards. */
    public long now() {
        return now;
    }

    /** Number of scheduled-but-not-yet-processed events. */
    public int pendingCount() {
        return queue.size();
    }

    /** Time of the next pending event, if any. */
    public OptionalLong nextEventTime() {
        Entry<T> next = queue.peek();
        return next == null ? OptionalLong.empty() : OptionalLong.of(next.time);
    }

    /** Schedules {@code payload} at an absolute time, which must not be in the past. */
    public void scheduleAt(long time, T payload) {
        Objects.requireNonNull(payload, "payload");
        if (time < now) {
            throw new IllegalArgumentException(
                    "cannot schedule in the past: time=" + time + " now=" + now);
        }
        queue.add(new Entry<>(time, nextSequence++, payload));
    }

    /** Schedules {@code payload} {@code delay} time units from now. */
    public void scheduleAfter(long delay, T payload) {
        if (delay < 0L) {
            throw new IllegalArgumentException("delay must be >= 0: " + delay);
        }
        scheduleAt(now + delay, payload);
    }

    /**
     * Processes the single next event, advancing {@link #now()} to its time.
     * Returns empty (and does not move time) if no event is pending.
     */
    public Optional<T> runNext() {
        Entry<T> next = queue.poll();
        if (next == null) {
            return Optional.empty();
        }
        now = next.time;
        return Optional.of(next.payload);
    }

    /**
     * Processes events scheduled at or before {@code endTime}, at most
     * {@code maxEvents} of them, and returns their payloads in processing
     * order.
     *
     * <p>If the budget is exhausted while due events remain, {@link #now()}
     * stops at the time of the last processed event and the rest stay queued —
     * the caller can resume on the next budget slice. If no due events remain,
     * {@link #now()} advances to {@code endTime} (the simulation was idle for
     * the remainder of the interval).
     */
    public List<T> runUntil(long endTime, int maxEvents) {
        if (endTime < now) {
            throw new IllegalArgumentException("endTime " + endTime + " is before now " + now);
        }
        if (maxEvents < 1) {
            throw new IllegalArgumentException("maxEvents must be >= 1: " + maxEvents);
        }
        List<T> processed = new ArrayList<>();
        while (processed.size() < maxEvents) {
            Entry<T> next = queue.peek();
            if (next == null || next.time > endTime) {
                break;
            }
            queue.poll();
            now = next.time;
            processed.add(next.payload);
        }
        Entry<T> remaining = queue.peek();
        if (remaining == null || remaining.time > endTime) {
            now = endTime;
        }
        return List.copyOf(processed);
    }
}
