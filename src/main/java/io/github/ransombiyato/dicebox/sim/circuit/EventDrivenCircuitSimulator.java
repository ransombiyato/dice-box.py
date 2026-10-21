package io.github.ransombiyato.dicebox.sim.circuit;

import io.github.ransombiyato.dicebox.sim.CombinationalComponent;
import io.github.ransombiyato.dicebox.sim.EventScheduler;
import io.github.ransombiyato.dicebox.sim.NetResolver;
import io.github.ransombiyato.dicebox.sim.SignalValue;
import io.github.ransombiyato.dicebox.sim.Terminal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Timed, event-driven simulation of a {@link Circuit} with a fixed gate
 * propagation delay (the TIMING_AWARE view; use
 * {@code AccuracyMode.defaultGateDelay()} or an explicit delay).
 *
 * <p>Model, stated precisely:
 *
 * <ul>
 *   <li>Every driver of a net (external input, component output) has a
 *       current driven value. A net's value is always the
 *       {@link NetResolver} resolution of its drivers.</li>
 *   <li>When a net value changes at time <i>t</i>, each component reading it
 *       is re-evaluated from the values as of <i>t</i>, and its new outputs
 *       are scheduled to be applied at <i>t + gateDelay</i>. This is a
 *       <b>transport delay</b>: outputs already in flight are not
 *       cancelled if inputs change again meanwhile.</li>
 *   <li>Events at the same time apply in scheduling order (the scheduler's
 *       FIFO rule), so runs are fully deterministic.</li>
 *   <li>{@link #runUntil} is always bounded by both a time horizon and a
 *       maximum event count — the per-tick budget mechanism for future
 *       server integration. There is deliberately no unbounded run.</li>
 * </ul>
 *
 * <p>The simulator captures the circuit's nets at construction. Change the
 * topology through the {@link Circuit} API, then create a new simulator —
 * stale events for removed nets are dropped, but new nets are not adopted.
 *
 * <p>Snapshots ({@link #snapshot}/{@link #restore}) capture a <i>quiescent</i>
 * state (no pending events): simulation time, every net value, and every
 * driver value, in a deterministic line format. In-flight events are not
 * representable in this milestone and snapshotting with events pending
 * fails loudly instead of silently dropping them.
 */
public final class EventDrivenCircuitSimulator {
    /** Result of one bounded {@link #runUntil} call. */
    public record RunOutcome(int processedEvents, long now, int pendingEvents, boolean settled) {
        public RunOutcome {
            if (processedEvents < 0 || now < 0L || pendingEvents < 0) {
                throw new IllegalArgumentException("negative value in RunOutcome");
            }
        }
    }

    private record DriveEvent(String netName, String driverKey, SignalValue value) {}

    private static final String EXTERNAL_DRIVER = "@external";
    private static final String SNAPSHOT_HEADER = "dicebox-circuit-snapshot-v1";

    private final Circuit circuit;
    private final long gateDelay;
    private final EventScheduler<DriveEvent> scheduler;
    private final Map<String, SignalValue> netValues = new LinkedHashMap<>();
    private final Map<String, Map<String, SignalValue>> driversByNet = new LinkedHashMap<>();
    private final Map<String, NetResolver.Resolution> resolutions = new LinkedHashMap<>();

    public EventDrivenCircuitSimulator(Circuit circuit, long gateDelay) {
        this(circuit, gateDelay, 0L);
    }

    private EventDrivenCircuitSimulator(Circuit circuit, long gateDelay, long startTime) {
        this.circuit = Objects.requireNonNull(circuit, "circuit");
        if (gateDelay < 0L) {
            throw new IllegalArgumentException("gateDelay must be >= 0: " + gateDelay);
        }
        this.gateDelay = gateDelay;
        this.scheduler = new EventScheduler<>(startTime);
        for (Map.Entry<String, Integer> net : circuit.nets().entrySet()) {
            netValues.put(net.getKey(), SignalValue.unknown(net.getValue()));
            driversByNet.put(net.getKey(), new LinkedHashMap<>());
            resolutions.put(net.getKey(), NetResolver.resolve(net.getValue(), List.of()));
        }
    }

    public long gateDelay() {
        return gateDelay;
    }

    public long now() {
        return scheduler.now();
    }

    public int pendingEvents() {
        return scheduler.pendingCount();
    }

    /** True when no events are waiting to be applied. */
    public boolean isSettled() {
        return scheduler.pendingCount() == 0;
    }

    /** Current value of a net known to this simulator. */
    public SignalValue netValue(String netName) {
        SignalValue value = netValues.get(netName);
        if (value == null) {
            throw new IllegalArgumentException(
                    "net not known to this simulator (added after creation?): " + netName);
        }
        return value;
    }

    /** Current values of all nets, in circuit net order. */
    public Map<String, SignalValue> netValues() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(netValues));
    }

    public Set<String> conflictedNets() {
        return collectNets(true);
    }

    public Set<String> floatingNets() {
        return collectNets(false);
    }

    private Set<String> collectNets(boolean conflicts) {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, NetResolver.Resolution> entry : resolutions.entrySet()) {
            boolean match = conflicts ? entry.getValue().conflict() : entry.getValue().floating();
            if (match) {
                result.add(entry.getKey());
            }
        }
        return Set.copyOf(result);
    }

    /**
     * Schedules an external drive of a net at the current simulation time.
     * The drive takes effect when {@link #runUntil} processes it, in order
     * with any other events at the same time.
     */
    public void setInput(String netName, SignalValue value) {
        Objects.requireNonNull(value, "value");
        int expectedWidth = circuit.netWidth(netName); // throws on unknown net
        if (!netValues.containsKey(netName)) {
            throw new IllegalArgumentException(
                    "net not known to this simulator (added after creation?): " + netName);
        }
        if (value.width() != expectedWidth) {
            throw new IllegalArgumentException(
                    "input for net " + netName + " is " + value.width()
                            + " bits, expected " + expectedWidth);
        }
        scheduler.scheduleAt(scheduler.now(), new DriveEvent(netName, EXTERNAL_DRIVER, value));
    }

    /**
     * Applies due events in order until {@code endTime} or until
     * {@code maxEvents} events have been applied, whichever comes first.
     * Time advances to {@code endTime} only if no due events remain;
     * otherwise it stops at the last applied event so a later call resumes
     * exactly where this one left off.
     */
    public RunOutcome runUntil(long endTime, int maxEvents) {
        if (endTime < scheduler.now()) {
            throw new IllegalArgumentException(
                    "endTime " + endTime + " is before now " + scheduler.now());
        }
        if (maxEvents < 1) {
            throw new IllegalArgumentException("maxEvents must be >= 1: " + maxEvents);
        }
        int processed = 0;
        while (processed < maxEvents) {
            OptionalLong nextTime = scheduler.nextEventTime();
            if (nextTime.isEmpty() || nextTime.getAsLong() > endTime) {
                break;
            }
            DriveEvent event = scheduler.runNext().orElseThrow();
            processed++;
            apply(event);
        }
        OptionalLong remaining = scheduler.nextEventTime();
        if (remaining.isEmpty() || remaining.getAsLong() > endTime) {
            // No due events left: advance across the idle remainder. This
            // call processes nothing (next event, if any, is beyond endTime).
            scheduler.runUntil(endTime, 1);
        }
        return new RunOutcome(processed, scheduler.now(), scheduler.pendingCount(), isSettled());
    }

    private void apply(DriveEvent event) {
        SignalValue current = netValues.get(event.netName());
        if (current == null) {
            return; // Net was removed after the event was scheduled: drop it.
        }
        Map<String, SignalValue> drivers = driversByNet.get(event.netName());
        SignalValue previousDrive = drivers.put(event.driverKey(), event.value());
        if (event.value().equals(previousDrive)) {
            return; // This driver already drove exactly this: net cannot change.
        }
        NetResolver.Resolution resolution =
                NetResolver.resolve(current.width(), drivers.values());
        resolutions.put(event.netName(), resolution);
        if (resolution.value().equals(current)) {
            return;
        }
        netValues.put(event.netName(), resolution.value());

        for (CombinationalComponent component : circuit.components()) {
            if (!readsNet(component, event.netName())) {
                continue;
            }
            Map<String, SignalValue> inputs =
                    ComponentSupport.inputsFor(circuit, component, netValues);
            Map<String, SignalValue> outputs =
                    ComponentSupport.evaluateChecked(component, inputs);
            for (Terminal terminal : component.terminals()) {
                if (!terminal.canDrive()) {
                    continue;
                }
                TerminalRef ref = new TerminalRef(component.id(), terminal.name());
                circuit.netOf(ref).ifPresent(outNet -> {
                    if (netValues.containsKey(outNet)) {
                        scheduler.scheduleAt(
                                scheduler.now() + gateDelay,
                                new DriveEvent(
                                        outNet,
                                        component.id() + "." + terminal.name(),
                                        outputs.get(terminal.name())));
                    }
                });
            }
        }
    }

    private boolean readsNet(CombinationalComponent component, String netName) {
        for (Terminal terminal : component.terminals()) {
            if (!terminal.canAccept()) {
                continue;
            }
            TerminalRef ref = new TerminalRef(component.id(), terminal.name());
            if (netName.equals(circuit.netOf(ref).orElse(null))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Encodes the current quiescent state. Fails if events are pending —
     * run until settled first; in-flight events are never silently dropped.
     */
    public String snapshot() {
        if (!isSettled()) {
            throw new IllegalStateException(
                    "snapshot requires a settled simulator, but " + pendingEvents()
                            + " events are pending");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(SNAPSHOT_HEADER).append('\n');
        sb.append("time ").append(now()).append('\n');
        List<String> netNames = new ArrayList<>(netValues.keySet());
        Collections.sort(netNames);
        for (String netName : netNames) {
            sb.append("net ").append(netName).append(' ')
                    .append(netValues.get(netName).encode()).append('\n');
        }
        for (String netName : netNames) {
            List<String> driverKeys = new ArrayList<>(driversByNet.get(netName).keySet());
            Collections.sort(driverKeys);
            for (String driverKey : driverKeys) {
                sb.append("drive ").append(netName).append(' ').append(driverKey).append(' ')
                        .append(driversByNet.get(netName).get(driverKey).encode()).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Recreates a simulator in a previously snapshotted quiescent state.
     * The snapshot's nets must match the given circuit's nets exactly, and
     * stored net values must agree with the resolution of stored drivers —
     * corrupt or foreign snapshots are rejected.
     */
    public static EventDrivenCircuitSimulator restore(
            Circuit circuit, long gateDelay, String snapshot) {
        Objects.requireNonNull(circuit, "circuit");
        Objects.requireNonNull(snapshot, "snapshot");
        String[] lines = snapshot.split("\\R");
        if (lines.length == 0 || !lines[0].equals(SNAPSHOT_HEADER)) {
            throw new IllegalArgumentException(
                    "unrecognized snapshot header (expected " + SNAPSHOT_HEADER + ")");
        }
        Long time = null;
        Map<String, SignalValue> storedNets = new LinkedHashMap<>();
        Map<String, Map<String, SignalValue>> storedDrivers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(" ");
            try {
                switch (parts[0]) {
                    case "time" -> {
                        if (parts.length != 2 || time != null) {
                            throw new IllegalArgumentException("bad time line: " + line);
                        }
                        time = Long.parseLong(parts[1]);
                    }
                    case "net" -> {
                        if (parts.length != 3
                                || storedNets.putIfAbsent(parts[1], SignalValue.decode(parts[2]))
                                        != null) {
                            throw new IllegalArgumentException("bad net line: " + line);
                        }
                    }
                    case "drive" -> {
                        if (parts.length != 4) {
                            throw new IllegalArgumentException("bad drive line: " + line);
                        }
                        Map<String, SignalValue> byDriver =
                                storedDrivers.computeIfAbsent(parts[1], k -> new LinkedHashMap<>());
                        if (byDriver.putIfAbsent(parts[2], SignalValue.decode(parts[3])) != null) {
                            throw new IllegalArgumentException("duplicate drive line: " + line);
                        }
                    }
                    default -> throw new IllegalArgumentException("unknown snapshot line: " + line);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("corrupt snapshot line: " + line, e);
            }
        }
        if (time == null || time.longValue() < 0L) {
            throw new IllegalArgumentException("snapshot has no valid time line");
        }
        if (!storedNets.keySet().equals(circuit.nets().keySet())) {
            throw new IllegalArgumentException(
                    "snapshot nets " + storedNets.keySet() + " do not match circuit nets "
                            + circuit.nets().keySet());
        }

        EventDrivenCircuitSimulator simulator =
                new EventDrivenCircuitSimulator(circuit, gateDelay, time.longValue());
        for (Map.Entry<String, Map<String, SignalValue>> entry : storedDrivers.entrySet()) {
            Map<String, SignalValue> target = simulator.driversByNet.get(entry.getKey());
            if (target == null) {
                throw new IllegalArgumentException(
                        "snapshot drives unknown net: " + entry.getKey());
            }
            for (SignalValue driveValue : entry.getValue().values()) {
                if (driveValue.width() != circuit.netWidth(entry.getKey())) {
                    throw new IllegalArgumentException(
                            "snapshot drive width mismatch on net " + entry.getKey());
                }
            }
            target.putAll(entry.getValue());
        }
        for (String netName : simulator.netValues.keySet()) {
            SignalValue stored = storedNets.get(netName);
            if (stored.width() != circuit.netWidth(netName)) {
                throw new IllegalArgumentException(
                        "snapshot net width mismatch on net " + netName);
            }
            NetResolver.Resolution resolution = NetResolver.resolve(
                    stored.width(), simulator.driversByNet.get(netName).values());
            if (!resolution.value().equals(stored)) {
                throw new IllegalArgumentException(
                        "inconsistent snapshot: stored value of net " + netName
                                + " does not match its stored drivers");
            }
            simulator.resolutions.put(netName, resolution);
            simulator.netValues.put(netName, resolution.value());
        }
        return simulator;
    }
}
