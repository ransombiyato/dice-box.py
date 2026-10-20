package io.github.ransombiyato.dicebox.sim;

import java.util.Objects;

/**
 * A typed connection point on a simulated component: a stable name, a
 * direction, and a signal width in bits (1 = single-bit signal, &gt;1 = bus).
 *
 * <p>Terminals describe <i>logical</i> connectivity only. Physical position on
 * a block face is a separate concern owned by the future surface-placement
 * system; a terminal's identity does not change when its component moves.
 */
public record Terminal(String name, Direction direction, int width) {
    /** Signal flow direction relative to the owning component. */
    public enum Direction {
        INPUT,
        OUTPUT,
        BIDIRECTIONAL
    }

    public Terminal {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(direction, "direction");
        if (name.isBlank()) {
            throw new IllegalArgumentException("terminal name must not be blank");
        }
        if (width < 1 || width > SignalValue.MAX_WIDTH) {
            throw new IllegalArgumentException(
                    "terminal width must be in 1.." + SignalValue.MAX_WIDTH + ": " + width);
        }
    }

    /** Whether this terminal can drive a signal onto a net. */
    public boolean canDrive() {
        return direction != Direction.INPUT;
    }

    /** Whether this terminal can accept a signal from a net. */
    public boolean canAccept() {
        return direction != Direction.OUTPUT;
    }
}
