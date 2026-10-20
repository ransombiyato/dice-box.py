# Contributing

DiceBox Computers is built in small, tested phases (see
[`docs/MASTER_SPEC.md`](docs/MASTER_SPEC.md) and
[`docs/ROADMAP.md`](docs/ROADMAP.md)). Contributions follow the same rules as
the spec's engineering requirements.

## Getting set up

```bash
git clone https://github.com/ransombiyato/dice-box.py.git
cd dice-box.py
./gradlew build        # JDK 21; first run downloads NeoForge/Minecraft artifacts
./gradlew runClient    # dev client
```

## Non-negotiables

1. **Never fake functionality.** No decorative blocks, canned output, or
   hardcoded demos presented as real behavior.
2. **Never claim what tests don't prove.** A CPU instruction, a boot
   process, a transfer protocol, "cycle-accurate", "DOOM runs" — each claim
   needs implementing code *and* automated tests behind it.
3. **The `sim` package stays pure Java.** Nothing under
   `io.github.ransombiyato.dicebox.sim` may import Minecraft or NeoForge
   classes. Simulation first, world integration second.
4. **Bounded work only.** No unbounded simulation loops; engine calls take
   explicit budgets. No world mutation off the server thread.
5. **No Dependabot.** The owner removed it; don't re-add
   `.github/dependabot.yml`. Dependency updates are manual, reviewed PRs.
6. **No secrets in the repo.** Ever. Tokens, keys, and credentials do not go
   in code, docs, issues, or commit messages.
7. **No copyrighted payloads.** Don't bundle game ROMs, proprietary OS
   images, or unlicensed assets. Originals or properly licensed only.

## Workflow

1. Branch off `main` (`feature/…`, `fix/…`, `chore/…`); no direct commits to
   `main`.
2. Implement the next coherent, limited slice of the current roadmap phase.
3. Add/update automated tests for the behavior you changed.
4. Run `./gradlew build` locally (needs a desktop-class JDK 21 + network).
5. Update `README.md`, `docs/ROADMAP.md`, and the relevant subsystem docs in
   the same PR; record real test results in `docs/TESTING.md`.
6. Open a PR describing *what* changed, *why*, and *how it was verified*
   (including failures/limitations).

## Checks

CI (`.github/workflows/ci.yml`) runs `./gradlew build` on JDK 21 and the
Python tool tests on every PR. Both must be green to merge.

## Reporting bugs

Use the **Bug report** issue template with Minecraft/NeoForge/mod/Java
versions, steps to reproduce, and relevant log excerpts.

## License

Contributions are licensed under the MIT License, same as the project.
