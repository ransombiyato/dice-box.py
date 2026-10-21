package io.github.ransombiyato.dicebox.sim.circuit;

import io.github.ransombiyato.dicebox.sim.CombinationalComponent;
import io.github.ransombiyato.dicebox.sim.SignalValue;
import io.github.ransombiyato.dicebox.sim.Terminal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A circuit graph: named <b>nets</b> (wires/buses of fixed width),
 * combinational components, and connections from component terminals to nets.
 *
 * <p>A {@code Circuit} is a mutable <i>structure</i>: components, nets and
 * connections can be added and removed (the topology-change API), and the
 * evaluators in this package compute signals over whatever topology exists
 * when they run. A circuit holds no signal state itself — state lives in
 * evaluation results and in the event-driven simulator.
 *
 * <p>Rules, enforced eagerly with exceptions:
 *
 * <ul>
 *   <li>Component ids, terminal names and net names must match
 *       {@code [A-Za-z0-9_-]+} (stable, serialization-safe identifiers).</li>
 *   <li>Component ids and net names are unique.</li>
 *   <li>A terminal connects to at most one net, and only to a net of equal
 *       width.</li>
 *   <li>Every component's terminal names must be unique within it.</li>
 * </ul>
 *
 * Softer structural issues (unconnected terminals, undriven nets, multiple
 * drivers) are reported by {@link #validate()} instead, because circuits are
 * built incrementally.
 *
 * <p>Nets that the outside world drives (inputs of the circuit as a whole)
 * should be marked with {@link #declareExternalInput} so validation does not
 * report them as undriven. Evaluators still accept external drives for any
 * net; the declaration exists for diagnostics.
 *
 * <p>This milestone accepts {@link CombinationalComponent}s only. Sequential
 * components get their own contract in milestone C.
 */
public final class Circuit {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private final Map<String, CombinationalComponent> components = new LinkedHashMap<>();
    private final Map<String, Integer> nets = new LinkedHashMap<>();
    private final Map<TerminalRef, String> netByTerminal = new LinkedHashMap<>();
    private final Set<String> externalInputs = new LinkedHashSet<>();

    private static void requireValidName(String what, String name) {
        Objects.requireNonNull(name, what);
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    what + " must match [A-Za-z0-9_-]+: '" + name + "'");
        }
    }

    /** Adds a net of the given bit width. */
    public void addNet(String name, int width) {
        requireValidName("net name", name);
        // Validates width range (1..64).
        SignalValue.unknown(width);
        if (nets.putIfAbsent(name, width) != null) {
            throw new IllegalArgumentException("duplicate net: " + name);
        }
    }

    /** Adds a component with a not-yet-used id. */
    public void addComponent(CombinationalComponent component) {
        Objects.requireNonNull(component, "component");
        requireValidName("component id", component.id());
        Set<String> seen = new LinkedHashSet<>();
        for (Terminal terminal : component.terminals()) {
            requireValidName("terminal name", terminal.name());
            if (!seen.add(terminal.name())) {
                throw new IllegalArgumentException(
                        "component " + component.id() + " has duplicate terminal: "
                                + terminal.name());
            }
        }
        if (components.putIfAbsent(component.id(), component) != null) {
            throw new IllegalArgumentException("duplicate component id: " + component.id());
        }
    }

    /** Connects a terminal to a net. Fails on unknown ids or width mismatch. */
    public void connect(TerminalRef terminal, String netName) {
        Objects.requireNonNull(terminal, "terminal");
        Objects.requireNonNull(netName, "netName");
        Terminal spec = terminal(terminal)
                .orElseThrow(() -> new IllegalArgumentException("unknown terminal: " + terminal));
        Integer netWidth = nets.get(netName);
        if (netWidth == null) {
            throw new IllegalArgumentException("unknown net: " + netName);
        }
        if (spec.width() != netWidth.intValue()) {
            throw new IllegalArgumentException(
                    "width mismatch: " + terminal + " is " + spec.width()
                            + " bits but net " + netName + " is " + netWidth + " bits");
        }
        if (netByTerminal.putIfAbsent(terminal, netName) != null) {
            throw new IllegalArgumentException("terminal already connected: " + terminal);
        }
    }

    /** Convenience form of {@link #connect(TerminalRef, String)}. */
    public void connect(String componentId, String terminalName, String netName) {
        connect(new TerminalRef(componentId, terminalName), netName);
    }

    /** Removes a terminal's connection. Returns whether it was connected. */
    public boolean disconnect(TerminalRef terminal) {
        Objects.requireNonNull(terminal, "terminal");
        return netByTerminal.remove(terminal) != null;
    }

    /** Removes a component and all of its connections. Returns whether it existed. */
    public boolean removeComponent(String componentId) {
        Objects.requireNonNull(componentId, "componentId");
        if (components.remove(componentId) == null) {
            return false;
        }
        netByTerminal.keySet().removeIf(ref -> ref.componentId().equals(componentId));
        return true;
    }

    /**
     * Removes a net, all connections to it, and any external-input
     * declaration. Returns whether it existed.
     */
    public boolean removeNet(String netName) {
        Objects.requireNonNull(netName, "netName");
        if (nets.remove(netName) == null) {
            return false;
        }
        netByTerminal.values().removeIf(netName::equals);
        externalInputs.remove(netName);
        return true;
    }

    /** Declares that the outside world drives this net. */
    public void declareExternalInput(String netName) {
        Objects.requireNonNull(netName, "netName");
        if (!nets.containsKey(netName)) {
            throw new IllegalArgumentException("unknown net: " + netName);
        }
        externalInputs.add(netName);
    }

    public boolean isExternalInput(String netName) {
        return externalInputs.contains(netName);
    }

    public Set<String> externalInputs() {
        return Set.copyOf(externalInputs);
    }

    /** All components, in the order they were added. */
    public List<CombinationalComponent> components() {
        return List.copyOf(components.values());
    }

    public Optional<CombinationalComponent> component(String componentId) {
        return Optional.ofNullable(components.get(componentId));
    }

    /** Net widths by name, in the order nets were added. */
    public Map<String, Integer> nets() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(nets));
    }

    /** Width of a net; fails if the net does not exist. */
    public int netWidth(String netName) {
        Integer width = nets.get(netName);
        if (width == null) {
            throw new IllegalArgumentException("unknown net: " + netName);
        }
        return width.intValue();
    }

    /** The terminal specification behind a reference, if it exists. */
    public Optional<Terminal> terminal(TerminalRef ref) {
        Objects.requireNonNull(ref, "ref");
        CombinationalComponent component = components.get(ref.componentId());
        if (component == null) {
            return Optional.empty();
        }
        return component.terminal(ref.terminalName());
    }

    /** The net a terminal is connected to, if any. */
    public Optional<String> netOf(TerminalRef ref) {
        Objects.requireNonNull(ref, "ref");
        return Optional.ofNullable(netByTerminal.get(ref));
    }

    /** All connections, terminal → net name, in connection order. */
    public Map<TerminalRef, String> connections() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(netByTerminal));
    }

    /** Terminals connected to a net, in connection order. */
    public List<TerminalRef> terminalsOnNet(String netName) {
        netWidth(netName); // validates existence
        List<TerminalRef> result = new ArrayList<>();
        for (Map.Entry<TerminalRef, String> entry : netByTerminal.entrySet()) {
            if (entry.getValue().equals(netName)) {
                result.add(entry.getKey());
            }
        }
        return List.copyOf(result);
    }

    /**
     * Structural diagnostics for the current topology. An empty list means a
     * fully connected, singly driven circuit. See {@link CircuitProblem} for
     * why these are reports rather than exceptions.
     */
    public List<CircuitProblem> validate() {
        List<CircuitProblem> problems = new ArrayList<>();
        for (CombinationalComponent component : components.values()) {
            for (Terminal terminal : component.terminals()) {
                TerminalRef ref = new TerminalRef(component.id(), terminal.name());
                if (!netByTerminal.containsKey(ref)) {
                    problems.add(new CircuitProblem(
                            CircuitProblem.Kind.UNCONNECTED_TERMINAL,
                            ref + " (" + terminal.direction() + ", " + terminal.width()
                                    + " bits) is not connected to any net"));
                }
            }
        }
        for (String netName : nets.keySet()) {
            List<TerminalRef> drivers = new ArrayList<>();
            for (TerminalRef ref : terminalsOnNet(netName)) {
                if (terminal(ref).map(Terminal::canDrive).orElse(false)) {
                    drivers.add(ref);
                }
            }
            if (drivers.isEmpty() && !externalInputs.contains(netName)) {
                problems.add(new CircuitProblem(
                        CircuitProblem.Kind.UNDRIVEN_NET,
                        "net " + netName + " has no driver and is not declared"
                                + " an external input; it will read as unknown"));
            } else if (drivers.size() > 1) {
                problems.add(new CircuitProblem(
                        CircuitProblem.Kind.MULTIPLE_DRIVERS,
                        "net " + netName + " has " + drivers.size() + " drivers " + drivers
                                + "; disagreements resolve to unknown (see NetResolver)"));
            }
        }
        return List.copyOf(problems);
    }
}
