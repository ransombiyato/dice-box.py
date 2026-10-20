# Master specification (Parts 1–10)

Stored 2026-10-20 at the repository owner's request so future agents do not
lose it between sessions. This records the Parts 1–10 prompt as given, with
the owner's prose condensed only where it repeated itself. **Not stored:**
credentials (a token pasted with the prompt was redacted and must be revoked —
never commit credentials) and process instructions that conflict with the
hosting platform's fixed session/branch rules.

> Note: the prompt as received jumps from Part 7 to Part 9. **Part 8 was not
> included.** If the owner supplies Part 8, insert it here and in the roadmap.

**Product:** a substantial Minecraft Java Edition mod that simulates ACTUAL
COMPUTERS. Target: **Minecraft Java 1.21.1 + a compatible NeoForge version**,
correct Java toolchain (Java 21) and Gradle configuration. Survival-friendly,
visually polished, extensible. Players eventually: build computers from
transistors and gates; construct CPUs/GPUs; install operating systems;
transfer files on removable media; manufacture RAM and expansion cards; run
compatible real software, including a legally supported path toward DOOM.

Owner side-recommendation: build a dev script that maps a screen to ASCII art
with click/keybind coordinate ability, to inspect the game headlessly.
(Status: `tools/ascii_screen.py` implements image→ASCII + cell→pixel
mapping; live client capture/input needs a future client-side bridge.)

---

## Part 1/10: Clean reset and project foundation

Mandatory first task — reset the repository:

1. Inspect root, Git status/branches/history, tracked/untracked/ignored
   files, build config, tests, source, assets, README, environment files.
2. Identify what belongs to the project and what must not be deleted.
3. Report intended destructive scope; preserve a recoverable reference to
   valuable uncommitted work where appropriate.
4. Perform the clean scratch reset of the old implementation within the
   verified repository root. Remove obsolete implementation files.
5. Preserve Git metadata unless re-initialization is necessary and explicitly
   authorized. Never delete credentials, unrelated files, or anything outside
   the repository. No blind destructive Git commands.
6. If the reset cannot safely be performed, explain the limitation and use a
   clearly documented safe alternative.
7. Create a fresh, reproducible Minecraft 1.21.1 + NeoForge Gradle project.
   Verify it builds and launches before major features.

Do not just write a plan. Perform the work.

### Engineering rules

- Small, testable phases. Never fake functionality (no decorative blocks,
  canned output, hardcoded demos presented as real).
- Do not claim unimplemented features are complete. Test changes; fix
  failures where feasible.
- After every meaningful phase update `README.md` and `docs/ROADMAP.md`:
  current state, verified tests, limitations, changed files, architectural
  decisions, next tasks. Leave the repo reproducible for another agent.

### Architecture

Separate the simulation engine from Minecraft rendering/world updates. Clear
modules for: core component contracts & configuration; digital logic,
signals, terminals, connections, buses; event scheduling, clocks, simulation
budgets; CPU emulation & hardware; memory, storage, filesystems, software
loading; power, heat, manufacturing; peripherals & displays; miniature
surface components; schematics & transactional placement; Minecraft world
integration, rendering, GUI, persistence, multiplayer networking.

Document in `docs/ARCHITECTURE.md`. Avoid many empty classes for show.
Every simulated component: stable identifiers, typed terminals, connection
rules, state handling, serialization, diagnostics, measurable sim costs.

### Required initial deliverables

Safe reset; clean NeoForge project; successful build and dev launch; basic
automated test infrastructure; `README.md`; `docs/ARCHITECTURE.md`;
`docs/ROADMAP.md`. Then Part 2. Do not claim the full mod is finished.

## Part 2/10: Simulation engine

Do not reset again. Dedicated simulation engine, independent of Minecraft
rendering/block-updates.

