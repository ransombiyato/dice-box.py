# Testing

## What exists

| Suite | Where | What it proves |
| --- | --- | --- |
| JUnit 5 sim-core tests | `src/test/java/io/github/ransombiyato/dicebox/sim/` (6 classes) | Signal logic incl. unknown propagation, scheduler ordering/budgets, net conflict/floating resolution, connection diagnostics, clock edges, gate truth tables, encode/decode round-trip |
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
| 2026-10-20 | GitHub Actions (JDK 21 Temurin), PR #8 run [39455939635](https://github.com/ransombiyato/dice-box.py/actions/runs/39455939635) | `./gradlew build` | ✅ **Passed in 3m28s** after the fix (`f691159`): main+test compilation, all 35 JUnit tests (the `build` task fails if any test fails), mod jar artifact uploaded. Dev launch (`runClient`) still pending a desktop with a display |

**Sandbox limitation (recorded honestly, per spec):** the coding-agent
sandbox that produced Phase A has outbound network access only to
github.com / pypi.org / npm and no installable JDK compiler (Debian mirrors
blocked; GitHub release-asset downloads blocked; the PyPI-provided Java is a
JRE without `javac`). Gradle itself, ModDevGradle, NeoForge and Minecraft
artifacts all live on other hosts. Therefore the Java code in this milestone
was **not** compiled locally — its compile/test evidence comes from GitHub
Actions, and any failure there must be fixed before merging. A dev launch
(`runClient`) additionally needs a display, which the sandbox lacks; see
ROADMAP Phase A acceptance.

## Future suites (per spec Parts 2–9, added with their features)

Circuit graph/feedback/snapshot tests; sequential state-transition tests;
CPU reference vectors per architecture (instruction, flags, addressing,
interrupts, timing claims); storage/enumeration/boot tests; schematic
capture/obstruction/rollback tests; GameTests for in-world behavior;
reproducible performance benchmarks with documented budgets. No benchmark
numbers exist yet, and none may be invented.
