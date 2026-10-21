package io.github.ransombiyato.dicebox.sim.circuit;

import java.util.Objects;

/**
 * Identifies one terminal of one component inside a {@link Circuit}:
 * {@code componentId.terminalName}, e.g. {@code and1.in0}.
 *
 * <p>This is the stable logical address of a connection point. It contains no
 * geometry: where the component physically sits is a separate concern owned
 * by the future surface-placement system.
 */
public record TerminalRef(String componentId, String terminalName) {
    public TerminalRef {
        Objects.requireNonNull(componentId, "componentId");
        Objects.requireNonNull(terminalName, "terminalName");
        if (componentId.isBlank()) {
            throw new IllegalArgumentException("componentId must not be blank");
        }
        if (terminalName.isBlank()) {
            throw new IllegalArgumentException("terminalName must not be blank");
        }
    }

    /** Parses {@code "componentId.terminalName"} (splits on the last dot). */
    public static TerminalRef parse(String text) {
        Objects.requireNonNull(text, "text");
        int dot = text.lastIndexOf('.');
        if (dot <= 0 || dot == text.length() - 1) {
            throw new IllegalArgumentException(
                    "expected 'componentId.terminalName': '" + text + "'");
        }
        return new TerminalRef(text.substring(0, dot), text.substring(dot + 1));
    }

    @Override
    public String toString() {
        return componentId + "." + terminalName;
    }
}