- **Digital simulation:** single- & multi-bit signals; typed terminals and
  buses; high/low plus unknown/invalid states where useful; combinational &
  sequential logic; event scheduling, configurable propagation delays;
  clocked components; defined behavior for feedback, conflicting drivers,
  simultaneous events; deterministic evaluation, reproducible tests; circuit
  graph caching & incremental topology updates where justified.
- **Accuracy levels:** 1 Educational, 2 Functional, 3 Timing-aware,
  4 Cycle-accurate. Never call something cycle-accurate without tests
  against trusted references.
- **Performance:** configurable clock rates; event-driven execution; bounded
  work per server tick; simulation budgets & component limits; fast-forward
  idle devices; suspend/resume inactive machines; chunk load/unload behavior;
  persistent snapshots/safe restore; profiling for large circuits/multiple
  computers. Never an unlimited sim loop on the server thread. Worker threads
  only with a deliberate thread-safety design; Minecraft world mutations obey
  NeoForge/Minecraft threading rules.
- **Diagnostics APIs:** inspect signals/bus values; trace connections; find
  disconnected terminals; detect invalid widths & conflicting drivers;
  measure scheduled events & execution costs; clear error reporting.
- **Tests:** scheduler ordering, signal behavior, clocks, feedback handling,
  graph changes, save/restore. Update README, ARCHITECTURE, ROADMAP,
  `docs/TESTING.md`; record real results. Proceed to Part 3.

## Part 3/10: Logic components and miniature surface placement

- **Components (functional):** transistors & supported transistor models;
  NOT/AND/OR/NAND/NOR/XOR/XNOR/buffer gates; switches, buttons, inverters,
  appropriate passives; latches & flip-flops; clock generators; mux/demux,
  encoders/decoders; adders/subtractors/comparators; counters & shift
  registers; registers, ALUs, control logic, bus drivers, tri-state where
  supported. Start with truth-table and state-transition tests.
- Construction at multiple abstraction levels (transistors → gates →
  subcircuits → standard blocks → ICs → CPUs); expansion of higher-level
  components into lower-level designs when supported; behavioral-equivalence
  verification.
- **Multiple miniature components per block face (mandatory):** many per
  face, all six faces. Unified surface placement: fine configurable grid;
  local surface coordinates; rotation (+ optional free rotation); snapping,
  selection, movement, removal; footprints & overlap detection; pins/
  terminals/connection geometry; placement previews; persistent positions &
  config; server-authoritative multiplayer. Not a separate block per tiny
  piece. Compact versioned container (e.g. block entity, efficient indexing/
  serialization); limits per face/per block. Physical position separate from
  logical circuit identity; moves preserve valid connections and report
  invalidated ones.
- **Wiring:** visible wires, PCB traces, board-mounted connections, multi-bit
  buses, backplane/connector links. Terminal direction/width/compatibility/
  signal semantics.
- **Visuals:** recognizable models/textures (gates, transistors, ICs, pins,
  sockets, traces, LEDs, connectors); mesh reuse & batching; no primitive
  placeholder cubes as final models.
- Tests: overlap, rotations, invalid connections, graph changes, persistence,
  large surface counts. Update README/handoff. Proceed to Part 4.

## Part 4/10: Memory, CPU emulation and custom hardware

Real behavior, not decoration.

- **Memory:** registers, SRAM, DRAM, ROM, flash/EEPROM-like; configurable
  caches; memory controllers & address decoding; buses, access timing,
  memory-mapped I/O; configurable capacity/latency/bandwidth; persistent vs
  volatile; architecture-specific memory maps. Efficient representation
  (arrays/pages/sparse); no huge host buffers per simulated device by default.
- **Craftable RAM modules:** realistic sticks incl. 1/2/4 GB tiers, older
  modules & historically appropriate tech — simulated capacities, not host
  RAM allocations. Generations, form factors, socket compatibility, timing,
  bandwidth, power, manufacturing costs. Incompatible RAM must actually fail.
