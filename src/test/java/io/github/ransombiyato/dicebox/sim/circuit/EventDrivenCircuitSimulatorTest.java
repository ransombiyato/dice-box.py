package io.github.ransombiyato.dicebox.sim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ransombiyato.dicebox.sim.LogicGate;
import io.github.ransombiyato.dicebox.sim.SignalValue;
import org.junit.jupiter.api.Test;

class EventDrivenCircuitSimulatorTest {
    private static SignalValue bit(int v) {
        return SignalValue.known(1, v);
    }

    private static Circuit notCircuit() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("y", 1);
        circuit.declareExternalInput("a");
        circuit.addComponent(new LogicGate("inv", LogicGate.Kind.NOT, 1, 1));
        circuit.connect("inv", "in0", "a");
        circuit.connect("inv", "out", "y");
        return circuit;
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

    private static Circuit notChain(int gates) {
        Circuit circuit = new Circuit();
        circuit.addNet("n0", 1);
        circuit.declareExternalInput("n0");
        for (int i = 0; i < gates; i++) {
            circuit.addNet("n" + (i + 1), 1);
            circuit.addComponent(new LogicGate("inv" + i, LogicGate.Kind.NOT, 1, 1));
            circuit.connect("inv" + i, "in0", "n" + i);
            circuit.connect("inv" + i, "out", "n" + (i + 1));
        }
        return circuit;
    }

    @Test
    void outputChangesExactlyAfterGateDelay() {
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(notCircuit(), 5L);
        sim.setInput("a", bit(0));

        EventDrivenCircuitSimulator.RunOutcome early = sim.runUntil(4L, 100);
        assertFalse(early.settled());
        assertEquals(4L, early.now());
        assertEquals(SignalValue.unknown(1), sim.netValue("y"));

        EventDrivenCircuitSimulator.RunOutcome atEdge = sim.runUntil(5L, 100);
        assertTrue(atEdge.settled());
        assertEquals(5L, sim.now());
        assertEquals(bit(1), sim.netValue("y"));

        sim.setInput("a", bit(1));
        sim.runUntil(9L, 100);
        assertEquals(bit(1), sim.netValue("y"), "old output holds until the delay elapses");
        sim.runUntil(10L, 100);
        assertEquals(bit(0), sim.netValue("y"));
    }

    @Test
    void delaysAccumulateAlongAChain() {
        // 3 inverters, delay 2 each: input at t=0, output at t=6.
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(notChain(3), 2L);
        sim.setInput("n0", bit(0));

        sim.runUntil(5L, 100);
        assertFalse(sim.isSettled());
        assertEquals(SignalValue.unknown(1), sim.netValue("n3"));

        EventDrivenCircuitSimulator.RunOutcome done = sim.runUntil(6L, 100);
        assertTrue(done.settled());
        // NOT(NOT(NOT(0))) = 1
        assertEquals(bit(1), sim.netValue("n3"));
        assertEquals(bit(0), sim.netValue("n2"));
    }

    @Test
    void eventBudgetPausesAndResumesExactly() {
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(notChain(6), 1L);
        sim.setInput("n0", bit(1));

        EventDrivenCircuitSimulator.RunOutcome partial = sim.runUntil(100L, 2);
        assertEquals(2, partial.processedEvents());
        assertFalse(partial.settled());
        // Second event is the first gate output at t=1; time must stop there.
        assertEquals(1L, partial.now());
        assertTrue(partial.pendingEvents() > 0);

        EventDrivenCircuitSimulator.RunOutcome rest = sim.runUntil(100L, 100);
        assertTrue(rest.settled());
        // Six inversions of 1 = 1.
        assertEquals(bit(1), sim.netValue("n6"));
    }

    @Test
    void zeroDelaySettlesAtTheSameTime() {
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(andCircuit(), 0L);
        sim.setInput("a", bit(1));
        sim.setInput("b", bit(0));
        EventDrivenCircuitSimulator.RunOutcome outcome = sim.runUntil(0L, 100);
        assertTrue(outcome.settled());
        assertEquals(0L, sim.now());
        assertEquals(bit(0), sim.netValue("y"));
    }

    @Test
    void conflictingDriversAreVisibleInSimulator() {
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

        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(circuit, 1L);
        sim.setInput("a", bit(0));
        sim.setInput("b", bit(1));
        sim.runUntil(10L, 100);
        assertTrue(sim.isSettled());
        assertEquals(SignalValue.Bit.UNKNOWN, sim.netValue("y").bit(0));
        assertTrue(sim.conflictedNets().contains("y"));
    }

    @Test
    void snapshotRestoresSettledStateAndContinues() {
        Circuit circuit = andCircuit();
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(circuit, 3L);
        sim.setInput("a", bit(1));
        sim.setInput("b", bit(1));
        sim.runUntil(3L, 100);
        assertEquals(bit(1), sim.netValue("y"));

        String snapshot = sim.snapshot();
        EventDrivenCircuitSimulator restored =
                EventDrivenCircuitSimulator.restore(circuit, 3L, snapshot);
        assertEquals(3L, restored.now());
        assertTrue(restored.isSettled());
        assertEquals(sim.netValues(), restored.netValues());

        restored.setInput("b", bit(0));
        restored.runUntil(6L, 100);
        assertEquals(bit(0), restored.netValue("y"));
        assertEquals(bit(1), sim.netValue("y"), "original simulator is unaffected");
    }

    @Test
    void snapshotWithPendingEventsIsRejected() {
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(notCircuit(), 5L);
        sim.setInput("a", bit(0));
        assertThrows(IllegalStateException.class, sim::snapshot);
        sim.runUntil(2L, 100);
        assertThrows(IllegalStateException.class, sim::snapshot);
    }

    @Test
    void corruptSnapshotsAreRejected() {
        Circuit circuit = notCircuit();
        assertThrows(
                IllegalArgumentException.class,
                () -> EventDrivenCircuitSimulator.restore(circuit, 1L, "nonsense"));
        // Stored net value y=1 cannot come from zero drivers: inconsistent.
        String inconsistent =
                "dicebox-circuit-snapshot-v1\ntime 0\nnet a 1:0:0\nnet y 1:1:0\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> EventDrivenCircuitSimulator.restore(circuit, 1L, inconsistent));
        // Snapshot for a different set of nets.
        String foreign = "dicebox-circuit-snapshot-v1\ntime 0\nnet a 1:0:1\n";
        assertThrows(
                IllegalArgumentException.class,
                () -> EventDrivenCircuitSimulator.restore(circuit, 1L, foreign));
    }

    @Test
    void invalidCallsAreRejected() {
        EventDrivenCircuitSimulator sim = new EventDrivenCircuitSimulator(notCircuit(), 1L);
        assertThrows(IllegalArgumentException.class, () -> sim.setInput("missing", bit(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> sim.setInput("a", SignalValue.known(2, 1L)));
        assertThrows(IllegalArgumentException.class, () -> sim.runUntil(10L, 0));
        assertThrows(IllegalArgumentException.class, () -> sim.netValue("missing"));
        assertThrows(
                IllegalArgumentException.class, () -> new EventDrivenCircuitSimulator(notCircuit(), -1L));

        sim.setInput("a", bit(1));
        sim.runUntil(5L, 100);
        assertThrows(IllegalArgumentException.class, () -> sim.runUntil(4L, 100));
    }
}
