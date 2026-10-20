package dev.minetech.core;

/**
 * A single electrical node.
 *
 * <p>Nets are merged with union-find when two pins are wired together, so a
 * long chain of wire parts costs one lookup rather than one hop per segment.
 *
 * <p>Digital nets resolve with wired-OR semantics: if any driver is high the
 * net is high, which is how Minecraft redstone dust already behaves and keeps
 * hand-wired contraptions predictable.
 */
public final class Net {

    private final int id;
    private int parent;
    private int value;
    private int driverHigh;
    private int driverCount;

    Net(int id) {
        this.id = id;
        this.parent = id;
    }

    public int id() {
        return id;
    }

    int parentId() {
        return parent;
    }

    void setParent(int parent) {
        this.parent = parent;
    }

    /** Resolved level of this net, 0 or 1. */
    public int value() {
        return value;
    }

    /** True when more than one part is driving this net. */
    public boolean contested() {
        return driverCount > 1;
    }

    void resetDrivers() {
        driverHigh = 0;
        driverCount = 0;
    }

    void drive(int level) {
        driverCount++;
        if (level != 0) {
            driverHigh++;
        }
    }

    /** Recompute the level from the drivers registered this pass. */
    boolean resolve() {
        int next = driverHigh > 0 ? 1 : 0;
        if (next != value) {
            value = next;
            return true;
        }
        return false;
    }
}
