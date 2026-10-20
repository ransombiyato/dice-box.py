package dev.minetech.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Behavioural tests for the circuit engine.
 *
 * <p>These mirror {@code tools/refmodel/test_circuit.py}; the two suites are the
 * shared contract for the simulation semantics.
 */
class CircuitTest {

    private static Part lever(Circuit circuit, boolean high) {
        Part lever = circuit.add(PartType.LEVER);
        lever.setManualHigh(high);
        return lever;
    }

    private static void set(Circuit circuit, Part lever, boolean high) {
        lever.setManualHigh(high);
        circuit.settle();
    }

    private record Gate(Circuit circuit, Part part, Part a, Part b) {}

    private static Gate gate(PartType type) {
        Circuit circuit = new Circuit();
        Part part = circuit.add(type);
        Part a = lever(circuit, false);
        Part b = lever(circuit, false);
        circuit.connect(a, "out", part, "a");
        circuit.connect(b, "out", part, "b");
        return new Gate(circuit, part, a, b);
    }

    @ParameterizedTest
    @CsvSource({
            "AND, 0, 0, 0", "AND, 0, 1, 0", "AND, 1, 0, 0", "AND, 1, 1, 1",
            "OR, 0, 0, 0", "OR, 0, 1, 1", "OR, 1, 0, 1", "OR, 1, 1, 1",
            "XOR, 0, 0, 0", "XOR, 0, 1, 1", "XOR, 1, 0, 1", "XOR, 1, 1, 0",
            "NAND, 0, 0, 1", "NAND, 0, 1, 1", "NAND, 1, 0, 1", "NAND, 1, 1, 0",
            "NOR, 0, 0, 1", "NOR, 0, 1, 0", "NOR, 1, 0, 0", "NOR, 1, 1, 0",
            "XNOR, 0, 0, 1", "XNOR, 0, 1, 0", "XNOR, 1, 0, 0", "XNOR, 1, 1, 1"
    })
    void twoInputGates(String typeName, int a, int b, int expected) {
        Gate g = gate(PartType.valueOf(typeName));
        set(g.circuit(), g.a(), a == 1);
        set(g.circuit(), g.b(), b == 1);
        assertEquals(expected, g.circuit().read(g.part(), "out"));
        assertTrue(g.circuit().isStable());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 0"})
    void notGate(int level, int expected) {
        Circuit circuit = new Circuit();
        Part inverter = circuit.add(PartType.NOT);
        Part input = lever(circuit, level == 1);
        circuit.connect(input, "out", inverter, "in");
        circuit.settle();
        assertEquals(expected, circuit.read(inverter, "out"));
    }

    @Test
    void bufferPropagates() {
        Circuit circuit = new Circuit();
        Part buffer = circuit.add(PartType.BUFFER);
        Part input = lever(circuit, true);
        circuit.connect(input, "out", buffer, "in");
        circuit.settle();
        assertEquals(1, circuit.read(buffer, "out"));
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 0, 0", "0, 1, 1, 0", "1, 0, 1, 0", "1, 1, 0, 1"})
    void halfAdder(int a, int b, int sum, int carry) {
        Circuit circuit = new Circuit();
        Part adder = circuit.add(PartType.HALF_ADDER);
        Part la = lever(circuit, a == 1);
        Part lb = lever(circuit, b == 1);
        circuit.connect(la, "out", adder, "a");
        circuit.connect(lb, "out", adder, "b");
        circuit.settle();
        assertEquals(sum, circuit.read(adder, "sum"));
        assertEquals(carry, circuit.read(adder, "carry"));
    }

    @ParameterizedTest
    @CsvSource({
            "0,0,0,0,0", "0,0,1,1,0", "0,1,0,1,0", "0,1,1,0,1",
            "1,0,0,1,0", "1,0,1,0,1", "1,1,0,0,1", "1,1,1,1,1"
    })
    void fullAdder(int a, int b, int cin, int sum, int carry) {
        Circuit circuit = new Circuit();
        Part adder = circuit.add(PartType.FULL_ADDER);
        Part la = lever(circuit, a == 1);
        Part lb = lever(circuit, b == 1);
        Part lc = lever(circuit, cin == 1);
        circuit.connect(la, "out", adder, "a");
        circuit.connect(lb, "out", adder, "b");
        circuit.connect(lc, "out", adder, "cin");
        circuit.settle();
        assertEquals(sum, circuit.read(adder, "sum"));
        assertEquals(carry, circuit.read(adder, "carry"));
    }

    @Test
    void srLatchSetsHoldsAndResets() {
        Circuit circuit = new Circuit();
        Part latch = circuit.add(PartType.SR_LATCH);
        Part s = lever(circuit, false);
        Part r = lever(circuit, false);
        circuit.connect(s, "out", latch, "s");
        circuit.connect(r, "out", latch, "r");

        set(circuit, s, true);
        assertEquals(1, circuit.read(latch, "q"));
        assertEquals(0, circuit.read(latch, "qn"));

        set(circuit, s, false);
        assertEquals(1, circuit.read(latch, "q"), "releasing set must hold");

        set(circuit, r, true);
        assertEquals(0, circuit.read(latch, "q"));

        set(circuit, r, false);
        assertEquals(0, circuit.read(latch, "q"), "releasing reset must hold");
    }

    @Test
    void flipFlopCapturesOnlyOnRisingEdge() {
        Circuit circuit = new Circuit();
        Part dff = circuit.add(PartType.D_FLIP_FLOP);
        Part d = lever(circuit, false);
        Part clk = lever(circuit, false);
        circuit.connect(d, "out", dff, "d");
        circuit.connect(clk, "out", dff, "clk");

        set(circuit, d, true);
        circuit.tick();
        assertEquals(0, circuit.read(dff, "q"), "clock low must not capture");

        set(circuit, clk, true);
        circuit.tick();
        assertEquals(1, circuit.read(dff, "q"));

        set(circuit, d, false);
        circuit.tick();
        assertEquals(1, circuit.read(dff, "q"), "must ignore D without a rising edge");

        set(circuit, clk, false);
        circuit.tick();
        set(circuit, clk, true);
        circuit.tick();
        assertEquals(0, circuit.read(dff, "q"));
    }

    @Test
    void counterCountsAndResetsAsynchronously() {
        Circuit circuit = new Circuit();
        Part counter = circuit.add(PartType.COUNTER_4BIT);
        Part clk = lever(circuit, false);
        Part rst = lever(circuit, false);
        circuit.connect(clk, "out", counter, "clk");
        circuit.connect(rst, "out", counter, "rst");

        circuit.tick();
        assertEquals(0, bits(circuit, counter));

        for (int expected = 1; expected <= 3; expected++) {
            set(circuit, clk, false);
            circuit.tick();
            set(circuit, clk, true);
            circuit.tick();
            assertEquals(expected, bits(circuit, counter));
        }

        set(circuit, rst, true);
        assertEquals(0, bits(circuit, counter), "reset must not wait for a clock edge");
    }

    private static int bits(Circuit circuit, Part counter) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= circuit.read(counter, "q" + i) << i;
        }
        return value;
    }

