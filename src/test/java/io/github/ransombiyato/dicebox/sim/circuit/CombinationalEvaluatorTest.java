package io.github.ransombiyato.dicebox.sim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ransombiyato.dicebox.sim.LogicGate;
import io.github.ransombiyato.dicebox.sim.SignalValue;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CombinationalEvaluatorTest {
    private static SignalValue bit(int v) {
        return SignalValue.known(1, v);
    }

    private static Circuit andCircuit() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("b", 1);
        circuit.addNet("y", 1);
        circuit.declareExternalInput("a");
        circuit.declareExternalInput("b");
        circuit.addComponent(new LogicGate("and1", LogicGate.Kind.AND, 1, 2));
        circuit.connect("and1", "in0", "a");
        circuit.connect("and1", "in1", "b");
        circuit.connect("and1", "out", "y");
        return circuit;
    }

    private static Circuit norLatch() {
        // Classic cross-coupled NOR SR latch:
        //   q  = NOR(r, qb)
        //   qb = NOR(s, q)
        Circuit circuit = new Circuit();
        circuit.addNet("s", 1);
        circuit.addNet("r", 1);
        circuit.addNet("q", 1);
        circuit.addNet("qb", 1);
        circuit.declareExternalInput("s");
        circuit.declareExternalInput("r");
        circuit.addComponent(new LogicGate("nor-q", LogicGate.Kind.NOR, 1, 2));
        circuit.addComponent(new LogicGate("nor-qb", LogicGate.Kind.NOR, 1, 2));
        circuit.connect("nor-q", "in0", "r");
        circuit.connect("nor-q", "in1", "qb");
        circuit.connect("nor-q", "out", "q");
        circuit.connect("nor-qb", "in0", "s");
        circuit.connect("nor-qb", "in1", "q");
        circuit.connect("nor-qb", "out", "qb");
        return circuit;
    }

    @Test
    void andGateThroughCircuit() {
        Circuit circuit = andCircuit();
        for (int a = 0; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                CircuitEvaluation result = CombinationalEvaluator.evaluate(
                        circuit, Map.of("a", bit(a), "b", bit(b)));
                assertTrue(result.settled(), "inputs " + a + "," + b);
                assertEquals(bit(a & b), result.value("y"));
                assertTrue(result.problems().isEmpty());
                assertTrue(result.conflictedNets().isEmpty());
            }
        }
    }

    @Test
    void halfAdder() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("b", 1);
        circuit.addNet("sum", 1);
        circuit.addNet("carry", 1);
        circuit.declareExternalInput("a");
        circuit.declareExternalInput("b");
        circuit.addComponent(new LogicGate("xor-sum", LogicGate.Kind.XOR, 1, 2));
        circuit.addComponent(new LogicGate("and-carry", LogicGate.Kind.AND, 1, 2));
        circuit.connect("xor-sum", "in0", "a");
        circuit.connect("xor-sum", "in1", "b");
        circuit.connect("xor-sum", "out", "sum");
        circuit.connect("and-carry", "in0", "a");
        circuit.connect("and-carry", "in1", "b");
        circuit.connect("and-carry", "out", "carry");

        for (int a = 0; a <= 1; a++) {
            for (int b = 0; b <= 1; b++) {
                CircuitEvaluation result = CombinationalEvaluator.evaluate(
                        circuit, Map.of("a", bit(a), "b", bit(b)));
                assertTrue(result.settled());
                assertEquals(bit(a ^ b), result.value("sum"), "sum " + a + "+" + b);
                assertEquals(bit(a & b), result.value("carry"), "carry " + a + "+" + b);
            }
        }
    }

    @Test
    void chainedGatesPropagateOverRounds() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("mid", 1);
        circuit.addNet("y", 1);
        circuit.declareExternalInput("a");
        circuit.addComponent(new LogicGate("not1", LogicGate.Kind.NOT, 1, 1));
        circuit.addComponent(new LogicGate("not2", LogicGate.Kind.NOT, 1, 1));
        circuit.connect("not1", "in0", "a");
        circuit.connect("not1", "out", "mid");
        circuit.connect("not2", "in0", "mid");
        circuit.connect("not2", "out", "y");

        CircuitEvaluation result = CombinationalEvaluator.evaluate(circuit, Map.of("a", bit(1)));
        assertTrue(result.settled());
        assertEquals(bit(0), result.value("mid"));
        assertEquals(bit(1), result.value("y"));
        // Synchronous rounds: a 2-deep chain cannot settle in 1 round.
        assertTrue(result.iterations() >= 2);
    }

    @Test
    void unconnectedInputReadsAsUnknown() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("y", 1);
        circuit.declareExternalInput("a");
        circuit.addComponent(new LogicGate("and1", LogicGate.Kind.AND, 1, 2));
        circuit.connect("and1", "in0", "a");
        circuit.connect("and1", "out", "y");
        // in1 deliberately unconnected: AND(1, X) = X, AND(0, X) = 0.
        CircuitEvaluation one = CombinationalEvaluator.evaluate(circuit, Map.of("a", bit(1)));
        assertEquals(SignalValue.Bit.UNKNOWN, one.value("y").bit(0));
        assertTrue(one.problems().stream()
                .anyMatch(p -> p.kind() == CircuitProblem.Kind.UNCONNECTED_TERMINAL));
        CircuitEvaluation zero = CombinationalEvaluator.evaluate(circuit, Map.of("a", bit(0)));
        assertEquals(bit(0), zero.value("y"));
    }

    @Test
    void floatingNetIsReported() {
        Circuit circuit = andCircuit();
        circuit.addNet("dangling", 2);
        CircuitEvaluation result = CombinationalEvaluator.evaluate(
                circuit, Map.of("a", bit(1), "b", bit(1)));
        assertEquals(SignalValue.unknown(2), result.value("dangling"));
        assertTrue(result.floatingNets().contains("dangling"));
    }

    @Test
    void conflictingDriversResolveToUnknownAndAreFlagged() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("b", 1);
        circuit.addNet("y", 1);
        circuit.declareExternalInput("a");
        circuit.declareExternalInput("b");
        circuit.addComponent(new LogicGate("buf1", LogicGate.Kind.BUFFER, 1, 1));
        circuit.addComponent(new LogicGate("buf2", LogicGate.Kind.BUFFER, 1, 1));
        circuit.connect("buf1", "in0", "a");
        circuit.connect("buf2", "in0", "b");
        circuit.connect("buf1", "out", "y");
        circuit.connect("buf2", "out", "y");

        CircuitEvaluation result = CombinationalEvaluator.evaluate(
                circuit, Map.of("a", bit(0), "b", bit(1)));
        assertTrue(result.settled());
        assertEquals(SignalValue.Bit.UNKNOWN, result.value("y").bit(0));
        assertEquals(Set.of("y"), result.conflictedNets());
    }

    @Test
    void stableFeedbackLatchHoldsSeededState() {
        Circuit circuit = norLatch();
        CircuitEvaluation hold = CombinationalEvaluator.evaluate(
                circuit,
                Map.of("s", bit(0), "r", bit(0)),
                Map.of("q", bit(1), "qb", bit(0)),
                100);
        assertTrue(hold.settled());
        assertEquals(bit(1), hold.value("q"));
        assertEquals(bit(0), hold.value("qb"));
    }

    @Test
    void latchCanBeSetThroughFeedback() {
        Circuit circuit = norLatch();
        CircuitEvaluation set = CombinationalEvaluator.evaluate(
                circuit,
                Map.of("s", bit(1), "r", bit(0)),
                Map.of("q", bit(0), "qb", bit(1)),
                100);
        assertTrue(set.settled());
        assertEquals(bit(1), set.value("q"));
        assertEquals(bit(0), set.value("qb"));
    }

    @Test
    void unstableFeedbackIsBoundedAndReported() {
        Circuit circuit = new Circuit();
        circuit.addNet("loop", 1);
        circuit.addComponent(new LogicGate("inv", LogicGate.Kind.NOT, 1, 1));
        circuit.connect("inv", "in0", "loop");
        circuit.connect("inv", "out", "loop");

        // From unknown, NOT(X) = X: immediately stable.
        CircuitEvaluation stable = CombinationalEvaluator.evaluate(circuit, Map.of());
        assertTrue(stable.settled());
        assertEquals(SignalValue.unknown(1), stable.value("loop"));

        // Seeded with 0, the ring oscillates 0 -> 1 -> 0 ... and must be
        // stopped by the iteration bound, with the net named.
        CircuitEvaluation oscillating = CombinationalEvaluator.evaluate(
                circuit, Map.of(), Map.of("loop", bit(0)), 10);
        assertFalse(oscillating.settled());
        assertEquals(10, oscillating.iterations());
        assertEquals(Set.of("loop"), oscillating.unstableNets());
    }

    @Test
    void invalidEvaluationArgumentsAreRejected() {
        Circuit circuit = andCircuit();
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinationalEvaluator.evaluate(circuit, Map.of("missing", bit(1))));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinationalEvaluator.evaluate(
                        circuit, Map.of("a", SignalValue.known(2, 1L))));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinationalEvaluator.evaluate(circuit, Map.of(), Map.of(), 0));
    }
}
