#!/usr/bin/env python3
"""Render a screen image as ASCII art for headless inspection.

Development aid for DiceBox Computers: simulated displays produce
framebuffers long before they are wired to an in-game screen, and developers
(including coding agents in headless sandboxes) cannot look at a Minecraft
window. This tool converts an image saved in PNM format (PBM is not
supported; P2/P3/P5/P6 are) into ASCII art, and maps any ASCII cell back to
the exact source pixel rectangle slash center pixel.

Honest scope: this reads image *files*. It does not attach to, screenshot, or
send keyboard/mouse input to a running Minecraft client. Doing that will
require a client-side bridge inside the mod (export the framebuffer to a
file, and accept input events) - see docs/ROADMAP.md. The cell-to-pixel
mapping provided here is the coordinate conversion such a bridge needs to
turn "click what is shown at ASCII column 12, row 4" into a source pixel.

Usage:
    python3 tools/ascii_screen.py frame.ppm [--cols 100] [--rows 40]
    python3 tools/ascii_screen.py frame.ppm --probe 12,4

Exit code 0 on success, 2 on usage/input errors (via argparse/ValueError).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

# Brightness ramp, darkest (space) to brightest (@).
RAMP = " .:-=+*#%@"


def _next_token(data: bytes, pos: int) -> tuple[str, int]:
    """Read one whitespace/comment-delimited token starting at ``pos``."""
    n = len(data)
    # Skip whitespace and '#' comments (to end of line).
    while pos < n:
        byte = data[pos : pos + 1]
        if byte == b"#":
            while pos < n and data[pos : pos + 1] not in (b"\n", b"\r"):
                pos += 1
        elif byte.isspace():
            pos += 1
        else:
            break
    start = pos
    while pos < n and not data[pos : pos + 1].isspace() and data[pos : pos + 1] != b"#":
        pos += 1
    if start == pos:
        raise ValueError("unexpected end of PNM data while reading a token")
    return data[start:pos].decode("ascii"), pos


def parse_pnm(data: bytes) -> tuple[int, int, list[int]]:
    """Parse P2/P3/P5/P6 data. Returns ``(width, height, grayscale_0_to_255)``.

    Row-major order, top row first. Color is converted using Rec.709 luma.
    Raises ``ValueError`` on malformed input.
    """
    magic, pos = _next_token(data, 0)
    if magic not in ("P2", "P3", "P5", "P6"):
        raise ValueError(f"unsupported PNM magic {magic!r} (expected P2/P3/P5/P6)")
    width_s, pos = _next_token(data, pos)
    height_s, pos = _next_token(data, pos)
    maxval_s, pos = _next_token(data, pos)
    width, height, maxval = int(width_s), int(height_s), int(maxval_s)
    if width <= 0 or height <= 0:
        raise ValueError(f"invalid image dimensions {width}x{height}")
    if maxval <= 0 or maxval > 65535:
        raise ValueError(f"invalid maxval {maxval}")

    channels = 3 if magic in ("P3", "P6") else 1
    samples_needed = width * height * channels
    samples: list[int] = []

    if magic in ("P5", "P6"):
        # Exactly one whitespace byte separates the header from the raster.
        if data[pos : pos + 2] == b"\r\n":
            pos += 2
        else:
            pos += 1
        raster = data[pos:]
        if maxval > 255:
            if len(raster) < samples_needed * 2:
                raise ValueError("truncated binary PNM raster")
            samples = [
                (raster[i] << 8) | raster[i + 1] for i in range(0, samples_needed * 2, 2)
            ]
        else:
            if len(raster) < samples_needed:
                raise ValueError("truncated binary PNM raster")
            samples = list(raster[:samples_needed])
    else:
        while len(samples) < samples_needed:
            token, pos = _next_token(data, pos)
            samples.append(int(token))

    gray: list[int] = []
    if channels == 1:
        gray = [min(255, s * 255 // maxval) for s in samples]
    else:
        for i in range(0, samples_needed, 3):
            r, g, b = samples[i], samples[i + 1], samples[i + 2]
            luma = int(round(0.2126 * r + 0.7152 * g + 0.0722 * b))
            gray.append(min(255, luma * 255 // maxval))
    return width, height, gray


def default_rows(width: int, height: int, cols: int) -> int:
    """Row count preserving proportions for terminal characters (~2:1 tall)."""
    return max(1, round(cols * height / width * 0.5))


def cell_to_pixels(
    col: int, row: int, *, cols: int, rows: int, width: int, height: int
) -> tuple[int, int, int, int, int, int]:
    """Map an ASCII cell to its source pixel rectangle and center pixel.

    Returns ``(x0, y0, x1, y1, center_x, center_y)`` where ``x1``/``y1`` are
    exclusive. Raises ``ValueError`` if the cell is outside the grid.
    """
    if not (0 <= col < cols and 0 <= row < rows):
        raise ValueError(f"cell ({col},{row}) outside {cols}x{rows} grid")
    x0 = col * width // cols
    x1 = max(x0 + 1, (col + 1) * width // cols)
    y0 = row * height // rows
    y1 = max(y0 + 1, (row + 1) * height // rows)
    return x0, y0, x1, y1, (x0 + x1 - 1) // 2, (y0 + y1 - 1) // 2


def render_ascii(
    gray: list[int],
    width: int,
    height: int,
    *,
    cols: int = 80,
    rows: int | None = None,
    invert: bool = False,
) -> str:
    """Render grayscale pixels (0=black .. 255=white) as ASCII lines."""
    if cols <= 0:
        raise ValueError(f"cols must be positive: {cols}")
    if len(gray) != width * height:
        raise ValueError(f"expected {width * height} pixels, got {len(gray)}")
    if rows is None:
        rows = default_rows(width, height, cols)
    if rows <= 0:
        raise ValueError(f"rows must be positive: {rows}")

    ramp = RAMP[::-1] if invert else RAMP
    last = len(ramp) - 1
    lines: list[str] = []
    for row in range(rows):
        chars: list[str] = []
        for col in range(cols):
            x0, y0, x1, y1, _, _ = cell_to_pixels(
                col, row, cols=cols, rows=rows, width=width, height=height
            )
            total = 0
            count = 0
            for y in range(y0, min(y1, height)):
                base = y * width
                for x in range(x0, min(x1, width)):
                    total += gray[base + x]
                    count += 1
            average = total // count if count else 0
            chars.append(ramp[average * last // 255])
        lines.append("".join(chars))
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="ascii_screen",
        description="Render a PNM screen image (P2/P3/P5/P6) as ASCII art.",
    )
    parser.add_argument("image", type=Path, help="path to a .ppm/.pgm image")
    parser.add_argument("--cols", type=int, default=80, help="ASCII columns (default: 80)")
    parser.add_argument("--rows", type=int, default=None, help="ASCII rows (default: auto)")
    parser.add_argument("--invert", action="store_true", help="invert the brightness ramp")
    parser.add_argument(
        "--probe",
        metavar="COL,ROW",
        default=None,
        help="after rendering, print the source pixel rectangle and center for this cell",
    )
    args = parser.parse_args(argv)

    try:
        data = args.image.read_bytes()
        width, height, gray = parse_pnm(data)
        rows = args.rows if args.rows is not None else default_rows(width, height, args.cols)
        print(
            render_ascii(gray, width, height, cols=args.cols, rows=rows, invert=args.invert)
        )
        if args.probe is not None:
            col_s, _, row_s = args.probe.partition(",")
            col, row = int(col_s), int(row_s)
            x0, y0, x1, y1, cx, cy = cell_to_pixels(
                col, row, cols=args.cols, rows=rows, width=width, height=height
            )
            print(
                f"cell ({col},{row}) -> pixels x={x0}..{x1 - 1}, y={y0}..{y1 - 1}; "
                f"center=({cx},{cy}); "
                f"normalized=({cx / max(1, width - 1):.3f},{cy / max(1, height - 1):.3f})"
            )
    except (OSError, ValueError) as exc:
        print(f"ascii_screen: error: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
