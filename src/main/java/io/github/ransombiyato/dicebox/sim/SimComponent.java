package io.github.ransombiyato.dicebox.sim;

import java.util.List;
import java.util.Optional;

/**
 * Common contract for every simulated component, from a single gate to a
 * whole CPU core.
 *
 * <p>A component has a stable identifier (unique within its circuit, used for
 * diagnostics and persistence) and a fixed set of typed {@link Terminal}s.
 * Evaluation semantics differ per component kind (combinational, clocked,
 * whole emulated CPUs), so they are deliberately <i>not</i> part of this
 * interface yet; they will be formalized when the circuit graph evaluator
 * lands (docs/ROADMAP.md, milestone B/C).
 */
public interface SimComponent {
    /** Stable identifier, unique within the owning circuit. */
    String id();

    /** All terminals of this component, in a stable order. */
    List<Terminal> terminals();

    /** Looks up a terminal by name. */
    default Optional<Terminal> terminal(String name) {
        return terminals().stream().filter(t -> t.name().equals(name)).findFirst();
    }
}