- **CPU framework:** separate implementations behind a common interface —
  registers, decoding, flags, addressing modes, interrupts, memory access,
  I/O, reset, stepping, timing, debugging, persistence. Stages:
  1. Intel 8085 (documented coverage, memory/I/O tests);
  2. Generic MOS 6502 (addressing, flags, interrupts);
  3. NES Ricoh 2A03 as distinct implementation with its real differences and
     NES hardware interfaces;
  4. Later only when foundations justify: Z80, 68000, selected x86.
  Never treat 8085 / generic 6502 / 2A03 as interchangeable. Validate with
  trusted reference vectors, test programs, edge cases, timing tests;
  document unsupported instructions/approximations.
- **Custom CPUs:** assembled from registers/ALUs/decoders/control/counters/
  buses/memory interfaces; documented custom-ISA design system with validated
  encodings & semantics; must execute actual configured instructions. No
  claiming arbitrary transistor layouts magically become CPUs.
- **Custom GPUs:** framebuffer memory, display controllers, graphics
  commands, pixel formats, palettes, device memory, power/performance;
  manufacturable from logic+memory+controllers+board+connectors+power;
  historical adapter examples. No unimplemented commercial-GPU/driver claims.
- **Motherboards:** CPU sockets, RAM/expansion slots, storage/power
  connectors, buses, controllers, external I/O; validated compatibility with
  explained failures.
- Tests: memory, CPU instructions, custom instruction semantics, hardware
  compatibility. Update README, `docs/HARDWARE.md`, roadmap. Part 5.

## Part 5/10: USB, storage, firmware and operating systems (core)

- **Storage/removable media:** craftable USB-style flash drives; removable
  devices/memory cards; persistent drives & disk images; ROM/firmware
  storage; controllers; filesystem formatting & file ops; capacity tiers,
  read/write & performance characteristics; safe insert/remove with defined
  disconnect behavior; persistence across saves/restarts. Players transfer
  files, programs, firmware, disk images, OS install media between computers.
- **USB-style framework:** not a decorative rectangle. Documented simulated
  protocol: host controller, identification/enumeration, connect/disconnect,
  endpoints/transfers, storage commands, capabilities/error handling, driver
  interfaces, multiplayer-safe state/persistence. Simplified USB-inspired
  protocol OK if documented honestly; never call it standards-compliant USB
  unless it is. Also serial/parallel/expansion-bus/networking as appropriate.
- **Boot/OS:** genuine architecture-appropriate sequence: reset → firmware →
  hardware discovery → memory/device init → boot-device selection →
  bootloader → executable/kernel load → OS init → drivers → errors/recovery.
  Don't force modern BIOS/UEFI onto all retro machines. Define executable &
  firmware formats, FS APIs, memory layouts, load addresses, entry points,
  devices, compatibility metadata. Original small demo OS/monitor with
  source, reproducible build, bootable images for supported targets, console,
  commands, memory/device inspection, file ops where supported. Additional
  OSes via documented interfaces without invasive core changes.
- **Legal:** user-supplied legally obtained ROMs/images/programs supported;
  only legally distributable original examples bundled; never unauthorized
  copyrighted game data or proprietary OS images. Imports are untrusted:
  validate format/size, no path traversal, never execute as host code.
  Document supported architectures/formats precisely; no arbitrary-OS claims.
- **DOOM:** long-term compatibility target, not a fake menu item. Needs real
  CPU/executable environment/memory/storage/graphics/input/timing (audio if
  claimed). Legally distributable source/original implementation; users
  supply game data when required; no unauthorized assets. Repeatable
  compatibility tests for every claimed software target.
- Tests: transfer, enumeration, persistence, boot failures, installation,
  import validation. Add `docs/OS_AND_SOFTWARE.md`. Part 6.

## Part 6/10: Physical computers, graphics, power and thermals

- **Physical PC building:** cases/chassis; motherboards+mounting; CPUs,
  coolers/heatsinks; fans, PSUs; RAM sticks; graphics/expansion cards;
  storage drives; cables/connectors/buttons/LEDs; front panels & I/O ports;
  rack servers & racks; labels/screws/brackets/vents/decor. Individual
  placement, custom builds, recognizable Minecraft-style models/textures.
  Historical examples; clearly distinguish real specs vs game equivalents.
