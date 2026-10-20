"""Unit tests for tools/ascii_screen.py (stdlib unittest, no dependencies).

Run from the repository root:
    python3 tools/test_ascii_screen.py
"""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from ascii_screen import cell_to_pixels, parse_pnm, render_ascii  # noqa: E402


def make_p6(width: int, height: int, rgb: bytes) -> bytes:
    return f"P6\n{width} {height}\n255\n".encode("ascii") + rgb


class ParsePnmTest(unittest.TestCase):
    def test_p6_black_and_white(self) -> None:
        data = make_p6(2, 1, bytes([0, 0, 0, 255, 255, 255]))
        width, height, gray = parse_pnm(data)
        self.assertEqual((2, 1), (width, height))
        self.assertEqual([0, 255], gray)

    def test_p5_with_comment(self) -> None:
        data = b"P5\n# a comment\n3 1\n255\n" + bytes([0, 128, 255])
        width, height, gray = parse_pnm(data)
        self.assertEqual((3, 1), (width, height))
        self.assertEqual([0, 128, 255], gray)

    def test_p3_ascii(self) -> None:
        data = b"P3 1 1 255 255 0 0"
        width, height, gray = parse_pnm(data)
        self.assertEqual((1, 1), (width, height))
        # Rec.709 luma of pure red: round(0.2126 * 255) = 54
        self.assertEqual([54], gray)

    def test_bad_magic_rejected(self) -> None:
        with self.assertRaises(ValueError):
            parse_pnm(b"P9 1 1 255 0")

    def test_truncated_raster_rejected(self) -> None:
        with self.assertRaises(ValueError):
            parse_pnm(b"P6 2 2 255\n" + bytes([0, 0, 0]))


class RenderTest(unittest.TestCase):
    def test_black_white_row(self) -> None:
        art = render_ascii([0, 255], 2, 1, cols=2, rows=1)
        self.assertEqual(" @", art)

    def test_invert(self) -> None:
        art = render_ascii([0, 255], 2, 1, cols=2, rows=1, invert=True)
        self.assertEqual("@ ", art)

    def test_cell_to_pixels(self) -> None:
        x0, y0, x1, y1, cx, cy = cell_to_pixels(
            2, 1, cols=10, rows=5, width=100, height=50
        )
        self.assertEqual((20, 10, 30, 20), (x0, y0, x1, y1))
        self.assertEqual((24, 14), (cx, cy))

    def test_cell_out_of_bounds(self) -> None:
        with self.assertRaises(ValueError):
            cell_to_pixels(10, 0, cols=10, rows=5, width=100, height=50)


if __name__ == "__main__":
    unittest.main()
