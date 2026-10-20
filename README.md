# DiceBox Computers

**A Minecraft mod (Java 1.21.1 + NeoForge) about building actual computers** —
from transistors and logic gates up through CPUs, memory, storage, operating
systems, and one day real compatible software. It is built one tested
milestone at a time, survival-first, and this README is the running handoff
document: it always describes what is *really* in the repo, not the dream.

> **Status (2026-10-20): Milestone A + first slices of B/C.**
> The repository was reset from its previous life (a Python project) into a
> NeoForge MDK project. What exists today is the buildable foundation plus a
> tested, pure-Java simulation core (signals, event scheduler, nets, gates).
> **There is no in-game content yet** — no blocks, items, GUIs, or computers.
> Anyone telling you otherwise is selling something.

The complete owner specification (Parts 1–10) is stored in
[`docs/MASTER_SPEC.md`](docs/MASTER_SPEC.md). Phase tracking lives in
[`docs/ROADMAP.md`](docs/ROADMAP.md), design in
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), evidence in
[`docs/TESTING.md`](docs/TESTING.md).

## What works right now

- **NeoForge project skeleton** for Minecraft 1.21.1 / NeoForge 21.1.176 /
  ModDevGradle 2.0.91 / Java 21 (from the official NeoForged MDK
  `archive/1.21-mdg`), Gradle wrapper 8.14.2, mod id `dicebox`. The mod
  entrypoint loads and logs; it deliberately registers no content.
- **Simulation core** (`src/main/java/io/github/ransombiyato/dicebox/sim`,
  pure Java, zero Minecraft dependencies):
  multi-bit signals with unknown-bit semantics, a deterministic budgeted
  event scheduler, typed terminals + connection diagnostics, net resolution
  with conflict/floating detection, a clock waveform model, accuracy modes,
  and full combinational logic gates (BUFFER/NOT/AND/OR/NAND/NOR/XOR/XNOR).
- **Automated tests:** 35 JUnit 5 tests over the simulation core
  (`src/test/java/...`), plus 9 Python tests for the dev tool below. CI runs
  everything on each pull request.
- **ASCII screen tool** (`tools/ascii_screen.py`): renders PNM
  framebuffers/screenshots as ASCII art and maps cells back to pixels, for
  developing display code headlessly. (It reads image files; it cannot drive
  a real Minecraft client.)

## What does *not* exist yet

Everything else in the spec: circuit graph evaluation, sequential logic,
surface placement of miniature components, memory, CPUs (8085 → 6502 →
2A03), storage/USB-style transfer, firmware/OS boot, physical PC parts,
graphics devices, power/thermals, schematics, survival progression — and
therefore, obviously, DOOM. See the roadmap for order and status.

## Requirements

- **JDK 21** (Gradle can auto-provision it via the Foojay toolchain resolver)
- Network access on first build (Gradle, Maven Central, NeoForge, Mojang)
- For `runClient`: a machine with a display

No Dependabot: the owner has removed it and does not want it back. Do not
add `.github/dependabot.yml`.

## Build, test, run

```bash
./gradlew build       # compile, run JUnit tests, produce the mod jar (build/libs)
./gradlew test        # simulation-core tests only
./gradlew runClient   # dev client: expect the "DiceBox Computers loaded" log line
./gradlew runServer   # dev dedicated server (--nogui)
python3 tools/test_ascii_screen.py                    # dev-tool tests
python3 tools/ascii_screen.py some-frame.ppm --cols 100 --probe 12,4
```

Verification note: the agent sandbox that performed the reset cannot reach
the hosts Gradle needs (nor install a JDK compiler), so Java
compile/test/build evidence for this milestone comes from GitHub Actions
(`.github/workflows/ci.yml`), and the dev launch must be confirmed once on a
real desktop JDK. This is recorded, not papered over, in
[`docs/TESTING.md`](docs/TESTING.md).

## Project layout

```
.
├── build.gradle / settings.gradle / gradle.properties   # MDG build; mod identity in gradle.properties
├── src/main/java/io/github/ransombiyato/dicebox/
│   ├── DiceBoxMod.java        # @Mod entrypoint (no content registered yet)
│   └── sim/                   # pure-Java simulation core (NEVER import Minecraft here)
├── src/main/templates/META-INF/neoforge.mods.toml       # expanded by generateModMetadata
├── src/main/resources/assets/dicebox/lang/en_us.json
├── src/test/java/.../sim/      # JUnit 5 sim-core tests
├── tools/                      # ascii_screen.py + its unittest suite
├── docs/                       # MASTER_SPEC, ARCHITECTURE, ROADMAP, TESTING
└── .github/workflows/ci.yml    # JDK 21 build+test, Python tool tests
```

## Handoff snapshot

- **Reset:** previous Python implementation removed 2026-10-20 per owner
  instruction (spec Part 1). Recoverable at commit
  `4d4f761e21a222e35f3717690d0429f4bafc7b35` (`main`, pre-reset). Working tree
  was clean; nothing uncommitted was lost. `.git` untouched.
- **Next tasks:** (1) land the foundation PR with green CI; (2) confirm
  `runClient` once on a desktop and record it in TESTING.md; (3) Roadmap
  Phase B next slice — circuit graph, combinational settling with feedback
  policy, event-driven evaluation, snapshots. Then Phase C sequential logic.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Short version: small tested phases,
never fake functionality, never claim what tests don't prove, update README
+ ROADMAP after every meaningful phase.

## License

[MIT](LICENSE). The NeoForged MDK template scaffolding this project started
from can be used without restriction (see the MDK repository). No
copyrighted game data, ROMs, or proprietary OS images are (or will be)
bundled; future software support is bring-your-own legally obtained files.