- **Graphics:** character displays, monochrome, palette retro, framebuffer,
  configurable pixel formats, color displays, display timings. Real output
  from simulated software; device memory/controllers/drivers/performance.
  Historical adapters → custom GPU manufacturing.
- **Hybrid power:** generators, batteries, PSUs, regulators, cables/
  connectors, device budgets, voltage compatibility, efficiency losses,
  overload/brownouts, configurable consumption. Logic uses efficient logical
  signals by default; optional analog only for selected components with
  defined limits. No costly analog per ordinary gate by default.
- **Thermals:** simplified configurable heat/cooling/fan/overheat/throttling.
  Meaningful without forcing thermodynamics on every circuit.
- **Manufacturing:** craft components, PCBs, ICs, CPUs, GPUs, RAM, cooling,
  cases, server hardware via coherent recipes/assembly; testing/repair/
  replacement where appropriate.
- Tests: compatibility, power failure, cooling, actual display output. Part 7.

## Part 7/10: Engineering schematics and survival gameplay (both core)

- **Schematic tool (survival-craftable):** select, capture, save, preview,
  rotate, mirror, copy, place reusable constructions. Captures blocks/
  multiblocks; surface components, positions, orientations; wires/traces/
  buses/terminal mappings; circuit connectivity; hardware config & relevant
  persistent state; reusable subcircuits/board designs. Defined transient-
  state policy (e.g. document whether volatile RAM is omitted). Versioned
  format with integrity checks & migration; never execute schematic code.
- **Obstruction/compatibility checker:** validate the ENTIRE destination and
  all affected faces before placement: obstructions; existing surface
  components/overlaps; support/mounting; incompatible blocks/slots/
  components; world height/boundaries; connection geometry; missing
  materials; version incompatibility; protection/permissions. Visual preview
  distinguishing valid/obstructed/incompatible/missing; explain every error;
  complete material manifest vs inventory before placement.
- **Safe placement (survival):** consume materials; no free duplication;
  server-side inventory/permission validation; transactional/atomic where
  feasible; else rollback + refund; no half-built machines after failures;
  cancellation, progress, size limits, interruption recovery. Creative
  bypasses material cost but not compatibility/protection rules. Rotation,
  mirroring, subcircuits, blueprint libraries, repeatable save/load.
- **Survival progression tiers:**
  1. Basic materials, conductors, switches, lamps, batteries.
  2. Transistors, passives, gates, test equipment.
  3. Adders, registers, ALUs, ICs, PCBs, memory.
  4. CPUs, RAM, storage, displays, basic programmable computers.
  5. Firmware, USB-style storage, OS installation, expansion cards, graphics.
  6. Custom motherboards/CPUs/GPUs, manufacturing, advanced cooling.
  7. Servers, racks, networking, automation, large infrastructure.
  Costs challenging but achievable; processing, fabrication, assembly,
  repair, advanced manufacturing. Useful early computers; creative mode;
  engineering workspace; server-configurable progression/size limits.
- Tests: obstruction, missing materials, overlap, rollback, cancellation,
  corruption, repeat placement, persistence, multiplayer security.
  Add `docs/SCHEMATICS.md`. Part 8 (missing — see note at top).

## Part 9/10: Testing, assets and acceptance criteria

- **Visual quality:** original/properly licensed textures/models/UI for
  components, cases, RAM, GPUs, PCBs, connectors, monitors, servers, decor.
  Recognizable silhouettes, coherent materials, readable labels, consistent
  scale, mesh reuse/batching/culling/LOD where supported. No crude
  placeholders as finished assets. Document third-party assets/licenses.
