package dev.minetech.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A wall of parts and the nets between them.
 *
 * <p>The engine is a fixed-point evaluator: every tick, each part pushes its
 * outputs and each net resolves from the drivers it received. That repeats
 * until the values stop changing, which mirrors how a real combinational
 * circuit settles after a gate delay. A circuit that never settles (a NOT gate
 * wired into itself, say) is reported as unstable instead of hanging the game.
 *
 * <p>Everything here is plain Java with no Minecraft types, so it can be
 * exercised by unit tests in seconds.
 */
public final class Circuit {

    /** Give up on settling after this many passes. */
    public static final int MAX_SETTLE_PASSES = 32;

    private final List<Net> nets = new ArrayList<>();
    private final List<Part> parts = new ArrayList<>();
    private int[] previous = new int[0];
    private boolean stable = true;
    private long passes;

    // ---- construction -----------------------------------------------------

    /** Add a part and give each of its pins its own net. */
    public Part add(PartType type) {
        Part part = new Part(parts.size(), type);
        int[] pins = part.portNets();
        for (int i = 0; i < pins.length; i++) {
            Net net = new Net(nets.size());
            nets.add(net);
            pins[i] = net.id();
        }
        parts.add(part);
        return part;
    }

    /** Wire two pins together. */
    public void connect(Part a, String portA, Part b, String portB) {
        int netA = portNet(a, portA);
        int netB = portNet(b, portB);
        int rootA = find(netA);
        int rootB = find(netB);
        if (rootA != rootB) {
            nets.get(rootB).setParent(rootA);
        }
    }

    /** Wire a pin to an already-resolved net id. */
    public void connectToNet(Part part, String port, int netId) {
        int mine = portNet(part, port);
        int rootA = find(mine);
        int rootB = find(netId);
        if (rootA != rootB) {
            nets.get(rootB).setParent(rootA);
        }
    }

    private int portNet(Part part, String port) {
        int index = part.type().portIndex(port);
        if (index < 0) {
            throw new IllegalArgumentException(part.type().id() + " has no port " + port);
        }
        return part.portNets()[index];
    }

    private int find(int netId) {
        int root = netId;
        while (nets.get(root).parentId() != root) {
            root = nets.get(root).parentId();
        }
        int walk = netId;
        while (nets.get(walk).parentId() != walk) {
            int next = nets.get(walk).parentId();
            nets.get(walk).setParent(root);
            walk = next;
        }
        return root;
    }

    // ---- inspection -------------------------------------------------------

    public List<Part> parts() {
        return parts;
    }

    public List<Net> nets() {
        return nets;
    }

    public int partCount() {
        return parts.size();
    }

    public int netCount() {
        return nets.size();
    }

    /** Resolved level on a pin. Unconnected pins read 0. */
    public int read(Part part, int portIndex) {
        int net = part.portNets()[portIndex];
        if (net < 0) {
            return 0;
        }
        return nets.get(find(net)).value();
    }

    /** Convenience: read by pin name. */
    public int read(Part part, String port) {
        int index = part.type().portIndex(port);
        return index < 0 ? 0 : read(part, index);
    }

    void drive(Part part, int portIndex, int level) {
        int net = part.portNets()[portIndex];
        if (net < 0) {
            return;
        }
        nets.get(find(net)).drive(level);
    }

    /** True when the last settle reached a fixed point. */
    public boolean isStable() {
        return stable;
    }

    /** Number of evaluation passes used by the last settle. */
    public long lastPassCount() {
        return passes;
    }

    // ---- simulation -------------------------------------------------------

    /** Advance one tick: state machines first, then settle combinational logic. */
    public void tick() {
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            if (part.type().sequential()) {
                part.onTick(this);
            }
        }
        settle();
    }

    /**
     * Evaluate until the net values stop changing.
     *
     * @return true if a fixed point was reached
     */
    public boolean settle() {
        int count = nets.size();
        if (previous.length != count) {
            previous = new int[count];
        }
        long used = 0;

        for (int pass = 0; pass < MAX_SETTLE_PASSES; pass++) {
            used++;
            for (int i = 0; i < count; i++) {
                nets.get(i).resetDrivers();
            }
            for (int i = 0; i < parts.size(); i++) {
                parts.get(i).evaluate(this);
            }
            boolean changed = false;
            for (int i = 0; i < count; i++) {
                Net net = nets.get(i);
                changed |= net.resolve();
            }

            boolean sameAsBefore = true;
            for (int i = 0; i < count; i++) {
                if (nets.get(i).value() != previous[i]) {
                    sameAsBefore = false;
                    break;
                }
            }
            if (sameAsBefore && !changed) {
                stable = true;
                passes = used;
                return true;
            }
            for (int i = 0; i < count; i++) {
                previous[i] = nets.get(i).value();
            }
        }

        stable = false;
        passes = used;
        return false;
    }
}
