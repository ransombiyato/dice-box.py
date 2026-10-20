package io.github.ransombiyato.dicebox.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A combinational logic gate over fixed-width signals: the first real
 * component built on the simulation core (early milestone C).
 *
 * <p>{@link #evaluate} is a pure function of its inputs, including the
 * three-valued unknown propagation defined by {@link SignalValue}. Applying
 * propagation <i>delay</i> (scheduling the output change through
 * {@link EventScheduler}) is the circuit evaluator's job and lands with the
 * graph evaluator; this class owns only the gate's logic function and its
 * terminals ({@code in0..inN-1}, {@code out}).
 */
public final class LogicGate implements SimComponent {
    public enum Kind {
        BUFFER,
        NOT,
        AND,
        OR,
        NAND,
        NOR,
        XOR,
        XNOR
    }

    private final String id;
    private final Kind kind;
    private final int width;
    private final int inputCount;
    private final List<Terminal> terminals;

    public LogicGate(String id, Kind kind, int width, int inputCount) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        if (id.isBlank()) {
            throw new IllegalArgumentException("component id must not be blank");
        }
        // Validates width range.
        SignalValue.unknown(width);
        boolean unary = kind == Kind.BUFFER || kind == Kind.NOT;
        if (unary && inputCount != 1) {
            throw new IllegalArgumentException(kind + " requires exactly 1 input: " + inputCount);
        }
        if (!unary && inputCount < 2) {
            throw new IllegalArgumentException(kind + " requires at least 2 inputs: " + inputCount);
        }
        this.id = id;
        this.kind = kind;
        this.width = width;
        this.inputCount = inputCount;
        List<Terminal> terms = new ArrayList<>(inputCount + 1);
        for (int i = 0; i < inputCount; i++) {
            terms.add(new Terminal("in" + i, Terminal.Direction.INPUT, width));
        }
        terms.add(new Terminal("out", Terminal.Direction.OUTPUT, width));
        this.terminals = List.copyOf(terms);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<Terminal> terminals() {
        return terminals;
    }

    public Kind kind() {
        return kind;
    }

    public int width() {
        return width;
    }

    public int inputCount() {
        return inputCount;
    }

    /**
     * Computes the output for the given inputs (in terminal order
     * {@code in0..inN-1}). All inputs must match the gate width.
     */
    public SignalValue evaluate(List<SignalValue> inputs) {
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.size() != inputCount) {
            throw new IllegalArgumentException(
                    "gate " + id + " expects " + inputCount + " inputs, got " + inputs.size());
        }
        for (SignalValue input : inputs) {
            Objects.requireNonNull(input, "input");
            if (input.width() != width) {
                throw new IllegalArgumentException(
                        "gate " + id + " expects input width " + width + ", got " + input.width());
            }
        }
        switch (kind) {
            case BUFFER:
                return inputs.get(0);
            case NOT:
                return inputs.get(0).not();
            case AND: {
                SignalValue result = inputs.get(0);
                for (int i = 1; i < inputs.size(); i++) {
                    result = result.and(inputs.get(i));
                }
                return result;
            }
            case OR: {
                SignalValue result = inputs.get(0);
                for (int i = 1; i < inputs.size(); i++) {
                    result = result.or(inputs.get(i));
                }
                return result;
            }
            case XOR: {
                SignalValue result = inputs.get(0);
                for (int i = 1; i < inputs.size(); i++) {
                    result = result.xor(inputs.get(i));
                }
                return result;
            }
            case NAND: {
                SignalValue result = inputs.get(0);
                for (int i = 1; i < inputs.size(); i++) {
                    result = result.and(inputs.get(i));
                }
                return result.not();
            }
            case NOR: {
                SignalValue result = inputs.get(0);
                for (int i = 1; i < inputs.size(); i++) {
                    result = result.or(inputs.get(i));
                }
                return result.not();
            }
            case XNOR: {
                SignalValue result = inputs.get(0);
                for (int i = 1; i < inputs.size(); i++) {
                    result = result.xor(inputs.get(i));
                }
                return result.not();
            }
            default:
                throw new IllegalStateException("unhandled gate kind: " + kind);
        }
    }
}
