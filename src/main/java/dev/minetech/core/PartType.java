package dev.minetech.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The catalogue of parts.
 *
 * <p>Each entry fixes a stable string id (used by blueprints, so saved
 * contraptions survive updates), the pins, and whether the part is
 * sequential (has state that only changes on a clock edge).
 *
 * <p>Pin layout is deliberately plain: inputs on the left edge, outputs on the
 * right edge, so hand-wiring a block reads top-to-bottom like a schematic.
 */
public enum PartType {

    // ---- passive / wiring -------------------------------------------------
    WIRE("wire", Category.PASSIVE, false,
            new PortDef("a", 1, 8, PortDef.Dir.BIDIR),
            new PortDef("b", 14, 8, PortDef.Dir.BIDIR)),

    BUFFER("buffer", Category.PASSIVE, false,
            new PortDef("in", 3, 8, PortDef.Dir.IN),
            new PortDef("out", 12, 8, PortDef.Dir.OUT)),

    // ---- logic gates ------------------------------------------------------
    NOT("not", Category.LOGIC, false,
            new PortDef("in", 3, 8, PortDef.Dir.IN),
            new PortDef("out", 12, 8, PortDef.Dir.OUT)),

    AND("and", Category.LOGIC, false, gatePorts()),
    OR("or", Category.LOGIC, false, gatePorts()),
    XOR("xor", Category.LOGIC, false, gatePorts()),
    NAND("nand", Category.LOGIC, false, gatePorts()),
    NOR("nor", Category.LOGIC, false, gatePorts()),
    XNOR("xnor", Category.LOGIC, false, gatePorts()),

    // ---- arithmetic -------------------------------------------------------
    HALF_ADDER("half_adder", Category.ARITHMETIC, false,
            new PortDef("a", 2, 12, PortDef.Dir.IN),
            new PortDef("b", 2, 4, PortDef.Dir.IN),
            new PortDef("sum", 13, 4, PortDef.Dir.OUT),
            new PortDef("carry", 13, 12, PortDef.Dir.OUT)),

    FULL_ADDER("full_adder", Category.ARITHMETIC, false,
            new PortDef("a", 2, 12, PortDef.Dir.IN),
            new PortDef("b", 2, 8, PortDef.Dir.IN),
            new PortDef("cin", 2, 4, PortDef.Dir.IN),
            new PortDef("sum", 13, 4, PortDef.Dir.OUT),
            new PortDef("carry", 13, 12, PortDef.Dir.OUT)),

    // ---- memory / sequential ---------------------------------------------
    SR_LATCH("sr_latch", Category.MEMORY, true,
            new PortDef("s", 2, 12, PortDef.Dir.IN),
            new PortDef("r", 2, 4, PortDef.Dir.IN),
            new PortDef("q", 13, 4, PortDef.Dir.OUT),
            new PortDef("qn", 13, 12, PortDef.Dir.OUT)),

    D_FLIP_FLOP("d_flip_flop", Category.MEMORY, true,
            new PortDef("d", 2, 12, PortDef.Dir.IN),
            new PortDef("clk", 2, 4, PortDef.Dir.IN),
            new PortDef("q", 13, 4, PortDef.Dir.OUT),
            new PortDef("qn", 13, 12, PortDef.Dir.OUT)),

    COUNTER_4BIT("counter_4bit", Category.MEMORY, true,
            new PortDef("clk", 2, 12, PortDef.Dir.IN),
            new PortDef("rst", 2, 4, PortDef.Dir.IN),
            new PortDef("q0", 13, 13, PortDef.Dir.OUT),
            new PortDef("q1", 13, 10, PortDef.Dir.OUT),
            new PortDef("q2", 13, 7, PortDef.Dir.OUT),
            new PortDef("q3", 13, 4, PortDef.Dir.OUT)),

    // ---- I/O --------------------------------------------------------------
    BUTTON("button", Category.IO, false,
            new PortDef("out", 13, 8, PortDef.Dir.OUT)),

    LEVER("lever", Category.IO, false,
            new PortDef("out", 13, 8, PortDef.Dir.OUT)),

    LAMP("lamp", Category.IO, false,
            new PortDef("in", 3, 8, PortDef.Dir.IN)),

    CLOCK("clock", Category.IO, true,
            new PortDef("out", 13, 8, PortDef.Dir.OUT)),

    SEVEN_SEGMENT("seven_segment", Category.IO, false,
            new PortDef("a", 2, 12, PortDef.Dir.IN),
            new PortDef("b", 2, 9, PortDef.Dir.IN),
            new PortDef("c", 2, 6, PortDef.Dir.IN),
            new PortDef("d", 2, 3, PortDef.Dir.IN));

    /** Broad grouping, used for the creative tab and for recipe families. */
    public enum Category {
        PASSIVE,
        LOGIC,
        ARITHMETIC,
        MEMORY,
        IO,
        ANALOG
    }

    private static final Map<String, PartType> BY_ID = new HashMap<>();

    static {
        for (PartType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    private final String id;
    private final Category category;
    private final boolean sequential;
    private final PortDef[] ports;

    PartType(String id, Category category, boolean sequential, PortDef... ports) {
        this.id = id;
        this.category = category;
        this.sequential = sequential;
        this.ports = ports;
    }

    private static PortDef[] gatePorts() {
        return new PortDef[] {
                new PortDef("a", 2, 12, PortDef.Dir.IN),
                new PortDef("b", 2, 4, PortDef.Dir.IN),
                new PortDef("out", 13, 8, PortDef.Dir.OUT)
        };
    }

    /** Stable id used in blueprints and registry names. */
    public String id() {
        return id;
    }

    public Category category() {
        return category;
    }

    /** True when the part only changes state on a clock edge. */
    public boolean sequential() {
        return sequential;
    }

    public int portCount() {
        return ports.length;
    }

    /** Pin definition by index, in declaration order. */
    public PortDef port(int index) {
        return ports[index];
    }

    /** Pin index by name, or -1 when the part has no such pin. */
    public int portIndex(String name) {
        for (int i = 0; i < ports.length; i++) {
            if (ports[i].name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** Look up a part by its stable id, or {@code null}. */
    public static PartType byId(String id) {
        return BY_ID.get(id);
    }
}
