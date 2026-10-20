package io.github.ransombiyato.dicebox.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static checks for whether one terminal may be connected to another.
 *
 * <p>This is the first diagnostics API of the simulation core: instead of a
 * boolean, it reports every problem it finds so tools and GUIs can tell the
 * player <i>why</i> a connection is invalid (width mismatch, nothing driving
 * the net, and so on).
 */
public final class ConnectionValidator {
    /** A single reason a connection is invalid. */
    public enum Problem {
        /** Source and sink signal widths differ. */
        WIDTH_MISMATCH,
        /** The source terminal cannot drive a signal (it is a pure input). */
        SOURCE_CANNOT_DRIVE,
        /** The sink terminal cannot accept a signal (it is a pure output). */
        SINK_CANNOT_ACCEPT
    }

    private ConnectionValidator() {}

    /**
     * Validates a connection that carries a signal from {@code source} to
     * {@code sink}. An empty result means the connection is legal.
     */
    public static List<Problem> validate(Terminal source, Terminal sink) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(sink, "sink");
        List<Problem> problems = new ArrayList<>();
        if (source.width() != sink.width()) {
            problems.add(Problem.WIDTH_MISMATCH);
        }
        if (!source.canDrive()) {
            problems.add(Problem.SOURCE_CANNOT_DRIVE);
        }
        if (!sink.canAccept()) {
            problems.add(Problem.SINK_CANNOT_ACCEPT);
        }
        return List.copyOf(problems);
    }

    public static boolean isCompatible(Terminal source, Terminal sink) {
        return validate(source, sink).isEmpty();
    }
}
