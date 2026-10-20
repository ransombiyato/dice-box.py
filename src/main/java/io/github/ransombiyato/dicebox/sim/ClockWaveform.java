package io.github.ransombiyato.dicebox.sim;

/**
 * A periodic digital clock waveform over simulation time.
 *
 * <p>The waveform is high for {@code highDuration} time units and low for the
 * remainder of each {@code period}, offset by {@code phase}. It is a pure
 * function of time — it owns no mutable state and schedules nothing — so clock
 * generators (a later component) and clocked sequential logic can sample it
 * deterministically, and tests can assert exact edge times.
 */
public final class ClockWaveform {
    private final long period;
    private final long highDuration;
    private final long phase;

    public ClockWaveform(long period, long highDuration, long phase) {
        if (period < 2L) {
            throw new IllegalArgumentException("period must be >= 2: " + period);
        }
        if (highDuration <= 0L || highDuration >= period) {
            throw new IllegalArgumentException(
                    "highDuration must be in 1.." + (period - 1) + ": " + highDuration);
        }
        if (phase < 0L) {
            throw new IllegalArgumentException("phase must be >= 0: " + phase);
        }
        this.period = period;
        this.highDuration = highDuration;
        this.phase = phase;
    }

    /** A 50%-duty-cycle clock with the given period (must be &ge; 2 and even to be exact). */
    public static ClockWaveform withPeriod(long period) {
        return new ClockWaveform(period, period / 2L, 0L);
    }

    public long period() {
        return period;
    }

    public long highDuration() {
        return highDuration;
    }

    public long phase() {
        return phase;
    }

    private long positionInPeriod(long time) {
        if (time < 0L) {
            throw new IllegalArgumentException("time must be >= 0: " + time);
        }
        return Math.floorMod(time - phase, period);
    }

    /** Whether the clock is high at {@code time}. The rising-edge instant itself is high. */
    public boolean isHighAt(long time) {
        return positionInPeriod(time) < highDuration;
    }

    /**
     * The first time strictly after {@code time} at which the level changes.
     * Both rising and falling edges count.
     */
    public long nextEdgeAfter(long time) {
        long position = positionInPeriod(time);
        long delta = (position < highDuration) ? (highDuration - position) : (period - position);
        return time + delta;
    }
}
