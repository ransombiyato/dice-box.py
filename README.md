# dice-box.py

> A clean, fresh start.

This repository has been reset to a blank slate: the previous single-file
prototype has been removed and replaced with a small, well-structured Python
project skeleton — packaging, tests, linting, CI, and docs — ready for whatever
gets built next.

## Status

🚧 **Fresh start.** The package currently ships a placeholder CLI and nothing
else. Everything below describes the scaffolding, not finished features.

## Requirements

- Python 3.9 or newer
- [uv](https://docs.astral.sh/uv/) or `pip` for dependency management

## Getting started

```bash
# clone
git clone https://github.com/ransombiyato/dice-box.py.git
cd dice-box.py

# create an environment and install the project with dev dependencies
uv venv && uv pip install -e ".[dev]"
# ...or with plain pip:
python -m venv .venv && . .venv/bin/activate && pip install -e ".[dev]"

# run the CLI
dicebox            # or: python -m dicebox
dicebox --version
```

## Development

```bash
pytest              # run the test suite
ruff check .        # lint
ruff format .       # format
```

All three run in CI on every push and pull request (see
[`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

## Project layout

```
.
├── .github/            # CI workflow, issue & PR templates, dependabot
├── src/dicebox/        # the package (src layout, installable)
│   ├── __init__.py
│   ├── __main__.py
│   └── cli.py
├── tests/              # pytest suite
├── pyproject.toml      # packaging, pytest & ruff configuration
├── CHANGELOG.md
├── CONTRIBUTING.md
└── LICENSE
```

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) first — it covers branching, commit
style, and the checks your pull request must pass.

## License

Released under the [MIT License](LICENSE).
