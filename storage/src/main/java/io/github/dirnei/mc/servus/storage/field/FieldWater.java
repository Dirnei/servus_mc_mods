package io.github.dirnei.mc.servus.storage.field;

/** Water a field uses. Pure Java. */
public final class FieldWater {

    private FieldWater() {
    }

    /** The mB one interval uses for a field of that many rails; never below 1 for a field. */
    public static int use(int rails, int perRail) {
        if (rails <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) rails * perRail));
    }
}