- **Automated testing coverage:** circuit logic (truth tables, arithmetic,
  sequential state, bus widths/compatibility, feedback, connection errors,
  surface overlap/placement, save/restore); CPU (reference vectors,
  registers/flags/addressing, interrupts/memory maps, quirks, timing claims,
  debugger stepping, runnable programs); storage/OS (FS ops, enumeration/
  transfer, persistence, boot detection, firmware/bootloader, executable
  loading, invalid images, display/input); schematics (capture/serialize,
  rotate/mirror, connection preservation, obstructions, missing materials,
  rollback/cancellation, corrupted/oversized, repeat save/load/placement,
  multiplayer permissions); world (save/reload, chunk unload, power loss,
  invalid configs, resource accounting, server restart).
- **Performance benchmarks (reproducible, never invented):** large circuits;
  dense surface placement; concurrent CPUs; large simulated memory; multiple
  computers; schematic preview/placement; world save/load; server tick &
  memory impact, with documented budgets.
- **Documentation set:** README (install/build/launch/features/limitations/
  next), ARCHITECTURE, ROADMAP, HARDWARE, OS_AND_SOFTWARE, SCHEMATICS,
  TESTING. Update after every phase; test before claiming completion.
  Done = implementation + tests + error handling + persistence (where
  applicable) + documentation. Compilation alone proves nothing.
- Part 10.

## Part 10/10: Execution roadmap and final rules

Same repository/project; never reset again after Part 1.

### Required implementation order

A. Clean baseline, NeoForge build & tests.
B. Simulation interfaces, signal engine, event scheduler, diagnostics.
C. Gates, sequential logic, buses, miniature surface placement.
D. Memory, persistent storage, RAM modules, PCBs.
E. Intel 8085 with reference tests & runnable programs.
F. First complete programmable computer (memory, firmware/monitor, display,
   input, persistence).
G. Schematic tool, obstruction validation, materials, rollback, survival
   progression.
H. Removable media, USB-style controller, file transfer, boot pipeline,
   original demo OS.
I. Generic 6502 & NES 2A03, graphics devices, custom CPU/GPU construction,
   expansion hardware.
J. Advanced manufacturing, racks, networking, power, cooling, optional analog.
K. Real-software compatibility, DOOM target, polish, optimization, release
   validation.

Subdivide any phase that is too large to implement & validate safely.

### Phase acceptance (every phase)

Inspect repo+README; identify unfinished dependencies; implement next
coherent limited feature set; add tests; run tests+build; fix failures;
record actual outcomes incl. failures; update README+ROADMAP; update
subsystem docs; record changed files/interfaces/decisions/limitations/next
tasks; leave repo reproducible. Don't write docs instead of code, and don't
attempt the whole project in one giant change. Continue autonomously.

### Honesty requirements — never claim

- CPU instruction support without implementing+testing it;
- a computer boots when it only shows a scripted screen;
- an OS installs without a genuine loader/storage pipeline;
- USB works when the port is decorative;
- a GPU works when the display is hardcoded;
- a custom CPU executes without implemented semantics;
- schematics preserve connections when they don't;
- placement is safe without destination checks;
- performance is fine without reproducible measurements;
- DOOM runs without a real compatible execution path.

Report partial implementations accurately; list unsupported behavior.

### Final definition of done

A polished survival-friendly computer-engineering mod: circuits at
transistor/gate/IC/CPU levels; multiple miniature components per block face;
computers from compatible physical parts; manufacturable RAM/vintage CPUs/
GPUs/PCBs/custom hardware; real CPU instruction execution; installable
compatible software & original demo OSes; functional removable-storage file
transfer; displays/input/expansion/peripherals; craftable schematic tool with
obstruction & missing-material detection; large systems with power, cooling,
networking, servers; coherent survival progression; high-quality visuals.
DOOM last — first the CPU/memory/storage/graphics/input/software it requires.

### Immediate action

Begin/continue the current milestone from actual repo state. First message:
Part 1 inspection + safe reset. Later: inspect README/code, verify completed
work, continue next unfinished phase. Don't repeat completed work. README.md
is the persistent handoff. Build the real system, one tested milestone at a
time.
