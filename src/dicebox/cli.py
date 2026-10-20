"""Command line entry point for dicebox.

The project is a fresh start: this CLI is intentionally a thin, well-behaved
shell that new features can be layered onto.
"""

from __future__ import annotations

import argparse
import sys
from collections.abc import Sequence

from . import __version__


def build_parser() -> argparse.ArgumentParser:
    """Build the argument parser used by :func:`main`."""
    parser = argparse.ArgumentParser(
        prog="dicebox",
        description="A clean, fresh Python project.",
    )
    parser.add_argument(
        "--version",
        action="version",
        version=f"%(prog)s {__version__}",
    )
    parser.add_argument(
        "command",
        nargs="?",
        default="hello",
        choices=["hello"],
        help="the command to run (default: hello)",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    """Run the CLI and return a process exit code."""
    args = build_parser().parse_args(argv)
    if args.command == "hello":
        print(f"hello from dicebox {__version__} — this project is a blank slate.")
    return 0


if __name__ == "__main__":  # pragma: no cover
    sys.exit(main())
