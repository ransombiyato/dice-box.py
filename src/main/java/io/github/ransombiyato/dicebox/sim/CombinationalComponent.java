package io.github.ransombiyato.dicebox.sim;

import java.util.Map;

/**
 * A component whose outputs are a pure function of its current inputs
 * (gates, adders, multiplexers, ...), with no internal state.
 *
 * <p>Contract for {@link #evaluate}:
 *
 * <ul>
 *   <li>The input map contains an entry for every terminal that
 *       {@link Terminal#canAccept() can accept} a signal. Unconnected
 *       terminals are supplied as fully-unknown values by the evaluator, so
 *       implementations never need to handle missing keys — but must not
 *       fail on unknown bits.</li>
 *   <li>The returned map must contain an entry for every terminal that
 *       {@link Terminal#canDrive() can drive} a signal, each with that
 *       terminal's width.</li>
 *   <li>Evaluation must be deterministic and side-effect free: same inputs,
 *       same outputs, no hidden state, no wall-clock time, no randomness.
 *       Evaluators may call it many times while a circuit settles.</li>
 * </ul>
 *
 * Sequential (stateful) components such as latches will get their own
 * contract when they land in milestone C; circuits in this milestone accept
 * combinational components only.
 */
public interface CombinationalComponent extends SimComponent {
    /**
     * Computes output-terminal values from input-terminal values, keyed by
     * terminal name.
     */
    Map<String, SignalValue> evaluate(Map<String, SignalValue> inputsByTerminal);
}
