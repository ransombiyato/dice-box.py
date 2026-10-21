# Architecture

Status key: **implemented** = code + tests exist today; **planned** = designed
here, no code yet. Nothing planned is described as working anywhere in this
repository. The full requirement source is [MASTER_SPEC.md](MASTER_SPEC.md).

## Targets

- Minecraft Java **1.21.1**, NeoForge **21.1.176**, ModDevGradle **2.0.91**
  (NeoForged MDK branch `archive/1.21-mdg`), Gradle wrapper **8.14.2**,
  Java **21** toolchain, Parchment mappings `2024.11.17` for 1.21.1.
- Mod id `dicebox`, display name "DiceBox Computers",
  base package `io.github.ransombiyato.dicebox`.

## The one hard rule

**The simulation engine must not depend on Minecraft.** Everything in
`io.github.ransombiyato.dicebox.sim` is pure Java (JDK only): no
`net.minecraft`, no `net.neoforged`, no rendering, no world access, no
wall-clock time, no randomness. This makes the engine unit-testable in
milliseconds without a game runtime, deterministic by construction, and safe
to run under explicit budgets instead of on the server thread's goodwill.
Minecraft integration code (future packages outside `sim`) adapts the engine
to blocks/block-entities/GUIs/networking — never the other way round.

Consequences that future code must keep:

- Simulation time is an abstract monotonic `long`, not ticks or real time.
  The integration layer maps sim time to server ticks and enforce budgets
  ("at most N events per tick"), via `EventScheduler.runUntil(time, budget)`.
- World mutations happen only in integration code on the correct thread.
  Worker-thread simulation is forbidden until a thread-safety design is
  written and reviewed here.
- Sim state must be serializable without Minecraft types (building block:
  `SignalValue.encode/decode`).

## Implemented today (milestones A, B slices 1–2, early C)

Package `io.github.ransombiyato.dicebox.sim`:

| Class | Responsibility |
| --- | --- |
| `SignalValue` | Immutable 1–64-bit signal; per-bit 0/1/unknown; AND/OR/XOR/NOT with three-valued unknown propagation (`0&X=0`, `1\|X=1`); resize; stable text encoding for persistence. |
| `EventScheduler<T>` | Deterministic discrete-event queue ordered by (time, insertion sequence); bounded `runUntil(end, maxEvents)` budgeting; no past scheduling. |
| `Terminal` | Typed connection point: name, `INPUT/OUTPUT/BIDIRECTIONAL`, width 1–64. Logical identity only, no geometry. |
| `ConnectionValidator` | Diagnostics: reports *all* reasons a source→sink connection is illegal (width mismatch, cannot drive/accept) instead of a bare boolean. |
| `NetResolver` | Net value from multiple drivers: agreement, known-beats-unknown, per-bit conflict flagging, floating (no driver) detection. |
| `ClockWaveform` | Pure-function clock: period, duty, phase; `isHighAt`, `nextEdgeAfter`. |
| `AccuracyMode` | EDUCATIONAL / FUNCTIONAL / TIMING_AWARE / CYCLE_ACCURATE intent + default gate delay. Selecting a mode claims nothing by itself. |
| `SimComponent` | Minimal component contract: stable `id()`, typed `terminals()`. |
| `CombinationalComponent` | Pure-function evaluation contract (inputs-by-terminal → outputs-by-terminal); deterministic, side-effect free. Sequential components get a separate contract in milestone C. |
| `LogicGate` | First real component (early milestone C): BUFFER/NOT/AND/OR/NAND/NOR/XOR/XNOR over any width/input count. Evaluates by input list or via the `CombinationalComponent` map contract. Delay application belongs to the circuit evaluators, not the gate. |

Package `io.github.ransombiyato.dicebox.sim.circuit` (Phase B slice 2):

| Class | Responsibility |
| --- | --- |
| `Circuit` | Mutable circuit structure: named nets (width-checked), combinational components, terminal→net connections, external-input declarations, topology changes (disconnect/remove), structural validation. Holds no signal state. |
| `TerminalRef` / `CircuitProblem` | Stable logical address of a terminal (`comp.term`); structured diagnostics (unconnected terminal / undriven net / multiple drivers). |
| `CombinationalEvaluator` | Stateless synchronous settling to a fixed point with a hard iteration bound; external drives + seed values; feedback either settles or is reported as unstable by net name. Returns `CircuitEvaluation` (values, settled flag, iterations, conflicted/floating/unstable nets, problems). |
| `EventDrivenCircuitSimulator` | Stateful timed simulation: drivers per net resolved through `NetResolver`; net changes re-evaluate readers and apply outputs after a fixed transport delay via `EventScheduler`; both time and event budgets mandatory; quiescent snapshot/restore (versioned line format, consistency-checked). |

