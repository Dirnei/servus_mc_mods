package io.github.dirnei.mc.servus.alchemy.gnome;

/** What a gnome's hands do, synced to clients for the animation. */
public enum GnomeAction {
    /** Walking or standing, the prop held. */
    IDLE,
    /** Short taps on the cracks of his Hollow while the vein ripens. */
    TAP,
    /** Full pickaxe swings at the cave entry while the ripe vein yields. */
    DIG;

    private static final GnomeAction[] VALUES = values();

    /** The action at {@link #ordinal()}, {@link #IDLE} for an unknown index. */
    public static GnomeAction byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : IDLE;
    }
}
