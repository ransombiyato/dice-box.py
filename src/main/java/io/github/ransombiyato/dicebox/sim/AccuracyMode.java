package io.github.ransombiyato.dicebox.sim;

/**
 * Configurable simulation accuracy levels.
 *
 * <p>These modes describe <i>intent</i> and cost trade-offs for the engine.
 * Only the behavior actually implemented and tested at a given milestone may
 * be claimed in documentation — in particular, nothing in this codebase is
 * cycle-accurate until architecture-specific cores exist with reference-vector
 * tests (see docs/ROADMAP.md). Selecting {@link #CYCLE_ACCURATE} today changes
 * no behavior beyond the values exposed here.
 */
public enum AccuracyMode {
    /** Simplified behavior, accessible diagnostics, lowest cost. Zero-delay combinational view. */
    EDUCATIONAL(0),
    /** Correct logic and sequential behavior with configurable timing. */
    FUNCTIONAL(1),
    /** Scheduled propagation delays and clock behavior are modeled. */
    TIMING_AWARE(1),
    /** Architecture-specific cycles, buses and interrupts. Long-term target only. */
    CYCLE_ACCURATE(1);

    private final int defaultGateDelay;

    AccuracyMode(int defaultGateDelay) {
        this.defaultGateDelay = defaultGateDelay;
    }

    /** Default propagation delay, in simulation time units, for a simple gate in this mode. */
    public int defaultGateDelay() {
        return defaultGateDelay;
    }

    /** Whether this mode schedules propagation delays through the event scheduler. */
    public boolean schedulesPropagation() {
        return defaultGateDelay > 0;
    }
}
