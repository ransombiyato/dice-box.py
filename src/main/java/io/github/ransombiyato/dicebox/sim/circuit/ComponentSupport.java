package io.github.ransombiyato.dicebox.sim.circuit;

import io.github.ransombiyato.dicebox.sim.CombinationalComponent;
import io.github.ransombiyato.dicebox.sim.SignalValue;
import io.github.ransombiyato.dicebox.sim.Terminal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared plumbing between the evaluators: feeding a component from current
 * net values and checking its outputs against the combinational contract.
 * Package-private on purpose — not part of the public simulation API.
 */
final class ComponentSupport {
    private ComponentSupport() {}

    /**
     * Builds the input map for a component: every accepting terminal gets
     * its connected net's current value, or a fully-unknown value of the
     * terminal's width when unconnected.
     */
    static Map<String, SignalValue> inputsFor(
            Circuit circuit, CombinationalComponent component, Map<String, SignalValue> netValues) {
        Map<String, SignalValue> inputs = new LinkedHashMap<>();
        for (Terminal terminal : component.terminals()) {
            if (!terminal.canAccept()) {
                continue;
            }
            TerminalRef ref = new TerminalRef(component.id(), terminal.name());
            SignalValue value = circuit.netOf(ref)
                    .map(netValues::get)
                    .orElseGet(() -> SignalValue.unknown(terminal.width()));
            inputs.put(terminal.name(), value);
        }
        return inputs;
    }

    /**
     * Evaluates a component and validates that it produced a correctly-sized
     * value for every driving terminal. Contract breaches by component code
     * are programming errors and fail fast with {@link IllegalStateException}.
     */
    static Map<String, SignalValue> evaluateChecked(
            CombinationalComponent component, Map<String, SignalValue> inputs) {
        Map<String, SignalValue> outputs = component.evaluate(Map.copyOf(inputs));
        if (outputs == null) {
            throw new IllegalStateException(
                    "component " + component.id() + " returned null outputs");
        }
        for (Terminal terminal : component.terminals()) {
            if (!terminal.canDrive()) {
                continue;
            }
            SignalValue value = outputs.get(terminal.name());
            if (value == null) {
                throw new IllegalStateException(
                        "component " + component.id() + " produced no value for output "
                                + terminal.name());
            }
            if (value.width() != terminal.width()) {
                throw new IllegalStateException(
                        "component " + component.id() + " produced width " + value.width()
                                + " for " + terminal.name() + ", expected " + terminal.width());
            }
        }
        return outputs;
    }
}
