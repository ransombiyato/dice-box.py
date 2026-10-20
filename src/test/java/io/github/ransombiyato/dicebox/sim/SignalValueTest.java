package io.github.ransombiyato.dicebox.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SignalValueTest {
    @Test
    void knownValueIsTruncatedToWidth() {
        SignalValue v = SignalValue.known(4, 0b1_1111L);
        assertEquals(15L, v.requireKnownValue());
        assertTrue(v.isFullyKnown());
        assertEquals(4, v.width());
    }

    @Test
    void invalidWidthsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SignalValue.known(0, 0L));
        assertThrows(IllegalArgumentException.class, () -> SignalValue.unknown(65));
    }

    @Test
    void andWithKnownZeroAbsorbsUnknown() {
        // 0 AND X = 0
        assertEquals(
                SignalValue.known(1, 0L),
                SignalValue.known(1, 0L).and(SignalValue.unknown(1)));
        // 1 AND X = X
        assertEquals(
                SignalValue.Bit.UNKNOWN,
                SignalValue.known(1, 1L).and(SignalValue.unknown(1)).bit(0));
    }

    @Test
    void orWithKnownOneAbsorbsUnknown() {
        // 1 OR X = 1
        assertEquals(
                SignalValue.known(1, 1L),
                SignalValue.known(1, 1L).or(SignalValue.unknown(1)));
        // 0 OR X = X
        assertEquals(
                SignalValue.Bit.UNKNOWN,
                SignalValue.known(1, 0L).or(SignalValue.unknown(1)).bit(0));
    }

    @Test
    void xorAndNotOnKnownValues() {
        assertEquals(
                SignalValue.known(4, 0b0110L),
                SignalValue.known(4, 0b1100L).xor(SignalValue.known(4, 0b1010L)));
        assertEquals(SignalValue.known(4, 0b0101L), SignalValue.known(4, 0b1010L).not());
        assertEquals(SignalValue.unknown(3), SignalValue.unknown(3).not());
    }

    @Test
    void mixedUnknownPropagationIsPerBit() {
        // a = 1X01, b = 110X -> AND = 1?0? computed per bit:
        // bit3: 1&1=1, bit2: X&1=X, bit1: 0&0=0, bit0: 1&X=X  =>  bits=1000, unknown=0101
        SignalValue a = SignalValue.of(4, 0b1001L, 0b0100L);
        SignalValue b = SignalValue.of(4, 0b1100L, 0b0001L);
        assertEquals(SignalValue.of(4, 0b1000L, 0b0101L), a.and(b));
    }

    @Test
    void resizeTruncatesAndZeroExtends() {
        assertEquals(SignalValue.known(4, 15L), SignalValue.known(8, 0xFFL).resize(4));
        SignalValue extended = SignalValue.of(2, 0b10L, 0b01L).resize(4);
        assertEquals(SignalValue.of(4, 0b0010L, 0b0001L), extended);
        assertEquals(SignalValue.Bit.ZERO, extended.bit(3));
    }

    @Test
    void encodeDecodeRoundTrips() {
        SignalValue v = SignalValue.of(8, 0b1010L, 0b0101L);
        assertEquals(v, SignalValue.decode(v.encode()));
        assertThrows(IllegalArgumentException.class, () -> SignalValue.decode("not:a:signal"));
        assertThrows(IllegalArgumentException.class, () -> SignalValue.decode("8:1"));
    }

    @Test
    void widthMismatchAndUnknownValueAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SignalValue.known(2, 0L).and(SignalValue.known(3, 0L)));
        assertThrows(IllegalStateException.class, () -> SignalValue.unknown(2).requireKnownValue());
        assertFalse(SignalValue.unknown(2).isFullyKnown());
    }

    @Test
    void toStringShowsUnknownBits() {
        assertEquals("4'b1X00", SignalValue.of(4, 0b1000L, 0b0100L).toString());
    }
}
