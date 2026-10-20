package io.github.ransombiyato.dicebox.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NetResolverTest {
    @Test
    void noDriversMeansFloating() {
        NetResolver.Resolution r = NetResolver.resolve(4, List.of());
        assertTrue(r.floating());
        assertFalse(r.conflict());
        assertEquals(SignalValue.unknown(4), r.value());
    }

    @Test
    void agreeingDriversResolveToTheirValue() {
        NetResolver.Resolution r =
                NetResolver.resolve(2, List.of(SignalValue.known(2, 1L), SignalValue.known(2, 1L)));
        assertFalse(r.conflict());
        assertFalse(r.floating());
        assertEquals(SignalValue.known(2, 1L), r.value());
    }

    @Test
    void knownDriverWinsOverUnknownDriver() {
        NetResolver.Resolution r =
                NetResolver.resolve(2, List.of(SignalValue.unknown(2), SignalValue.known(2, 2L)));
        assertFalse(r.conflict());
        assertEquals(SignalValue.known(2, 2L), r.value());
    }

    @Test
    void disagreeingDriversConflictAndYieldUnknown() {
        NetResolver.Resolution r =
                NetResolver.resolve(1, List.of(SignalValue.known(1, 0L), SignalValue.known(1, 1L)));
        assertTrue(r.conflict());
        assertFalse(r.floating());
        assertEquals(SignalValue.Bit.UNKNOWN, r.value().bit(0));
    }

    @Test
    void conflictIsPerBit() {
        // bit1 agrees (1,1); bit0 conflicts (0 vs 1)
        NetResolver.Resolution r =
                NetResolver.resolve(
                        2, List.of(SignalValue.known(2, 0b10L), SignalValue.known(2, 0b11L)));
        assertTrue(r.conflict());
        assertEquals(SignalValue.of(2, 0b10L, 0b01L), r.value());
    }

    @Test
    void driverWidthMismatchIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NetResolver.resolve(4, List.of(SignalValue.known(2, 0L))));
    }
}
