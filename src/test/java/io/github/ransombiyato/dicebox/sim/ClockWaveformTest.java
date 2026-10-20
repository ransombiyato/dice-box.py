package io.github.ransombiyato.dicebox.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClockWaveformTest {
    @Test
    void fiftyPercentDutyCycle() {
        ClockWaveform clock = ClockWaveform.withPeriod(10L);
        for (long t = 0L; t < 5L; t++) {
            assertTrue(clock.isHighAt(t), "expected high at " + t);
        }
        for (long t = 5L; t < 10L; t++) {
            assertFalse(clock.isHighAt(t), "expected low at " + t);
        }
        assertTrue(clock.isHighAt(10L));
    }

    @Test
    void nextEdgeFindsBothTransitions() {
        ClockWaveform clock = ClockWaveform.withPeriod(10L);
        assertEquals(5L, clock.nextEdgeAfter(0L));
        assertEquals(5L, clock.nextEdgeAfter(4L));
        assertEquals(10L, clock.nextEdgeAfter(5L));
        assertEquals(10L, clock.nextEdgeAfter(9L));
    }

    @Test
    void phaseShiftsTheWaveform() {
        ClockWaveform clock = new ClockWaveform(10L, 3L, 2L);
        assertFalse(clock.isHighAt(0L));
        assertTrue(clock.isHighAt(2L));
        assertTrue(clock.isHighAt(4L));
        assertFalse(clock.isHighAt(5L));
        assertEquals(5L, clock.nextEdgeAfter(2L));
        assertEquals(12L, clock.nextEdgeAfter(5L));
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ClockWaveform(1L, 1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new ClockWaveform(10L, 10L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new ClockWaveform(10L, 0L, 0L));
        assertThrows(
                IllegalArgumentException.class, () -> ClockWaveform.withPeriod(10L).isHighAt(-1L));
    }
}
