package io.github.ransombiyato.dicebox.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ransombiyato.dicebox.sim.Terminal.Direction;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConnectionValidatorTest {
    @Test
    void outputToInputOfSameWidthIsLegal() {
        Terminal source = new Terminal("out", Direction.OUTPUT, 8);
        Terminal sink = new Terminal("in", Direction.INPUT, 8);
        assertEquals(List.of(), ConnectionValidator.validate(source, sink));
        assertTrue(ConnectionValidator.isCompatible(source, sink));
    }

    @Test
    void widthMismatchIsReported() {
        Terminal source = new Terminal("out", Direction.OUTPUT, 8);
        Terminal sink = new Terminal("in", Direction.INPUT, 4);
        assertEquals(
                List.of(ConnectionValidator.Problem.WIDTH_MISMATCH),
                ConnectionValidator.validate(source, sink));
        assertFalse(ConnectionValidator.isCompatible(source, sink));
    }

    @Test
    void inputToOutputReportsBothDirectionProblems() {
        Terminal source = new Terminal("in", Direction.INPUT, 1);
        Terminal sink = new Terminal("out", Direction.OUTPUT, 1);
        assertEquals(
                List.of(
                        ConnectionValidator.Problem.SOURCE_CANNOT_DRIVE,
                        ConnectionValidator.Problem.SINK_CANNOT_ACCEPT),
                ConnectionValidator.validate(source, sink));
    }

    @Test
    void bidirectionalTerminalsWorkOnEitherSide() {
        Terminal bidir = new Terminal("bus", Direction.BIDIRECTIONAL, 16);
        Terminal input = new Terminal("in", Direction.INPUT, 16);
        Terminal output = new Terminal("out", Direction.OUTPUT, 16);
        assertTrue(ConnectionValidator.isCompatible(bidir, input));
        assertTrue(ConnectionValidator.isCompatible(output, bidir));
        assertTrue(ConnectionValidator.isCompatible(bidir, bidir));
    }

    @Test
    void invalidTerminalsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Terminal(" ", Direction.INPUT, 1));
        assertThrows(IllegalArgumentException.class, () -> new Terminal("x", Direction.INPUT, 0));
    }
}
