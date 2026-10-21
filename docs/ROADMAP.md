# Roadmap

Source spec: [MASTER_SPEC.md](MASTER_SPEC.md) (Parts 1–10; Part 8 was not
included in the owner's prompt — if it arrives, insert it and re-plan).
Phases follow Part 10's required order A→K. A phase is done only when its
implementation, automated tests, error handling, persistence (where
applicable) and documentation all exist — compilation alone is not done.

| Phase | Scope | Status |
| --- | --- | --- |
| **A** | Clean baseline, NeoForge build & tests (Part 1) | 🟡 Build+tests verified green in CI; dev launch pending desktop confirmation (see TESTING.md) |
| **B** | Sim interfaces, signal engine, scheduler, diagnostics (Part 2) | 🟡 Circuit graph + settling & event-driven evaluators + snapshots implemented and CI-verified (PR #10, 62 tests); caching/suspension polish remains |
| **C** | Gates, sequential logic, buses, miniature surface placement (Part 3) | 🟡 Logic gates evaluated inside circuits; sequential components + surface placement not started |
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
- [x] **Build evidence:** `./gradlew build` green in GitHub Actions on PR #8
      ([run 39455939635](https://github.com/ransombiyato/dice-box.py/actions/runs/39455939635),
      3m28s; first run failed on missing JUnit dependencies — fixed and
      recorded in TESTING.md). The agent sandbox cannot run this locally
      (blocked Gradle/Maven/NeoForge/Mojang hosts, no JDK compiler).
- [ ] **Dev-launch evidence:** `./gradlew runClient` (or `runServer`) reaches
      the title screen / "Done" with the DiceBox load log line, on a machine
      with a display/JDK. Not claimable from the sandbox; instructions in
      README. (Optional stretch: headless CI `runServer` smoke job.)

## Phase B — Simulation engine

Slice 1 (done with A, 35 tests at the time): `SignalValue` (multi-bit &
unknown semantics, encoding round-trip), `EventScheduler` (deterministic
ordering, budgets), `Terminal`, `ConnectionValidator` (width/direction
diagnostics), `NetResolver` (conflicting-driver & floating behavior),
`ClockWaveform`, `AccuracyMode`.

Slice 2 (this change; sim test total now 62):

- [x] `CombinationalComponent` contract formalized; `LogicGate` implements
      it (by-terminal-name evaluation).
- [x] Circuit graph (`sim/circuit/Circuit`): nets + typed connections,
      eager width/uniqueness checks, structural validation (unconnected
      terminals, undriven nets, multiple drivers), topology change API
      (disconnect / remove component / remove net).
- [x] Combinational settling (`CombinationalEvaluator`): synchronous
      rounds, hard iteration bound, seeded initial values; stable feedback
      settles (seeded NOR latch test), oscillating feedback is stopped and
      reported by net name (seeded NOT-ring test).
- [x] Event-driven evaluation (`EventDrivenCircuitSimulator`): fixed
      transport gate delay via `EventScheduler`, same-time FIFO policy,
      strict time + event budgets with exact pause/resume, conflict/
      floating visibility, processed-event counts (`RunOutcome`).
- [x] Snapshot save/restore of quiescent simulator state (time, nets,
      drivers; consistency-checked, version header). Snapshotting with
      pending events fails loudly.

Remaining in Phase B (polish, after Phase C starts):

1. Graph caching / incremental re-evaluation for large circuits.
2. Suspension/resumption of *idle machines* (needs machine/world concepts
   from later phases; a settled simulator already costs nothing per call).
3. Richer execution-cost reporting for a future per-tick budgeter
   (currently: per-call processed/pending event counts).

## Phase C — Logic components & surface placement

Done: combinational `LogicGate` (all 8 kinds, truth-table tests), now
runnable inside real circuits via both evaluators (half-adder and
gate-wired latch tests). Remaining: a sequential component contract, then
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

1. Land the Phase B slice-2 PR with green CI; record the run in TESTING.md.
2. Owner or agent with a desktop JDK: run `./gradlew runClient`, confirm the
   load log line, record it in TESTING.md (completes Phase A acceptance —
   still open).
3. Phase C slice 1: sequential component contract (clocked `step`-style
   semantics on the event-driven simulator) + SR latch, D latch/flip-flop
   with state-transition tests; verify the latch components against the
   gate-wired equivalents already tested.
4. Phase C slice 2: arithmetic (adders, comparator), registers/counters,
   shift registers; then surface-placement design for the Minecraft side.
