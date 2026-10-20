package dev.minetech.core;

/**
 * A single pin on a part.
 *
 * <p>Coordinates are in a 16x16 grid covering the surface of the block the part
 * is mounted on, matching the texture space of the part so the pin on the model
 * lines up with the pin in the circuit.
 *
 * @param name label such as {@code in}, {@code out}, {@code q0}
 * @param x    column, 0 (left) to 15 (right)
 * @param y    row, 0 (top) to 15 (bottom)
 * @param dir  whether the pin is an input, an output, or bi-directional
 */
public record PortDef(String name, int x, int y, Dir dir) {

    public enum Dir {
        IN,
        OUT,
        BIDIR
    }

    public boolean canRead() {
        return dir != Dir.OUT;
    }

    public boolean canWrite() {
        return dir != Dir.IN;
    }
}
