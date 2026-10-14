package io.github.dirnei.mc.servus.alchemy;

/**
 * Salt that leaves the Calciner as Ash: whole Ash from mB, the mB that Ash takes away, and the
 * Ash Pan's room. A remainder smaller than one Ash stays behind. Pure Java.
 */
public final class AshCount {

    private AshCount() {
    }

    /** Whole Ash in {@code salt} mB when one Ash is worth {@code saltPerAsh} mB. */
    public static int ash(int salt, int saltPerAsh) {
        if (salt <= 0 || saltPerAsh <= 0) {
            return 0;
        }
        return salt / saltPerAsh;
    }

    /** The mB taking {@code ash} Ash removes, never more than whole Ash in {@code salt}. */
    public static int saltFor(int ash, int salt, int saltPerAsh) {
        return Math.max(0, Math.min(ash, ash(salt, saltPerAsh))) * Math.max(0, saltPerAsh);
    }

    /** One stack: the room of an Ash Pan whose data names none. */
    public static final int DEFAULT_ROOM = 64;

    /** The Ash Pan's room in Ash: {@code configured}, or {@link #DEFAULT_ROOM} when that is below 1. */
    public static int ashRoom(int configured) {
        return configured < 1 ? DEFAULT_ROOM : configured;
    }

    /** The Ash Pan's room in mB for {@code ashRoom} Ash, counted like {@link #ashRoom}. */
    public static int room(int ashRoom, int saltPerAsh) {
        return ashRoom(ashRoom) * Math.max(0, saltPerAsh);
    }

    /** Whether {@code extra} mB more Salt fit beside {@code salt} mB in an Ash Pan of {@code ashRoom} Ash. */
    public static boolean fits(int salt, int extra, int ashRoom, int saltPerAsh) {
        return Math.max(0, salt) + Math.max(0, extra) <= room(ashRoom, saltPerAsh);
    }
}
