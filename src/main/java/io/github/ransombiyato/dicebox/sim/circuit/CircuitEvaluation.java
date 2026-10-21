package io.github.ransombiyato.dicebox.sim.circuit;

import io.github.ransombiyato.dicebox.sim.SignalValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The outcome of one combinational evaluation of a circuit.
 *
 * @param netValues settled (or last) value of every net in the circuit
 * @param settled whether the circuit reached a fixed point within the
 *     iteration limit. When {@code false}, {@code unstableNets} names the
 *     nets still changing on the final iteration — typically an oscillating
 *     feedback loop for the given inputs/seed
 * @param iterations synchronous update rounds performed (&ge; 1 when the
 *     circuit has any net or component work to do)
 * @param conflictedNets nets whose drivers disagreed in the final state
 * @param floatingNets nets with no driver at all in the final state
 * @param unstableNets nets still changing when evaluation stopped
 * @param problems structural diagnostics from {@link Circuit#validate()}
 */
public record CircuitEvaluation(
        Map<String, SignalValue> netValues,
        boolean settled,
        int iterations,
        Set<String> conflictedNets,
        Set<String> floatingNets,
        Set<String> unstableNets,
        List<CircuitProblem> problems) {

    public CircuitEvaluation {
        Objects.requireNonNull(netValues, "netValues");
        Objects.requireNonNull(conflictedNets, "conflictedNets");
        Objects.requireNonNull(floatingNets, "floatingNets");
        Objects.requireNonNull(unstableNets, "unstableNets");
        Objects.requireNonNull(problems, "problems");
        if (iterations < 0) {
            throw new IllegalArgumentException("iterations must be >= 0: " + iterations);
        }
        netValues = Map.copyOf(netValues);
        conflictedNets = Set.copyOf(conflictedNets);
        floatingNets = Set.copyOf(floatingNets);
        unstableNets = Set.copyOf(unstableNets);
        problems = List.copyOf(problems);
    }

    /** Value of one net; fails if the evaluation has no such net. */
    public SignalValue value(String netName) {
        SignalValue value = netValues.get(netName);
        if (value == null) {
            throw new IllegalArgumentException("no such net in evaluation: " + netName);
        }
        return value;
    }
}
