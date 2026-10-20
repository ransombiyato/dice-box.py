"""Reference model of the Minetech circuit engine.

This mirrors ``dev.minetech.core`` one-to-one so the simulation semantics can be
executed and tested without a JVM or a Minecraft client. Any behaviour change
should be made here and in the Java engine at the same time, and the tests in
``test_circuit.py`` are the shared contract between the two.
"""

from __future__ import annotations

from dataclasses import dataclass, field

MAX_SETTLE_PASSES = 32

# name -> (port name, direction)
PORTS: dict[str, tuple[tuple[str, str], ...]] = {
    "wire": (("a", "bidir"), ("b", "bidir")),
    "buffer": (("in", "in"), ("out", "out")),
    "not": (("in", "in"), ("out", "out")),
    "and": (("a", "in"), ("b", "in"), ("out", "out")),
    "or": (("a", "in"), ("b", "in"), ("out", "out")),
    "xor": (("a", "in"), ("b", "in"), ("out", "out")),
    "nand": (("a", "in"), ("b", "in"), ("out", "out")),
    "nor": (("a", "in"), ("b", "in"), ("out", "out")),
    "xnor": (("a", "in"), ("b", "in"), ("out", "out")),
    "half_adder": (("a", "in"), ("b", "in"), ("sum", "out"), ("carry", "out")),
    "full_adder": (
        ("a", "in"),
        ("b", "in"),
        ("cin", "in"),
        ("sum", "out"),
        ("carry", "out"),
    ),
    "sr_latch": (("s", "in"), ("r", "in"), ("q", "out"), ("qn", "out")),
    "d_flip_flop": (("d", "in"), ("clk", "in"), ("q", "out"), ("qn", "out")),
    "counter_4bit": (
        ("clk", "in"),
        ("rst", "in"),
        ("q0", "out"),
        ("q1", "out"),
        ("q2", "out"),
        ("q3", "out"),
    ),
    "button": (("out", "out"),),
    "lever": (("out", "out"),),
    "lamp": (("in", "in"),),
    "clock": (("out", "out"),),
    "seven_segment": (
        ("a", "in"),
        ("b", "in"),
        ("c", "in"),
        ("d", "in"),
    ),
}

SEQUENTIAL = {"sr_latch", "d_flip_flop", "counter_4bit", "clock"}


class Net:
    __slots__ = ("id", "parent", "value", "driver_high", "driver_count")

    def __init__(self, net_id: int) -> None:
        self.id = net_id
        self.parent = net_id
        self.value = 0
        self.driver_high = 0
        self.driver_count = 0

    def reset(self) -> None:
        self.driver_high = 0
        self.driver_count = 0

    def drive(self, level: int) -> None:
        self.driver_count += 1
        if level:
            self.driver_high += 1

    def resolve(self) -> bool:
        nxt = 1 if self.driver_high else 0
        if nxt != self.value:
            self.value = nxt
            return True
        return False


@dataclass
class Part:
    id: int
    kind: str
    nets: list[int] = field(default_factory=list)
    state: int = 0
    manual_high: bool = False
    clock_period: int = 20
    clock_phase: int = 0
    last_clock: int = 0
    x: int = 0
    y: int = 0
    rotation: int = 0

    def port(self, name: str) -> int:
        names = [p[0] for p in PORTS[self.kind]]
        return names.index(name)

    @property
    def sequential(self) -> bool:
        return self.kind in SEQUENTIAL


