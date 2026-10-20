"""Smoke tests for the dicebox CLI."""

from __future__ import annotations

import pytest

from dicebox import __version__
from dicebox.cli import main


def test_version_is_exposed() -> None:
    assert __version__ == "0.1.0"


def test_hello_command_prints_greeting(capsys: pytest.CaptureFixture[str]) -> None:
    assert main([]) == 0
    out = capsys.readouterr().out
    assert "dicebox" in out
    assert __version__ in out


def test_explicit_hello_command(capsys: pytest.CaptureFixture[str]) -> None:
    assert main(["hello"]) == 0
    assert "blank slate" in capsys.readouterr().out


def test_version_flag_exits_zero(capsys: pytest.CaptureFixture[str]) -> None:
    with pytest.raises(SystemExit) as excinfo:
        main(["--version"])
    assert excinfo.value.code == 0
    assert __version__ in capsys.readouterr().out


def test_unknown_command_is_rejected() -> None:
    with pytest.raises(SystemExit) as excinfo:
        main(["definitely-not-a-command"])
    assert excinfo.value.code == 2
