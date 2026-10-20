# Roadmap

Source spec: [MASTER_SPEC.md](MASTER_SPEC.md) (Parts 1–10; Part 8 was not
included in the owner's prompt — if it arrives, insert it and re-plan).
Phases follow Part 10's required order A→K. A phase is done only when its
implementation, automated tests, error handling, persistence (where
applicable) and documentation all exist — compilation alone is not done.

| Phase | Scope | Status |
| --- | --- | --- |
| **A** | Clean baseline, NeoForge build & tests (Part 1) | 🟡 Implemented; verification runs in CI (see TESTING.md) |
| **B** | Sim interfaces, signal engine, scheduler, diagnostics (Part 2) | 🟡 First slice implemented; circuit graph evaluator remains |
| **C** | Gates, sequential logic, buses, miniature surface placement (Part 3) | 🟡 Logic gates only; everything else not started |
| **D** | Memory, persistent storage, RAM modules, PCBs (Part 4) | ⬜ Not started |
| **E** | Intel 8085 + reference tests + runnable programs (Part 4) | ⬜ Not started |
| **F** | First complete programmable computer (Part 5/6 subset) | ⬜ Not started |
| **G** | Schematic tool, obstruction validation, materials, rollback, survival progression (Part 7) | ⬜ Not started |
| **H** | Removable media, USB-style controller, file transfer, boot pipeline, demo OS (Part 5) | ⬜ Not started |
| **I** | Generic 6502, NES 2A03, graphics devices, custom CPU/GPU, expansion (Parts 4/6) | ⬜ Not started |
| **J** | Advanced manufacturing, racks, networking, power, cooling, optional analog (Part 6) | ⬜ Not started |
| **K** | Real-software compatibility, DOOM target, polish, release validation (Parts 5/9) | ⬜ Not started |
| **—** | Part 8 (unknown; not in the stored prompt) | ❓ Awaiting owner |

## Phase A — Clean baseline (this milestone)

Acceptance:

- [x] Inspected old repo (single Python package at `4d4f761`, clean tree, no
      Dependabot config, no open PRs) before touching anything.
- [x] Preservation reference recorded: old Python tree remains at commit
      `4d4f761e21a222e35f3717690d0429f4bafc7b35` on `main`/`origin/main`.
- [x] Deleted only known project files (`pyproject.toml`, `src/dicebox/*`,
      `tests/test_cli.py`) inside the verified repo root; `.git` untouched.
- [x] Fresh NeoForge 1.21.1/MDG skeleton from the official NeoForged MDK
      (`archive/1.21-mdg`): `build.gradle`, `settings.gradle`,
      `gradle.properties`, wrapper 8.14.2, `neoforge.mods.toml` template.
- [x] Mod identity set (`dicebox` / DiceBox Computers / MIT / 0.1.0);
      entrypoint registers no fake content.
- [x] Test infrastructure: JUnit 5 wired in Gradle + CI
      (`.github/workflows/ci.yml`: JDK 21 build+test; Python tool tests).
- [x] README / ARCHITECTURE / ROADMAP / TESTING / MASTER_SPEC written.
- [ ] **Build evidence:** `./gradlew build` green in GitHub Actions on the
      PR (the agent sandbox cannot reach Gradle/Maven/NeoForge/Mojang hosts
      nor install a JDK compiler — see TESTING.md). Link the green run in the
      PR; until then this phase is *implemented, not verified*.
- [ ] **Dev-launch evidence:** `./gradlew runClient` (or `runServer`) reaches
      the title screen / "Done" with the DiceBox load log line, on a machine
      with a display/JDK. Not claimable from the sandbox; instructions in
      README. (Optional stretch: headless CI `runServer` smoke job.)

## Phase B — Simulation engine (first slice done with A)

Implemented + JUnit-tested (`src/test/java/.../sim`, 35 tests):
`SignalValue` (multi-bit & unknown semantics, encoding round-trip),
`EventScheduler` (deterministic ordering, budgets), `Terminal`,
`ConnectionValidator` (width/direction diagnostics), `NetResolver`
(conflicting-driver & floating behavior), `ClockWaveform`, `AccuracyMode`.

Next slice (in order):

1. Circuit graph: nets + connections, structural validation (disconnected
   terminals, undriven nets), topology change API.
2. Combinational settling with iteration limits + feedback detection/reporting.
3. Event-driven evaluation applying per-component delays via the scheduler;
   simultaneous-event policy documented and tested.
4. Snapshot save/restore of circuit state; suspension of idle circuits.
5. Execution-cost instrumentation (events processed, per-circuit budgets).

## Phase C — Logic components & surface placement

Done: combinational `LogicGate` (all 8 kinds, truth-table tests). Remaining:
sequential components (SR/D latches, flip-flops, counters, shift registers)
with state-transition tests; arithmetic (half/full adders → ALU); mux/demux/
encoders/decoders; tri-state drivers on shared buses; subcircuits + expansion
equivalence. Then the Minecraft-side surface system: one block entity holding
many miniature components per face (grid, footprints, overlap, rotation,
persistence, server-authoritative edits) — never one block per component.

## Phases D–K

Per MASTER_SPEC parts 4–7, 9, 10. Highlights to never shortcut:
8085 before 6502 before 2A03 (distinct cores, reference vectors); a computer
"boots" only via the real reset→firmware→loader pipeline; USB-style transfer
is a documented simulated protocol, not a texture; schematics validate the
entire destination transactionally; DOOM is milestone K and requires the
whole real stack underneath it, with user-supplied legally obtained data.

## Immediate next tasks

1. Land this PR with green CI (fix whatever the first real build surfaces).
2. Owner or agent with a desktop JDK: run `./gradlew runClient`, confirm the
   load log line, record it in TESTING.md (completes Phase A acceptance).
3. Phase B slice 1: circuit graph + validation tests.
4. Phase C: sequential logic starter set (D latch/flip-flop, counter) once
   the evaluator can clock them.
