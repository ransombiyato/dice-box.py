# Changelog

All notable changes are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Phase B slice 2 (circuit graph & evaluators): `CombinationalComponent`
  contract, `sim.circuit` package with `Circuit`, `TerminalRef`,
  `CircuitProblem`, synchronous `CombinationalEvaluator` (bounded settling,
  seeded feedback, oscillation reporting), and
  `EventDrivenCircuitSimulator` (transport gate delays, mandatory time/event
  budgets, quiescent snapshot save/restore). 27 new JUnit tests (62 total),
  including half-adder, seeded NOR-latch and oscillating-ring coverage.
- `EventScheduler(long initialTime)` constructor for snapshot restoration.

- Fresh NeoForge project for Minecraft Java 1.21.1 (NeoForge 21.1.176,
  ModDevGradle 2.0.91, Java 21, Gradle wrapper 8.14.2) based on the official
  NeoForged MDK, mod id `dicebox` ("DiceBox Computers").
- `DiceBoxMod` entrypoint that loads and logs, registering no content yet.
- Pure-Java simulation core (`io.github.ransombiyato.dicebox.sim`):
  `SignalValue` (multi-bit 0/1/unknown signals), `EventScheduler`
  (deterministic, budgeted), `Terminal`, `ConnectionValidator`,
  `NetResolver` (driver conflicts/floating), `ClockWaveform`,
  `AccuracyMode`, `SimComponent`, and `LogicGate` (8 gate kinds).
- 35 JUnit 5 tests for the simulation core; Gradle `test` task + GitHub
  Actions CI (JDK 21 build/test, Python tool tests).
- `tools/ascii_screen.py` (PNM → ASCII framebuffer viewer with
  cell→pixel mapping) with 9 passing stdlib-unittest tests.
- Docs: README (handoff), `docs/MASTER_SPEC.md` (owner's Parts 1–10 spec,
  minus a redacted credential; Part 8 was not supplied),
  `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/TESTING.md`.

### Removed

- The previous Python project (`pyproject.toml`, `src/dicebox/*.py`,
  `tests/test_cli.py`, Python CI). Recoverable at the pre-reset commit
  `4d4f761e21a222e35f3717690d0429f4bafc7b35` on `main`.

### Notes

- No Dependabot configuration exists in this repository (removed earlier at
  the owner's request), and none was re-added.
- Nothing in-game is implemented yet; see `docs/ROADMAP.md` for phase status.

[Unreleased]: https://github.com/ransombiyato/dice-box.py/compare/HEAD
