package io.github.ransombiyato.dicebox.sim.circuit;

import io.github.ransombiyato.dicebox.sim.CombinationalComponent;
import io.github.ransombiyato.dicebox.sim.NetResolver;
import io.github.ransombiyato.dicebox.sim.SignalValue;
import io.github.ransombiyato.dicebox.sim.Terminal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Settles a combinational circuit to a fixed point by synchronous iteration.
 *
 * <p>Each round evaluates <b>every</b> component against the net values from
 * the end of the previous round, collects all drivers per net (component
 * outputs plus constant external drives), resolves nets with
 * {@link NetResolver}, and only then moves to the next round. Because updates
 * are simultaneous rather than in-place, the result does not depend on
 * component ordering, and the same circuit + inputs + seed always produce
 * the same result.
 *
 * <p>Feedback policy is explicit:
 *
 * <ul>
 *   <li>Feedback that reaches a fixed point (including "stable unknown")
 *       settles normally. Seeding a latch's nets via {@code initialValues}
 *       lets stable feedback hold a state.</li>
 *   <li>Feedback that keeps changing exhausts {@code maxIterations} and is
 *       reported as {@code settled=false} with the still-changing nets
 *       named — never as an infinite loop.</li>
 * </ul>
 *
 * This evaluator is stateless and delay-free (the EDUCATIONAL/FUNCTIONAL
 * view). Timed propagation lives in {@link EventDrivenCircuitSimulator}.
 */
public final class CombinationalEvaluator {
    public static final int DEFAULT_MAX_ITERATIONS = 1000;

    private CombinationalEvaluator() {}

    /** Settles a circuit from an all-unknown seed with the default iteration limit. */
    public static CircuitEvaluation evaluate(
            Circuit circuit, Map<String, SignalValue> externalInputs) {
        return evaluate(circuit, externalInputs, Map.of(), DEFAULT_MAX_ITERATIONS);
    }

    /**
     * Settles a circuit.
     *
     * @param externalInputs constant external drives by net name; applied as
     *     an extra driver on that net for the whole evaluation. A net driven
     *     only externally starts at its external value before round 1, in
     *     preference to any seed, since nothing else can influence it
     * @param initialValues seed values for nets before round 1 (others start
     *     fully unknown); used to model retained feedback state
     * @param maxIterations hard bound on synchronous rounds (&ge; 1)
     */
    public static CircuitEvaluation evaluate(
            Circuit circuit,
            Map<String, SignalValue> externalInputs,
            Map<String, SignalValue> initialValues,
            int maxIterations) {
        Objects.requireNonNull(circuit, "circuit");
        Objects.requireNonNull(externalInputs, "externalInputs");
        Objects.requireNonNull(initialValues, "initialValues");
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be >= 1: " + maxIterations);
        }
        checkValues(circuit, "external input", externalInputs);
        checkValues(circuit, "initial value", initialValues);

        Map<String, SignalValue> values = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> net : circuit.nets().entrySet()) {
            values.put(net.getKey(), SignalValue.unknown(net.getValue()));
        }
        values.putAll(initialValues);
        // A net driven *only* externally has that value at every instant,
        // including before round 1. Starting such nets at their external
        // value (instead of waiting a round for the first resolution)
        // matters for feedback: without it, an unknown standing in for a
        // constant input rides around a feedback loop in lockstep with the
        // synchronous rounds and even a holding latch reports as unstable.
        Set<String> componentDrivenNets = new LinkedHashSet<>();
        for (CombinationalComponent component : circuit.components()) {
            for (Terminal terminal : component.terminals()) {
                if (terminal.canDrive()) {
                    circuit.netOf(new TerminalRef(component.id(), terminal.name()))
                            .ifPresent(componentDrivenNets::add);
                }
            }
        }
        for (Map.Entry<String, SignalValue> drive : externalInputs.entrySet()) {
            if (!componentDrivenNets.contains(drive.getKey())) {
                values.put(drive.getKey(), drive.getValue());
            }
        }

        List<CircuitProblem> problems = circuit.validate();
        Map<String, NetResolver.Resolution> resolutions = Map.of();
        Set<String> changed = new LinkedHashSet<>();
        boolean settled = false;
        int iterations = 0;

        for (int round = 1; round <= maxIterations; round++) {
            iterations = round;
            Map<String, List<SignalValue>> drivers = new LinkedHashMap<>();
            for (String netName : circuit.nets().keySet()) {
                drivers.put(netName, new ArrayList<>());
            }
            for (Map.Entry<String, SignalValue> drive : externalInputs.entrySet()) {
                drivers.get(drive.getKey()).add(drive.getValue());
            }

            for (CombinationalComponent component : circuit.components()) {
                Map<String, SignalValue> inputs =
                        ComponentSupport.inputsFor(circuit, component, values);
                Map<String, SignalValue> outputs =
                        ComponentSupport.evaluateChecked(component, inputs);
                for (Terminal terminal : component.terminals()) {
                    if (!terminal.canDrive()) {
                        continue;
                    }
                    TerminalRef ref = new TerminalRef(component.id(), terminal.name());
                    circuit.netOf(ref)
                            .ifPresent(net -> drivers.get(net).add(outputs.get(terminal.name())));
                }
            }

            Map<String, NetResolver.Resolution> newResolutions = new LinkedHashMap<>();
            Map<String, SignalValue> newValues = new LinkedHashMap<>();
            changed = new LinkedHashSet<>();
            for (Map.Entry<String, Integer> net : circuit.nets().entrySet()) {
                NetResolver.Resolution resolution =
                        NetResolver.resolve(net.getValue(), drivers.get(net.getKey()));
                newResolutions.put(net.getKey(), resolution);
                newValues.put(net.getKey(), resolution.value());
                if (!resolution.value().equals(values.get(net.getKey()))) {
                    changed.add(net.getKey());
                }
            }
            values = newValues;
            resolutions = newResolutions;
            if (changed.isEmpty()) {
                settled = true;
                break;
            }
        }

        Set<String> conflicted = new LinkedHashSet<>();
        Set<String> floating = new LinkedHashSet<>();
        for (Map.Entry<String, NetResolver.Resolution> entry : resolutions.entrySet()) {
            if (entry.getValue().conflict()) {
                conflicted.add(entry.getKey());
            }
            if (entry.getValue().floating()) {
                floating.add(entry.getKey());
            }
        }
        return new CircuitEvaluation(
                values,
                settled,
                iterations,
                conflicted,
                floating,
                settled ? Set.of() : changed,
                problems);
    }

    private static void checkValues(
            Circuit circuit, String what, Map<String, SignalValue> values) {
        for (Map.Entry<String, SignalValue> entry : values.entrySet()) {
            Objects.requireNonNull(entry.getValue(), what + " value for " + entry.getKey());
            int expectedWidth = circuit.netWidth(entry.getKey()); // throws on unknown net
            if (entry.getValue().width() != expectedWidth) {
                throw new IllegalArgumentException(
                        what + " for net " + entry.getKey() + " is "
                                + entry.getValue().width() + " bits, expected " + expectedWidth);
            }
        }
    }
}