class Circuit:
    def __init__(self) -> None:
        self.nets: list[Net] = []
        self.parts: list[Part] = []
        self._previous: list[int] = []
        self.stable = True
        self.passes = 0

    # -- construction ------------------------------------------------------
    def add(self, kind: str, state: int = 0, manual_high: bool = False) -> Part:
        if kind not in PORTS:
            raise KeyError(kind)
        part = Part(id=len(self.parts), kind=kind, state=state, manual_high=manual_high)
        for _ in PORTS[kind]:
            net = Net(len(self.nets))
            self.nets.append(net)
            part.nets.append(net.id)
        self.parts.append(part)
        return part

    def connect(self, a: Part, port_a: str, b: Part, port_b: str) -> None:
        root_a = self._find(a.nets[a.port(port_a)])
        root_b = self._find(b.nets[b.port(port_b)])
        if root_a != root_b:
            self.nets[root_b].parent = root_a

    def _find(self, net_id: int) -> int:
        root = net_id
        while self.nets[root].parent != root:
            root = self.nets[root].parent
        walk = net_id
        while self.nets[walk].parent != walk:
            nxt = self.nets[walk].parent
            self.nets[walk].parent = root
            walk = nxt
        return root

    # -- inspection --------------------------------------------------------
    def read(self, part: Part, port: str | int) -> int:
        idx = part.port(port) if isinstance(port, str) else port
        return self.nets[self._find(part.nets[idx])].value

    def drive(self, part: Part, port: str | int, level: int) -> None:
        idx = part.port(port) if isinstance(port, str) else port
        self.nets[self._find(part.nets[idx])].drive(level)

    # -- simulation --------------------------------------------------------
    def tick(self) -> None:
        for part in self.parts:
            if part.sequential:
                self._on_tick(part)
        self.settle()

    def _on_tick(self, part: Part) -> None:
        if part.kind == "clock":
            part.clock_phase = (part.clock_phase + 1) % part.clock_period
            if part.clock_phase == 0:
                part.state ^= 1
        elif part.kind == "d_flip_flop":
            clk = self.read(part, "clk")
            rising = part.last_clock == 0 and clk != 0
            part.last_clock = clk
            if rising:
                part.state = self.read(part, "d")
        elif part.kind == "counter_4bit":
            clk = self.read(part, "clk")
            rising = part.last_clock == 0 and clk != 0
            part.last_clock = clk
            if self.read(part, "rst"):
                part.state = 0
            elif rising:
                part.state = (part.state + 1) & 0xF

    def _evaluate(self, part: Part) -> None:
        k = part.kind
        if k in ("wire", "buffer"):
            self.drive(part, "b" if k == "wire" else "out", self.read(part, 0))
        elif k == "not":
            self.drive(part, "out", self.read(part, 0) ^ 1)
        elif k in ("and", "or", "xor", "nand", "nor", "xnor"):
            a = self.read(part, 0)
            b = self.read(part, 1)
            value = {"and": a & b, "or": a | b, "xor": a ^ b}.get(k)
            if value is None:  # nand / nor / xnor invert their base gate
                base = {"nand": a & b, "nor": a | b, "xnor": a ^ b}[k]
                value = base ^ 1
            self.drive(part, "out", value)
        elif k == "half_adder":
            a, b = self.read(part, 0), self.read(part, 1)
            self.drive(part, "sum", a ^ b)
            self.drive(part, "carry", a & b)
        elif k == "full_adder":
            a, b, cin = self.read(part, 0), self.read(part, 1), self.read(part, 2)
            self.drive(part, "sum", a ^ b ^ cin)
            self.drive(part, "carry", (a & b) | (a & cin) | (b & cin))
        elif k == "sr_latch":
            s, r = self.read(part, 0), self.read(part, 1)
            if s and not r:
                part.state = 1
            elif r and not s:
                part.state = 0
            self.drive(part, "q", part.state)
            self.drive(part, "qn", part.state ^ 1)
        elif k == "d_flip_flop":
            self.drive(part, "q", part.state)
            self.drive(part, "qn", part.state ^ 1)
        elif k == "counter_4bit":
            # reset is asynchronous: it clears the register the moment it goes
            # high, without waiting for a clock edge
            if self.read(part, "rst"):
                part.state = 0
            for bit, name in enumerate(("q0", "q1", "q2", "q3")):
                self.drive(part, name, (part.state >> bit) & 1)
        elif k in ("button", "lever"):
            self.drive(part, "out", 1 if part.manual_high else 0)
        # lamps and displays carry no output

    def settle(self) -> bool:
        count = len(self.nets)
        if len(self._previous) != count:
            self._previous = [0] * count
        used = 0
        for _ in range(MAX_SETTLE_PASSES):
            used += 1
            for net in self.nets:
                net.reset()
            for part in self.parts:
                self._evaluate(part)
            changed = False
            for net in self.nets:
                changed |= net.resolve()
            same = all(net.value == self._previous[i] for i, net in enumerate(self.nets))
            if same and not changed:
                self.stable = True
                self.passes = used
                return True
            for i, net in enumerate(self.nets):
                self._previous[i] = net.value
        self.stable = False
        self.passes = used
        return False
