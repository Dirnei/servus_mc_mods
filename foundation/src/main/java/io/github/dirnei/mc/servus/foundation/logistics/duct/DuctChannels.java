package io.github.dirnei.mc.servus.foundation.logistics.duct;

/**
 * The eight routing channels of a duct line. Every side of a line is on one
 * channel; items and energy only travel between sides on the same channel.
 * Pure Java: colors as ARGB ints and the lang key of each channel name.
 */
public final class DuctChannels {

    /** Number of channels per line. */
    public static final int COUNT = 8;

    /** Channel colors, index 0 to 7: white, orange, magenta, light blue, yellow, lime, pink, cyan. */
    private static final int[] COLORS = {
            0xFFF0F0F0,
            0xFFF07E1E,
            0xFFC84CD2,
            0xFF4AA6E6,
            0xFFF2D43A,
            0xFF7CC82E,
            0xFFF28AB4,
            0xFF2EB8C8,
    };

    private DuctChannels() {
    }

    /** Clamps any int into the channel range 0 to {@link #COUNT} minus one. */
    public static int clamp(int channel) {
        return Math.max(0, Math.min(COUNT - 1, channel));
    }

    /** The ARGB color of a channel; out of range values are clamped. */
    public static int color(int channel) {
        return COLORS[clamp(channel)];
    }

    /** The channel number shown to players, 1 to 8. */
    public static int display(int channel) {
        return clamp(channel) + 1;
    }

    /** The lang key of a channel name, for example {@code gui.servus_logistics.channel.3}. */
    public static String nameKey(int channel) {
        return "gui.servus_logistics.channel." + display(channel);
    }
}
