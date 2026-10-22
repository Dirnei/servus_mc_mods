package io.github.dirnei.mc.servus.foundation.machine.sink;

/**
 * The settings of a free standing chimney column: how it smokes, when
 * redstone lets it smoke, and whether its top block smokes. Pure, no
 * Minecraft imports.
 */
public final class ChimneyRules {

    /** The smoke a column gives, in carrot order. */
    public enum Smoke { OFF, THIN, COSY, SIGNAL }

    /** When redstone lets a column smoke, in carrot order. */
    public enum Mode { ALWAYS, POWERED, UNPOWERED }

    private ChimneyRules() {
    }

    /** The setting after {@code smoke}: off, thin, cosy, signal and round again. */
    public static Smoke nextSmoke(Smoke smoke) {
        Smoke[] values = Smoke.values();
        return values[(smoke.ordinal() + 1) % values.length];
    }

    /** The mode after {@code mode}: always, while powered, while unpowered and round again. */
    public static Mode nextMode(Mode mode) {
        Mode[] values = Mode.values();
        return values[(mode.ordinal() + 1) % values.length];
    }

    /**
     * Whether the top block of a column smokes.
     *
     * @param powered whether any block of the column receives a redstone signal
     * @param onSink  whether the column stands on a formed Sink, which then controls the smoke
     */
    public static boolean smokes(Smoke smoke, Mode mode, boolean powered, boolean onSink) {
        if (onSink || smoke == Smoke.OFF) {
            return false;
        }
        return switch (mode) {
            case ALWAYS -> true;
            case POWERED -> powered;
            case UNPOWERED -> !powered;
        };
    }
}