Package `io.github.ransombiyato.dicebox`:

| Class | Responsibility |
| --- | --- |
| `DiceBoxMod` | `@Mod` entrypoint only. Registers **no** content yet; logs on common setup so a dev launch visibly proves the mod loaded. |

Developer tooling (not part of the mod jar):

| File | Responsibility |
| --- | --- |
| `tools/ascii_screen.py` | PNM (P2/P3/P5/P6) framebuffer/screenshot → ASCII art; ASCII cell → exact source pixel rect/center. For headless display debugging. Explicitly does not drive a Minecraft client. |

## Planned module map (no code yet — created when their milestone starts)

To avoid empty-class theater, these packages/files are **not** created
up-front. Planned shape, all under `io.github.ransombiyato.dicebox`:

- `sim/circuit` extensions — graph caching/incremental re-evaluation and a
  sequential-component evaluation model (clocked state transitions) layered
  on the existing evaluators. (Milestones B polish / C)
- `sim/component` — sequential logic (latches, flip-flops, counters, shift
  registers), arithmetic (adders, ALU), mux/decoders, tri-state bus drivers,
  subcircuit expansion + equivalence checks. (Milestone C)
- `sim/memory` — RAM/ROM/flash models, sparse storage, address decoding,
  memory-mapped I/O, module compatibility (generation/form factor/timing).
  (Milestone D)
- `sim/cpu` — common CPU interface; `cpu/i8085` first, then generic `cpu/mos6502`,
  then distinct `cpu/nes2a03`. Custom-ISA execution framework later.
  (Milestones E, I)
- `sim/storage` — virtual disks, filesystems, removable media, USB-style
  enumeration/transfer protocol (documented as USB-inspired, not compliant
  USB). (Milestones D, H)
- `sim/display` — text/palette/framebuffer devices feeding real output to
  integration renderers; `tools/ascii_screen.py` is its headless test view.
- `world/*` (Minecraft-side) — surface-component block entity (many miniature
  components per face, all six faces; one block entity, never one block per
  component), placement/selection tools, rendering (shared/batched meshes),
  GUIs, persistence, server-authoritative networking. (Milestones C, F, G)
- `schematic/*` — versioned format, capture, full-destination validation,
  material manifests, transactional placement/rollback. (Milestone G)
- Power/thermal/manufacturing models. (Milestone J)

## Key design decisions

1. **Three-valued logic (0/1/X), no analog by default.** Unknown propagation
   is per-bit and documented in `SignalValue`. Analog voltage/current, if
   ever added, is opt-in per component (spec Part 6), never per gate.
2. **Conflicts are data, not exceptions.** A net with disagreeing drivers
   resolves to unknown bits and raises a `conflict` flag so diagnostics/UI can
   point at it. Exceptions are reserved for programming errors (bad widths,
   wrong input counts, scheduling in the past).
3. **Determinism first.** Scheduler ordering is fully specified
   (time, then insertion order). Any future parallelism must reproduce the
   same results or be rejected.
4. **Budgets are part of the API, not an afterthought.** `runUntil` cannot
   be called without a maximum event count. There is intentionally no
   `runForever`.
5. **Honest accuracy labels.** `AccuracyMode.CYCLE_ACCURATE` existing as an
   enum value does not make anything cycle-accurate; that claim requires
   reference-vector tests per architecture.
6. **Simulated capacity ≠ host memory.** Future "4 GB RAM sticks" are sparse
   models (pages/maps), never 4 GB host allocations (spec Part 4).
7. **Imports are hostile input.** Future ROM/disk-image/schematic loaders
   validate sizes/formats, forbid path traversal, and never execute imported
   bytes as host code (spec Part 5/7).
8. **Two evaluation semantics, both explicit.** Combinational settling uses
   synchronous rounds (simultaneous updates), so results never depend on
   component order; the event-driven simulator uses a fixed transport delay
   (outputs computed from values at triggering time, applied at t+delay,
   never cancelled in flight). Feedback in both: settles if it can, is
   bounded and reported if it can't — never an infinite loop.

## Threading

Today: everything is single-threaded and pure. Future integration must call
the engine from the server thread with a bounded budget per tick (design
above). Any proposal to simulate on worker threads must first add a section
here describing ownership, snapshot handoff, and how world mutations are
queued back to the server thread.
