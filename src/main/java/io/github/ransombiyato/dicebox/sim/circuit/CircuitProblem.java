package io.github.ransombiyato.dicebox.sim.circuit;

import java.util.Objects;

/**
 * One structural problem found by {@link Circuit#validate()}.
 *
 * <p>Problems describe circuits that can be <i>constructed</i> (building a
 * circuit is incremental) but are suspicious or incomplete. Evaluation still
 * has defined behavior for every kind here — unconnected inputs read as
 * unknown, undriven nets float, multiple drivers resolve via
 * {@code NetResolver} — so these are diagnostics for tools and players, not
 * fatal errors. Hard API misuse (unknown ids, width mismatches at connect
 * time, duplicates) fails immediately with an exception instead.
 */
public record CircuitProblem(Kind kind, String message) {
    public enum Kind {
        /** A component terminal is not connected to any net. */
        UNCONNECTED_TERMINAL,
        /** A net has no component driver and is not declared an external input. */
        UNDRIVEN_NET,
        /** More than one component terminal drives the same net. */
        MULTIPLE_DRIVERS
    }

    public CircuitProblem {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
