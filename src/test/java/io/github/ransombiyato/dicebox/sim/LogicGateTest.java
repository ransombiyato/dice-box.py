package io.github.ransombiyato.dicebox.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ransombiyato.dicebox.sim.LogicGate.Kind;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogicGateTest {
    private static boolean reference(Kind kind, boolean x, boolean y) {
        switch (kind) {
            case AND:
                return x && y;
            case OR:
                return x || y;
            case XOR:
                return x ^ y;
            case NAND:
                return !(x && y);
            case NOR:
                return !(x || y);
            case XNOR:
                return x == y;
            default:
                throw new AssertionError("not a binary gate kind: " + kind);
        }
    }

    @Test
    void binaryGateTruthTables() {
        Kind[] binaryKinds = {Kind.AND, Kind.OR, Kind.XOR, Kind.NAND, Kind.NOR, Kind.XNOR};
        for (Kind kind : binaryKinds) {
            LogicGate gate = new LogicGate("g-" + kind, kind, 1, 2);
            for (int a = 0; a <= 1; a++) {
                for (int b = 0; b <= 1; b++) {
                    SignalValue out =
                            gate.evaluate(
                                    List.of(SignalValue.known(1, a), SignalValue.known(1, b)));
                    long expected = reference(kind, a == 1, b == 1) ? 1L : 0L;
                    assertEquals(
                            expected,
                            out.requireKnownValue(),
                            kind + " with inputs " + a + "," + b);
                }
            }
        }
    }

    @Test
    void notAndBuffer() {
        LogicGate not = new LogicGate("n1", Kind.NOT, 4, 1);
        assertEquals(
                SignalValue.known(4, 0b0101L),
                not.evaluate(List.of(SignalValue.known(4, 0b1010L))));

        LogicGate buffer = new LogicGate("b1", Kind.BUFFER, 2, 1);
        SignalValue input = SignalValue.of(2, 0b10L, 0b01L);
        // Buffer passes unknowns through unchanged.
        assertEquals(input, buffer.evaluate(List.of(input)));
    }

    @Test
    void multiInputFolding() {
        LogicGate and3 = new LogicGate("a3", Kind.AND, 1, 3);
        assertEquals(
                0L,
                and3.evaluate(
                                List.of(
                                        SignalValue.known(1, 1L),
                                        SignalValue.known(1, 1L),
                                        SignalValue.known(1, 0L)))
                        .requireKnownValue());
        assertEquals(
                1L,
                and3.evaluate(
                                List.of(
                                        SignalValue.known(1, 1L),
                                        SignalValue.known(1, 1L),
                                        SignalValue.known(1, 1L)))
                        .requireKnownValue());
    }

    @Test
    void terminalsAreExposedInOrder() {
        LogicGate gate = new LogicGate("g1", Kind.AND, 8, 2);
        assertEquals(3, gate.terminals().size());
        assertEquals("in0", gate.terminals().get(0).name());
        assertEquals("in1", gate.terminals().get(1).name());
        assertEquals(Terminal.Direction.OUTPUT, gate.terminal("out").orElseThrow().direction());
        assertTrue(gate.terminal("nope").isEmpty());
    }

    @Test
    void invalidGatesAndCallsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LogicGate("x", Kind.NOT, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> new LogicGate("x", Kind.AND, 1, 1));
        LogicGate gate = new LogicGate("g", Kind.AND, 4, 2);
        assertThrows(
                IllegalArgumentException.class,
                () -> gate.evaluate(List.of(SignalValue.known(4, 0L))));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        gate.evaluate(
                                List.of(SignalValue.known(4, 0L), SignalValue.known(2, 0L))));
    }
}
