# Minetech

**Realistic micro-electronics for Minecraft.** Build working computers one tiny
part at a time: transistors, gates, adders, latches, RAM — mounted on a block
face, many to a surface, wired by hand, and actually computing.

> **Status: early development (0.1.0).** The simulation engine, its test suite,
> and the build pipeline are in place. Blocks, rendering and recipes are the
> next milestone — see the [roadmap](#roadmap).

- **Minecraft:** 1.21.11
- **Loader:** Fabric (`fabric-loader 0.20.1`, `fabric-api 0.141.6+1.21.11`)
- **Java:** 25 toolchain (mod bytecode targets Java 21 runtime compatibility target of the platform)
- **Build:** Gradle 9.7 · Loom `net.fabricmc.fabric-loom-remap` 1.18-SNAPSHOT

---

## The idea

Minecraft's redstone is great at *big* builds and terrible at *computers*. Making
a 4-bit adder eats a chunk; making a 6502 is a life project. Minetech flips the
scale: components are **millimetre-scale parts mounted on a surface**, dozens to
a single block face, so a full CPU fits in a room instead of a landscape.

Three rules drive every design decision:

1. **Small parts, dense walls.** Every part occupies a cell on a 16×16 face grid
   (an 8×8 or 16×16 arrangement per block face). Hovering a block shows the
   parts on that face; right-clicking opens the wall editor.
2. **It actually computes.** Parts are not decorative. A full adder adds, a
   D-flip-flop latches on a rising edge, a shift register shifts. Behaviour
   follows real datasheet truth tables.
3. **Survival friendly.** Everything is craftable from early-game materials and
   gated behind a sensible tech curve — no creative-only shortcuts, no
   unobtainable parts.

## What exists today

The simulation core is complete and tested, with **no Minecraft dependency**, so
it can be reasoned about and benchmarked in isolation:

| Piece | Detail |
| --- | --- |
| `dev.minetech.core.Circuit` | Fixed-point evaluator: parts push outputs, nets resolve, repeat until stable |
| `Net` | Union-find merged nodes, wired-OR semantics, contested-net detection |
| `Part` | Flat state, per-pin nets, edge-sensitive state machines |
| `PartType` | 20 parts with stable ids and 16×16 pin geometry |
| `src/test/java` | 44 JUnit tests running the real engine on every CI build |
| `tools/refmodel` | Executable Python mirror of the engine — 44 behavioural tests |

### Parts available

| Category | Parts |
| --- | --- |
| Passive | wire, buffer |
| Logic | `not`, `and`, `or`, `xor`, `nand`, `nor`, `xnor` |
| Arithmetic | `half_adder`, `full_adder` |
| Memory | `sr_latch`, `d_flip_flop`, `counter_4bit` |
| I/O | `button`, `lever`, `lamp`, `clock`, `seven_segment` |

## Why it stays fast

Big contraptions live or die on tick cost, so the engine is written for it:

- **Flat state.** Parts are objects with `int` arrays of pin-net ids — no
  per-tick allocation, no object graph walking.
- **Union-find nets.** A chain of ten wire parts costs one lookup, not ten hops.
- **Fixed-point settling with a budget.** `MAX_SETTLE_PASSES = 32`; a circuit
  that never settles (a NOT gate wired into itself) is flagged unstable instead
  of freezing the server.
- **Tick-driven, not frame-driven.** Sequential parts advance on the game tick;
  rendering only reads state.

## Build

```bash
git clone https://github.com/ransombiyato/dice-box.py.git
cd dice-box.py
gradle wrapper           # once, to generate ./gradlew

./gradlew build          # jar lands in build/libs/
./gradlew runClient      # dev client
./gradlew test           # 44 JUnit tests
```

Needs a JDK 25 toolchain and Gradle 9.7. A wrapper is not committed yet.

### Verify the engine without Minecraft

```bash
cd tools/refmodel
python -m pytest -q      # 44 tests: every gate, adders, latch, counter, ripple adder
```

The CI workflow (`.github/workflows/build.yml`) compiles and tests on every
push.

## Roadmap

- [x] **M0 — Engine.** Nets, gates, adders, sequential parts, fixed-point
      settling, test suite.
- [ ] **M1 — Mounted parts.** `PartPanelBlock` + block entity, 16×16 face
      grid, part placement/removal, `BlockEntityRenderer` for parts.
- [ ] **M2 — Wiring.** Pin-to-pin wire tool, net visualisation, multimeter.
- [ ] **M3 — Survival.** Tin/copper/silicon chain, PCB substrate, part recipes,
      tool tiers, early-game gating.
- [ ] **M4 — Copy tool.** Blueprint capture/paste of a whole panel, with
      survival material cost.
- [ ] **M5 — Big parts.** RAM, ROM, shift register, 7-segment decoder,
      multiplexers, bus parts.
- [ ] **M6 — Retro machines.** Bundled blueprints: an Intel 8085-class CPU and
      a NES 6502-class CPU, each assembled from the mod's own parts.
- [ ] **M7 — Polish.** Sound, particles, JEI/REI integration, config, server
      performance pass.

## Design notes

**Signal semantics.** Digital nets are wired-OR: any driver high pulls the net
high. This matches redstone dust, so hand-wiring behaves the way players already
expect. Contested nets are flagged for the multimeter to show.

**Edge detection.** `d_flip_flop` and `counter_4bit` sample their clock once per
tick, comparing against the previous level — a rising edge is `old == 0 &&
new != 0`. Sampling happens before the settle pass so state machines cannot
chase combinational values within a tick.

**Asynchronous reset.** The counter's reset clears the register as soon as it
goes high, without waiting for a clock edge, matching 74-series counters.

## License

MIT — see [LICENSE](LICENSE).
