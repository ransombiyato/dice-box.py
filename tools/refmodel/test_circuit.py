"""Behavioural contract for the Minetech circuit engine.

These tests run the Python reference model, which mirrors the Java engine in
``dev.minetech.core``. They cover every gate, the arithmetic parts, the
sequential parts, and a 4-bit ripple-carry adder built out of full adders.
"""

from __future__ import annotations

import itertools

import pytest

from circuit import Circuit


def source(circuit: Circuit, high: bool = True):
    """A lever used as a constant source."""
    return circuit.add("lever", manual_high=high)


def set_level(circuit: Circuit, lever, high: bool) -> None:
    lever.manual_high = high
    circuit.settle()


def gate(kind: str):
    """Build a gate with both inputs tied to levers."""
    circuit = Circuit()
    g = circuit.add(kind)
    a = source(circuit)
    b = source(circuit)
    circuit.connect(a, "out", g, "a")
    circuit.connect(b, "out", g, "b")
    return circuit, g, a, b


GATE_TABLE = {
    "and": lambda a, b: a & b,
    "or": lambda a, b: a | b,
    "xor": lambda a, b: a ^ b,
    "nand": lambda a, b: (a & b) ^ 1,
    "nor": lambda a, b: (a | b) ^ 1,
    "xnor": lambda a, b: (a ^ b) ^ 1,
}


@pytest.mark.parametrize("kind", sorted(GATE_TABLE))
@pytest.mark.parametrize("a,b", list(itertools.product((0, 1), repeat=2)))
def test_two_input_gates(kind: str, a: int, b: int) -> None:
    circuit, g, lever_a, lever_b = gate(kind)
    set_level(circuit, lever_a, bool(a))
    set_level(circuit, lever_b, bool(b))
    assert circuit.read(g, "out") == GATE_TABLE[kind](a, b)
    assert circuit.stable


@pytest.mark.parametrize("level", [0, 1])
def test_not_gate(level: int) -> None:
    circuit = Circuit()
    inv = circuit.add("not")
    lever = source(circuit, bool(level))
    circuit.connect(lever, "out", inv, "in")
    circuit.settle()
    assert circuit.read(inv, "out") == level ^ 1


def test_buffer_propagates() -> None:
    circuit = Circuit()
    buf = circuit.add("buffer")
    lever = source(circuit, True)
    circuit.connect(lever, "out", buf, "in")
    circuit.settle()
    assert circuit.read(buf, "out") == 1


@pytest.mark.parametrize("a,b", list(itertools.product((0, 1), repeat=2)))
def test_half_adder(a: int, b: int) -> None:
    circuit = Circuit()
    ha = circuit.add("half_adder")
    la, lb = source(circuit, bool(a)), source(circuit, bool(b))
    circuit.connect(la, "out", ha, "a")
    circuit.connect(lb, "out", ha, "b")
    circuit.settle()
    assert circuit.read(ha, "sum") == a ^ b
    assert circuit.read(ha, "carry") == a & b


@pytest.mark.parametrize("a,b,cin", list(itertools.product((0, 1), repeat=3)))
def test_full_adder(a: int, b: int, cin: int) -> None:
    circuit = Circuit()
    fa = circuit.add("full_adder")
    la, lb, lc = source(circuit, bool(a)), source(circuit, bool(b)), source(circuit, bool(cin))
    circuit.connect(la, "out", fa, "a")
    circuit.connect(lb, "out", fa, "b")
    circuit.connect(lc, "out", fa, "cin")
    circuit.settle()
    total = a + b + cin
    assert circuit.read(fa, "sum") == total & 1
    assert circuit.read(fa, "carry") == total >> 1


def test_sr_latch_set_reset_and_hold() -> None:
    circuit = Circuit()
    latch = circuit.add("sr_latch")
    s = source(circuit, False)
    r = source(circuit, False)
    circuit.connect(s, "out", latch, "s")
    circuit.connect(r, "out", latch, "r")

    set_level(circuit, s, True)
    assert circuit.read(latch, "q") == 1
    assert circuit.read(latch, "qn") == 0

    set_level(circuit, s, False)  # S released, R still low: hold
    assert circuit.read(latch, "q") == 1

    set_level(circuit, r, True)
    assert circuit.read(latch, "q") == 0
    assert circuit.read(latch, "qn") == 1

    set_level(circuit, r, False)
    assert circuit.read(latch, "q") == 0


def test_d_flip_flop_only_captures_on_rising_edge() -> None:
    circuit = Circuit()
    dff = circuit.add("d_flip_flop")
    d = source(circuit, False)
    clk = source(circuit, False)
    circuit.connect(d, "out", dff, "d")
    circuit.connect(clk, "out", dff, "clk")

    set_level(circuit, d, True)
    circuit.tick()  # clock low: nothing captured
    assert circuit.read(dff, "q") == 0

    set_level(circuit, clk, True)  # rising edge captures D
    circuit.tick()
    assert circuit.read(dff, "q") == 1

    set_level(circuit, d, False)  # change D while clock stays high
    circuit.tick()
    assert circuit.read(dff, "q") == 1, "must ignore D without a rising edge"

    set_level(circuit, clk, False)
    circuit.tick()
    set_level(circuit, clk, True)  # next rising edge
    circuit.tick()
    assert circuit.read(dff, "q") == 0


def test_counter_counts_and_resets() -> None:
    circuit = Circuit()
    counter = circuit.add("counter_4bit")
    clk = source(circuit, False)
    rst = source(circuit, False)
    circuit.connect(clk, "out", counter, "clk")
    circuit.connect(rst, "out", counter, "rst")

    def bits() -> int:
        return sum(circuit.read(counter, f"q{i}") << i for i in range(4))

    circuit.tick()
    assert bits() == 0
    for expected in (1, 2, 3):
        set_level(circuit, clk, False)
        circuit.tick()
        set_level(circuit, clk, True)
        circuit.tick()
        assert bits() == expected

    set_level(circuit, rst, True)
    assert bits() == 0


def test_ring_oscillator_reports_unstable() -> None:
    circuit = Circuit()
    inv = circuit.add("not")
    circuit.connect(inv, "out", inv, "in")
    assert circuit.settle() is False
    assert circuit.stable is False
    assert circuit.passes == 32


def test_four_bit_ripple_adder() -> None:
    """3 + 5 = 8, built from four full adders wired carry-to-carry."""
    circuit = Circuit()
    a_levers = [source(circuit, False) for _ in range(4)]
    b_levers = [source(circuit, False) for _ in range(4)]
    adders = [circuit.add("full_adder") for _ in range(4)]

    for i, fa in enumerate(adders):
        circuit.connect(a_levers[i], "out", fa, "a")
        circuit.connect(b_levers[i], "out", fa, "b")
        if i > 0:
            circuit.connect(adders[i - 1], "carry", fa, "cin")

    low = source(circuit, False)
    circuit.connect(low, "out", adders[0], "cin")

    def apply(a: int, b: int) -> int:
        for i in range(4):
            a_levers[i].manual_high = bool((a >> i) & 1)
            b_levers[i].manual_high = bool((b >> i) & 1)
        circuit.settle()
        result = sum(circuit.read(fa, "sum") << i for i, fa in enumerate(adders))
        result |= circuit.read(adders[3], "carry") << 4
        return result

    assert apply(3, 5) == 8
    assert apply(0, 0) == 0
    assert apply(1, 1) == 2
    assert apply(7, 1) == 8
    # overflow past four bits lands in the top carry, like a real ripple adder
    assert apply(15, 1) == 16
