package io.github.ransombiyato.dicebox.sim;

import java.util.Objects;

/**
 * An immutable digital signal value of a fixed bit width (1..64).
 *
 * <p>Each bit is one of {@link Bit#ZERO}, {@link Bit#ONE}, or
 * {@link Bit#UNKNOWN}. Unknown covers "not yet driven", "floating", and
 * "result of a driver conflict"; callers that need to distinguish those causes
 * track them separately (see {@link NetResolver}).
 *
 * <p>Logic operations propagate unknowns per bit using the usual three-valued
 * rules: {@code 0 AND X = 0}, {@code 1 OR X = 1}, everything else involving an
 * unknown stays unknown. This is a deliberate, documented simplification: it
 * is not an analog or high-impedance simulation.
 *
 * <p>Internal representation: {@code bits} holds the known 1-bits,
 * {@code unknownMask} holds the unknown bits. Invariant:
 * {@code (bits & unknownMask) == 0} and neither has bits set outside
 * {@code width}.
 */
public final class SignalValue {
    /** The state of a single bit of a signal. */
    public enum Bit {
        ZERO,
        ONE,
        UNKNOWN
    }

    public static final int MAX_WIDTH = 64;

    private final int width;
    private final long bits;
    private final long unknownMask;

    private SignalValue(int width, long bits, long unknownMask) {
        long mask = mask(width);
        if ((bits & ~mask) != 0L || (unknownMask & ~mask) != 0L) {
            throw new IllegalArgumentException(
                    "bits/unknownMask have bits set outside width " + width);
        }
        if ((bits & unknownMask) != 0L) {
            throw new IllegalArgumentException("a bit cannot be both known-1 and unknown");
        }
        this.width = width;
        this.bits = bits;
        this.unknownMask = unknownMask;
    }

    /** Returns a mask with the low {@code width} bits set. Validates width. */
    static long mask(int width) {
        if (width < 1 || width > MAX_WIDTH) {
            throw new IllegalArgumentException("width must be in 1.." + MAX_WIDTH + ": " + width);
        }
        return width == MAX_WIDTH ? -1L : (1L << width) - 1L;
    }

    /** A fully-known value. {@code value} is truncated to {@code width} bits. */
    public static SignalValue known(int width, long value) {
        return new SignalValue(width, value & mask(width), 0L);
    }

    /** A value whose every bit is unknown. */
    public static SignalValue unknown(int width) {
        return new SignalValue(width, 0L, mask(width));
    }

    /** A value from explicit known-1 bits and unknown bits (must not overlap). */
    public static SignalValue of(int width, long bits, long unknownMask) {
        return new SignalValue(width, bits, unknownMask);
    }

    public int width() {
        return width;
    }

    /** The known 1-bits. Unknown bits read as 0 here; check {@link #unknownMask()}. */
    public long bits() {
        return bits;
    }

    /** Mask of bits whose state is {@link Bit#UNKNOWN}. */
    public long unknownMask() {
        return unknownMask;
    }

    public boolean isFullyKnown() {
        return unknownMask == 0L;
    }

    /** The numeric value; fails if any bit is unknown. */
    public long requireKnownValue() {
        if (!isFullyKnown()) {
            throw new IllegalStateException("signal has unknown bits: " + this);
        }
        return bits;
    }

    /** State of bit {@code index} (0 = least significant). */
    public Bit bit(int index) {
        if (index < 0 || index >= width) {
            throw new IndexOutOfBoundsException("bit index " + index + " for width " + width);
        }
        long m = 1L << index;
        if ((unknownMask & m) != 0L) {
            return Bit.UNKNOWN;
        }
        return (bits & m) != 0L ? Bit.ONE : Bit.ZERO;
    }

    private long ones() {
        return bits;
    }

    private long zeros() {
        return ~bits & ~unknownMask & mask(width);
    }

    private void requireSameWidth(SignalValue other) {
        Objects.requireNonNull(other, "other");
        if (other.width != this.width) {
            throw new IllegalArgumentException(
                    "signal width mismatch: " + this.width + " vs " + other.width);
        }
    }

    public SignalValue and(SignalValue other) {
        requireSameWidth(other);
        long ones = this.ones() & other.ones();
        long zeros = this.zeros() | other.zeros();
        long unknown = ~(ones | zeros) & mask(width);
        return new SignalValue(width, ones, unknown);
    }

    public SignalValue or(SignalValue other) {
        requireSameWidth(other);
        long ones = this.ones() | other.ones();
        long zeros = this.zeros() & other.zeros();
        long unknown = ~(ones | zeros) & mask(width);
        return new SignalValue(width, ones, unknown);
    }

    public SignalValue xor(SignalValue other) {
        requireSameWidth(other);
        long ones = (this.ones() & other.zeros()) | (this.zeros() & other.ones());
        long zeros = (this.ones() & other.ones()) | (this.zeros() & other.zeros());
        long unknown = ~(ones | zeros) & mask(width);
        return new SignalValue(width, ones, unknown);
    }

    public SignalValue not() {
        return new SignalValue(width, zeros(), unknownMask);
    }

    /**
     * Resizes to {@code newWidth}: truncates high bits when shrinking, and
     * zero-extends with known 0 bits when growing.
     */
    public SignalValue resize(int newWidth) {
        long m = mask(newWidth);
        return new SignalValue(newWidth, bits & m, unknownMask & m);
    }

    /**
     * Compact, stable text encoding: {@code width:bits:unknownMask}.
     * Used as the persistence building block for signal state.
     */
    public String encode() {
        return width + ":" + bits + ":" + unknownMask;
    }

    /** Inverse of {@link #encode()}. */
    public static SignalValue decode(String encoded) {
        Objects.requireNonNull(encoded, "encoded");
        String[] parts = encoded.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid SignalValue encoding: " + encoded);
        }
        try {
            int w = Integer.parseInt(parts[0]);
            long b = Long.parseLong(parts[1]);
            long u = Long.parseLong(parts[2]);
            return new SignalValue(w, b, u);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid SignalValue encoding: " + encoded, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SignalValue other)) {
            return false;
        }
        return width == other.width && bits == other.bits && unknownMask == other.unknownMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, bits, unknownMask);
    }

    /** Verilog-style rendering, most significant bit first, e.g. {@code 4'b10X1}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(width + 3);
        sb.append(width).append("'b");
        for (int i = width - 1; i >= 0; i--) {
            switch (bit(i)) {
                case ZERO -> sb.append('0');
                case ONE -> sb.append('1');
                case UNKNOWN -> sb.append('X');
            }
        }
        return sb.toString();
    }
}
