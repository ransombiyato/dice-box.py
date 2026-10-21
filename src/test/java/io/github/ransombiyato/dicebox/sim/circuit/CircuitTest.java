package io.github.ransombiyato.dicebox.sim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ransombiyato.dicebox.sim.LogicGate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CircuitTest {
    private static Circuit andCircuit() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("b", 1);
        circuit.addNet("y", 1);
        circuit.addComponent(new LogicGate("and1", LogicGate.Kind.AND, 1, 2));
        circuit.connect("and1", "in0", "a");
        circuit.connect("and1", "in1", "b");
        circuit.connect("and1", "out", "y");
        circuit.declareExternalInput("a");
        circuit.declareExternalInput("b");
        return circuit;
    }

    private static List<CircuitProblem.Kind> kinds(Circuit circuit) {
        return circuit.validate().stream().map(CircuitProblem::kind).toList();
    }

    @Test
    void buildsAndConnects() {
        Circuit circuit = andCircuit();
        assertEquals(1, circuit.components().size());
        assertEquals(3, circuit.nets().size());
        assertEquals(3, circuit.connections().size());
        assertEquals(
                Optional.of("a"), circuit.netOf(new TerminalRef("and1", "in0")));
        assertEquals(
                List.of(new TerminalRef("and1", "in0")), circuit.terminalsOnNet("a"));
        assertEquals(1, circuit.netWidth("y"));
        // Fully connected, singly driven, external inputs declared: no problems.
        assertEquals(List.of(), circuit.validate());
    }

    @Test
    void duplicatesAreRejected() {
        Circuit circuit = andCircuit();
        assertThrows(IllegalArgumentException.class, () -> circuit.addNet("a", 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> circuit.addComponent(new LogicGate("and1", LogicGate.Kind.OR, 1, 2)));
    }

    @Test
    void invalidNamesUnknownsAndMismatchesAreRejected() {
        Circuit circuit = new Circuit();
        assertThrows(IllegalArgumentException.class, () -> circuit.addNet("bad name", 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> circuit.addComponent(new LogicGate("bad.id", LogicGate.Kind.NOT, 1, 1)));
        circuit.addNet("a", 1);
        circuit.addNet("wide", 4);
        circuit.addComponent(new LogicGate("g1", LogicGate.Kind.NOT, 1, 1));
        assertThrows(
                IllegalArgumentException.class, () -> circuit.connect("nope", "in0", "a"));
        assertThrows(
                IllegalArgumentException.class, () -> circuit.connect("g1", "in9", "a"));
        assertThrows(
                IllegalArgumentException.class, () -> circuit.connect("g1", "in0", "missing"));
        assertThrows(
                IllegalArgumentException.class, () -> circuit.connect("g1", "in0", "wide"));
        circuit.connect("g1", "in0", "a");
        assertThrows(
                IllegalArgumentException.class, () -> circuit.connect("g1", "in0", "a"));
    }

    @Test
    void validationReportsUnconnectedAndUndriven() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("dangling", 2);
        circuit.addComponent(new LogicGate("and1", LogicGate.Kind.AND, 1, 2));
        circuit.connect("and1", "in0", "a");
        circuit.declareExternalInput("a");

        List<CircuitProblem.Kind> found = kinds(circuit);
        // in1 and out are unconnected; 'dangling' has no driver; 'a' is a
        // declared external input so it must NOT be reported undriven.
        assertEquals(
                2, found.stream().filter(k -> k == CircuitProblem.Kind.UNCONNECTED_TERMINAL).count());
        assertEquals(
                1, found.stream().filter(k -> k == CircuitProblem.Kind.UNDRIVEN_NET).count());
        assertFalse(found.contains(CircuitProblem.Kind.MULTIPLE_DRIVERS));
    }

    @Test
    void validationReportsMultipleDrivers() {
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

        assertEquals(List.of(CircuitProblem.Kind.MULTIPLE_DRIVERS), kinds(circuit));
    }

    @Test
    void topologyChangesRemoveConnections() {
        Circuit circuit = new Circuit();
        circuit.addNet("a", 1);
        circuit.addNet("y", 1);
        circuit.addComponent(new LogicGate("not1", LogicGate.Kind.NOT, 1, 1));
        circuit.connect("not1", "in0", "a");
        circuit.connect("not1", "out", "y");

        TerminalRef in = new TerminalRef("not1", "in0");
        assertTrue(circuit.removeNet("a"));
        assertTrue(circuit.netOf(in).isEmpty());
        assertFalse(circuit.removeNet("a"));

        circuit.addNet("a", 1);
        circuit.connect("not1", "in0", "a");
        assertTrue(circuit.disconnect(in));
        assertFalse(circuit.disconnect(in));

        assertTrue(circuit.removeComponent("not1"));
        assertTrue(circuit.components().isEmpty());
        assertTrue(circuit.connections().isEmpty());
        assertFalse(circuit.removeComponent("not1"));
    }

    @Test
    void terminalRefParsesAndRoundTrips() {
        TerminalRef ref = TerminalRef.parse("and1.in0");
        assertEquals("and1", ref.componentId());
        assertEquals("in0", ref.terminalName());
        assertEquals("and1.in0", ref.toString());
        assertThrows(IllegalArgumentException.class, () -> TerminalRef.parse("no-dot"));
        assertThrows(IllegalArgumentException.class, () -> TerminalRef.parse("only."));
    }
}
