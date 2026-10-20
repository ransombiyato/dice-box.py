# Contributing

Thanks for helping out! This project is a fresh start, so the conventions below
are simple on purpose.

## Getting set up

```bash
git clone https://github.com/ransombiyato/dice-box.py.git
cd dice-box.py
python -m venv .venv && . .venv/bin/activate
pip install -e ".[dev]"
```

## Workflow

1. **Branch** off `main` with a descriptive name (`feature/…`, `fix/…`,
   `chore/…`). Never commit directly to `main`.
2. **Keep commits small and focused.** Write imperative, present-tense subjects
   (`Add dice roller`, not `added dice roller`).
3. **Add tests** for anything you add or change. New behaviour without a test
   will not merge.
4. **Open a pull request** and describe *what* changed and *why*.

## Checks

Everything below runs in CI and must pass before a pull request can merge:

```bash
pytest
ruff check .
ruff format --check .
```

Format before you commit:

```bash
ruff format .
```

## Code style

- Follow the configuration in `pyproject.toml` — Ruff handles imports, linting,
  and formatting, so you rarely need to hand-format.
- Prefer type hints on public functions.
- Keep the `src/` layout: new modules go in `src/dicebox/`, new tests in
  `tests/`.

## Reporting bugs

Open an issue with the **Bug report** template. Include the Python version,
your OS, the command you ran, and what you expected to happen.

## License

By contributing, you agree that your contributions are licensed under the MIT
License, the same license as the project.
