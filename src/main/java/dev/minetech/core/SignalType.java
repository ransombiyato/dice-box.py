package dev.minetech.core;

/**
 * How a pin carries a signal.
 *
 * <p>Digital pins are 0 or 1 and settle instantly. Analog pins carry an
 * intensity from 0 to 15 like redstone, used by the discrete parts
 * (resistors, diodes, transistors, LEDs).
 */
public enum SignalType {
    DIGITAL,
    ANALOG
}
