package io.github.ransombiyato.dicebox.sim;

import java.util.Collection;
import java.util.Objects;

/**
 * Resolves the value of a net (a wire or bus) from all of its drivers.
 *
 * <p>Defined behavior, per bit:
 *
 * <ul>
 *   <li><b>No drivers at all</b> → the net is <i>floating</i>; every bit is
 *       unknown.
 *   <li><b>Drivers disagree</b> (one drives 0, another drives 1) → the bit is
 *       unknown and the resolution is flagged as a <i>conflict</i>, so
 *       diagnostics can report it instead of silently picking a winner.
 *   <li><b>Known drivers agree, others unknown</b> → the known value wins.
 *   <li><b>Every driver unknown</b> → unknown, but not a conflict.
 * </ul>
 *
 * This is a logical resolution model, not an electrical one: no drive
 * strengths, no analog voltages.
 */
public final class NetResolver {
    /** The outcome of resolving one net. */
    public record Resolution(SignalValue value, boolean conflict, boolean floating) {
        public Resolution {
            Objects.requireNonNull(value, "value");
        }
    }

    private NetResolver() {}

    /**
     * Resolves a net of the given width from its drivers' current values.
     * Every driver value must have exactly {@code width} bits.
     */
    public static Resolution resolve(int width, Collection<SignalValue> drivers) {
        Objects.requireNonNull(drivers, "drivers");
        // Validates width (1..64) via SignalValue.
        SignalValue allUnknown = SignalValue.unknown(width);

        if (drivers.isEmpty()) {
            return new Resolution(allUnknown, false, true);
        }
        for (SignalValue driver : drivers) {
            Objects.requireNonNull(driver, "driver");
            if (driver.width() != width) {
                throw new IllegalArgumentException(
                        "driver width " + driver.width() + " does not match net width " + width);
            }
        }

        long bits = 0L;
        long unknown = 0L;
        boolean conflict = false;
        for (int i = 0; i < width; i++) {
            boolean sawZero = false;
            boolean sawOne = false;
            for (SignalValue driver : drivers) {
                switch (driver.bit(i)) {
                    case ZERO -> sawZero = true;
                    case ONE -> sawOne = true;
                    case UNKNOWN -> {
                        // An unknown driver neither wins nor conflicts.
                    }
                }
            }
            if (sawZero && sawOne) {
                conflict = true;
                unknown |= 1L << i;
            } else if (sawOne) {
                bits |= 1L << i;
            } else if (!sawZero) {
                unknown |= 1L << i;
            }
            // sawZero only: bit stays a known 0.
        }
        return new Resolution(SignalValue.of(width, bits, unknown), conflict, false);
    }
}
