# Testing

## What exists

| Suite | Where | What it proves |
| --- | --- | --- |
| JUnit 5 sim-core tests | `src/test/java/io/github/ransombiyato/dicebox/sim/` (6 classes) and `.../sim/circuit/` (3 classes); 62 tests total | Signal logic incl. unknown propagation, scheduler ordering/budgets, net conflict/floating resolution, connection diagnostics, clock edges, gate truth tables, encode/decode round-trip, circuit structure/validation/topology changes, combinational settling (half-adder, seeded NOR latch, oscillating NOT-ring), event-driven delay timing/budget pause-resume/snapshot-restore/corrupt-snapshot rejection |
| Python tool tests | `tools/test_ascii_screen.py` (9 tests, stdlib unittest) | PNM parsing (P3/P5/P6), ASCII rendering, cell→pixel mapping, error cases |

The sim tests deliberately touch **only** the pure-Java `sim` package: no
Minecraft runtime, no mocks of one. They are the behavioral evidence for the
engine slices they cover. They say nothing about in-game behavior (there is
no in-game content yet) and are not presented as doing so.

## How to run

Requires a JDK 21 (Gradle will also auto-provision a toolchain via the
Foojay resolver if your local JDK differs) and network access to Gradle /
Maven Central / NeoForge / Mojang artifact hosts on first run:

```bash
./gradlew test        # JUnit sim-core tests
./gradlew build       # tests + full mod jar build
python3 tools/test_ascii_screen.py
```

CI (`.github/workflows/ci.yml`) runs all of the above on every pull request
and uploads test reports/jars as artifacts on failure and success.

## Verified results (update this table with real runs only)

| Date | Environment | Command | Result |
| --- | --- | --- | --- |
| 2026-10-20 | Agent sandbox (Python 3.11) | `python3 tools/test_ascii_screen.py` | ✅ 9 tests passed; demo PGM rendered + probed correctly |
| 2026-10-20 | Agent sandbox | Java compile/test, Gradle build, dev launch | ⚠️ **Not runnable here** — see limitation below; verified in CI instead |
| 2026-10-20 | GitHub Actions (JDK 21 Temurin), PR #8 run [39455203420](https://github.com/ransombiyato/dice-box.py/actions/runs/39455203420) | `./gradlew build` | ❌ Failed before tests: JUnit dependencies were missing from `build.gradle`, so `compileTestJava` failed |
| 2026-10-20 | GitHub Actions (JDK 21 Temurin), PR #8 run [39455939635](https://github.com/ransombiyato/dice-box.py/actions/runs/39455939635) + final head run [39456635108](https://github.com/ransombiyato/dice-box.py/actions/runs/39456635108) | `./gradlew build` | ✅ **Passed** (3m28s; final head 2m31s) after the fix (`f691159`): main+test compilation, all 35 JUnit tests (the `build` task fails if any test fails), mod jar artifact uploaded; PR #8 merged to `main` as `014da73`. Dev launch (`runClient`) still pending a desktop with a display |
| 2026-10-21 | Agent sandbox | Phase B slice 2: tree-sitter Java grammar parse of all 27 Java files; grep checks (62 `@Test` methods; sim purity: no Minecraft/NeoForge imports) | ✅ 0 parse errors (syntax only — not compilation; see limitation below) |
| _(pending)_ | GitHub Actions (JDK 21 Temurin), Phase B slice-2 PR | `./gradlew build` | Link the run here when green; do not merge on red |

**Sandbox limitation (recorded honestly, per spec):** the coding-agent
sandbox producing these milestones has outbound network access only to
github.com / pypi.org / npm and no installable JDK compiler (Debian mirrors
blocked; GitHub release-asset downloads blocked; the PyPI-provided Java is a
JRE without `javac`). Gradle itself, ModDevGradle, NeoForge and Minecraft
artifacts all live on other hosts. Therefore the Java code is **not**
compiled locally in the sandbox — its compile/test evidence comes from
GitHub Actions (local checks are grammar-parsing and grep-level only), and
any CI failure must be fixed before merging. A dev launch (`runClient`)
additionally needs a display, which the sandbox lacks; see ROADMAP Phase A
acceptance.

## Future suites (per spec Parts 2–9, added with their features)

Sequential state-transition tests (latches/flip-flops/counters) with
equivalence checks against gate-wired versions; subcircuit expansion
equivalence; CPU reference vectors per architecture (instruction, flags,
addressing, interrupts, timing claims); storage/enumeration/boot tests;
schematic capture/obstruction/rollback tests; GameTests for in-world
behavior; reproducible performance benchmarks (large circuits, scheduling
throughput) with documented budgets. No benchmark numbers exist yet, and
none may be invented.