    @Test
    void ringOscillatorIsReportedUnstable() {
        Circuit circuit = new Circuit();
        Part inverter = circuit.add(PartType.NOT);
        circuit.connect(inverter, "out", inverter, "in");
        assertFalse(circuit.settle());
        assertFalse(circuit.isStable());
        assertEquals(Circuit.MAX_SETTLE_PASSES, circuit.lastPassCount());
    }

    @Test
    void fourBitRippleCarryAdder() {
        Circuit circuit = new Circuit();
        Part[] a = new Part[4];
        Part[] b = new Part[4];
        Part[] adders = new Part[4];
        for (int i = 0; i < 4; i++) {
            a[i] = lever(circuit, false);
            b[i] = lever(circuit, false);
            adders[i] = circuit.add(PartType.FULL_ADDER);
            circuit.connect(a[i], "out", adders[i], "a");
            circuit.connect(b[i], "out", adders[i], "b");
            if (i > 0) {
                circuit.connect(adders[i - 1], "carry", adders[i], "cin");
            }
        }
        Part low = lever(circuit, false);
        circuit.connect(low, "out", adders[0], "cin");

        assertEquals(8, add(circuit, a, b, adders, 3, 5));
        assertEquals(0, add(circuit, a, b, adders, 0, 0));
        assertEquals(2, add(circuit, a, b, adders, 1, 1));
        assertEquals(8, add(circuit, a, b, adders, 7, 1));
        assertEquals(16, add(circuit, a, b, adders, 15, 1));
    }

    private static int add(Circuit circuit, Part[] a, Part[] b, Part[] adders, int x, int y) {
        for (int i = 0; i < 4; i++) {
            a[i].setManualHigh(((x >> i) & 1) == 1);
            b[i].setManualHigh(((y >> i) & 1) == 1);
        }
        circuit.settle();
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= circuit.read(adders[i], "sum") << i;
        }
        result |= circuit.read(adders[3], "carry") << 4;
        return result;
    }
}
