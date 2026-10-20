package dev.minetech.core;

import java.util.Arrays;

/**
 * One placed part: a gate, a latch, a lamp, a wire.
 *
 * <p>A part owns no nets of its own beyond the ids of the pins it sits on, so a
 * whole wall of components is a handful of flat int arrays rather than a graph
 * of objects. That is what keeps big contraptions cheap to tick.
 */
public final class Part {

    private final int id;
    private final PartType type;
    private final int[] portNets;
    private int state;
    private boolean manualHigh;
    private int clockPeriod = 20;
    private int clockPhase;
    private int lastClock;
    private int gridX;
    private int gridY;
    private int rotation;

    Part(int id, PartType type) {
        this.id = id;
        this.type = type;
        this.portNets = new int[type.portCount()];
        Arrays.fill(portNets, -1);
    }

    // ---- identity ---------------------------------------------------------

    public int id() {
        return id;
    }

    public PartType type() {
        return type;
    }

    int[] portNets() {
        return portNets;
    }

    void setPortNet(int port, int net) {
        portNets[port] = net;
    }

    // ---- placement --------------------------------------------------------

    public int gridX() {
        return gridX;
    }

    public int gridY() {
        return gridY;
    }

    public int rotation() {
        return rotation;
    }

    public Part placeAt(int x, int y, int rotationQuarterTurns) {
        this.gridX = x;
        this.gridY = y;
        this.rotation = Math.floorMod(rotationQuarterTurns, 4);
        return this;
    }

    // ---- manual controls --------------------------------------------------

    public boolean manualHigh() {
        return manualHigh;
    }

    public void setManualHigh(boolean high) {
        this.manualHigh = high;
    }

    public void toggleManual() {
        this.manualHigh = !manualHigh;
    }

    /** Press-and-release behaviour for the button part. */
    public void pulse() {
        this.manualHigh = true;
    }

    public void release() {
        this.manualHigh = false;
    }

    public int clockPeriod() {
        return clockPeriod;
    }

    public void setClockPeriod(int ticks) {
        this.clockPeriod = Math.max(2, ticks);
    }

    // ---- simulation -------------------------------------------------------

    /** Advance internal state. Called once per tick, before settling. */
    void onTick(Circuit circuit) {
        switch (type) {
            case CLOCK -> {
                clockPhase = (clockPhase + 1) % clockPeriod;
                if (clockPhase == 0) {
                    state ^= 1;
                }
            }
            case D_FLIP_FLOP -> {
                int clk = circuit.read(this, index("clk"));
                boolean rising = lastClock == 0 && clk != 0;
                lastClock = clk;
                if (rising) {
                    state = circuit.read(this, index("d"));
                }
            }
            case COUNTER_4BIT -> {
                int clk = circuit.read(this, index("clk"));
                boolean rising = lastClock == 0 && clk != 0;
                lastClock = clk;
                if (circuit.read(this, index("rst")) != 0) {
                    state = 0;
                } else if (rising) {
                    state = (state + 1) & 0xF;
                }
            }
            default -> {
                // stateless parts keep nothing between ticks
            }
        }
    }

    /** Push this part's outputs onto its pins. */
    void evaluate(Circuit circuit) {
        switch (type) {
            case WIRE, BUFFER -> circuit.drive(this, 1, circuit.read(this, 0));
            case NOT -> circuit.drive(this, 1, circuit.read(this, 0) ^ 1);
            case AND -> circuit.drive(this, 2, gateAnd(circuit));
            case OR -> circuit.drive(this, 2, gateOr(circuit));
            case XOR -> circuit.drive(this, 2, gateXor(circuit));
            case NAND -> circuit.drive(this, 2, gateAnd(circuit) ^ 1);
            case NOR -> circuit.drive(this, 2, gateOr(circuit) ^ 1);
            case XNOR -> circuit.drive(this, 2, gateXor(circuit) ^ 1);
            case HALF_ADDER -> {
                int a = circuit.read(this, 0);
                int b = circuit.read(this, 1);
                circuit.drive(this, 2, a ^ b);
                circuit.drive(this, 3, a & b);
            }
            case FULL_ADDER -> {
                int a = circuit.read(this, 0);
                int b = circuit.read(this, 1);
                int cin = circuit.read(this, 2);
                int sum = a ^ b ^ cin;
                int carry = (a & b) | (a & cin) | (b & cin);
                circuit.drive(this, 3, sum);
                circuit.drive(this, 4, carry);
            }
            case SR_LATCH -> {
                int s = circuit.read(this, 0);
                int r = circuit.read(this, 1);
                if (s != 0 && r == 0) {
                    state = 1;
                } else if (r != 0 && s == 0) {
                    state = 0;
                }
                // s and r asserted together hold the previous state, like a
                // real latch refusing an illegal input
                circuit.drive(this, 2, state);
                circuit.drive(this, 3, state ^ 1);
            }
            case D_FLIP_FLOP -> {
                circuit.drive(this, 2, state);
                circuit.drive(this, 3, state ^ 1);
            }
            case COUNTER_4BIT -> {
                // reset is asynchronous: it clears the register the moment it
                // goes high, without waiting for a clock edge
                if (circuit.read(this, 1) != 0) {
                    state = 0;
                }
                circuit.drive(this, 2, state & 1);
                circuit.drive(this, 3, (state >> 1) & 1);
                circuit.drive(this, 4, (state >> 2) & 1);
                circuit.drive(this, 5, (state >> 3) & 1);
            }
            case BUTTON, LEVER -> circuit.drive(this, 0, manualHigh ? 1 : 0);
            default -> {
                // lamps and displays are read by the renderer, not by the net
            }
        }
    }

    private int gateAnd(Circuit circuit) {
        return circuit.read(this, 0) & circuit.read(this, 1);
    }

    private int gateOr(Circuit circuit) {
        return circuit.read(this, 0) | circuit.read(this, 1);
    }

    private int gateXor(Circuit circuit) {
        return circuit.read(this, 0) ^ circuit.read(this, 1);
    }

    private int index(String port) {
        return type.portIndex(port);
    }

    // ---- introspection for the renderer and the copy tool -----------------

    /** Current internal state, e.g. the value held by a latch or counter. */
    public int state() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
