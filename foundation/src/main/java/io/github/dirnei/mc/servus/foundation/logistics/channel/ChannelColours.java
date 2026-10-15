package io.github.dirnei.mc.servus.foundation.logistics.channel;

/**
 * The 16 channel colours, one per dye colour in dye order. Black is lifted
 * to a dark grey so a black ring stays visible on the dark duct. Pure Java.
 */
public final class ChannelColours {

    /** Number of colours. */
    public static final int COUNT = 16;

    /** The lowest value of each RGB component of the black channel colour. */
    public static final int BLACK_FLOOR = 0x2A;

    private static final String[] DYES = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    };

    private static final int[] ARGB = {
            0xFFF0F0F0,
            0xFFF9801D,
            0xFFC74EBD,
            0xFF3AB3DA,
            0xFFFED83D,
            0xFF80C71F,
            0xFFF38BAA,
            0xFF60686B,
            0xFF9D9D97,
            0xFF169C9C,
            0xFF8932B8,
            0xFF3C44AA,
            0xFF835432,
            0xFF5E7C16,
            0xFFB02E26,
            0xFF000000 | BLACK_FLOOR << 16 | BLACK_FLOOR << 8 | BLACK_FLOOR,
    };

    private ChannelColours() {
    }

    public static boolean valid(int colour) {
        return colour >= 0 && colour < COUNT;
    }

    /** The ARGB value of a colour index; out of range indices give white. */
    public static int argb(int colour) {
        return ARGB[valid(colour) ? colour : 0];
    }

    /** The dye name of a colour index, for example {@code light_blue}; out of range gives white. */
    public static String dye(int colour) {
        return DYES[valid(colour) ? colour : 0];
    }
}
