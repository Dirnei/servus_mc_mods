package io.github.dirnei.mc.servus.storage.shuttle.module;

/**
 * Which parts a shuttle shows. Counts fall into look steps so a few upgrades
 * more change the look only now and then; the last step is open ended, so
 * packs that raise the max counts still get sensible looks. Pure Java.
 */
public final class ShuttleLook {

    /** The look of one shuttle; equal keys draw the same model. */
    public record Key(int tier, int speedStep, int handlingStep, String tool) {

        /** @param tool the tool module id, empty for none */
        public static Key of(int tier, int speed, int handling, String tool) {
            return new Key(tier, ShuttleLook.speedStep(speed), ShuttleLook.handlingStep(handling), tool);
        }
    }

    private ShuttleLook() {
    }

    /** 0 plain wheels, 1 for 1 to 2 Speed, 2 for 3 to 5, 3 for 6 or more. */
    public static int speedStep(int count) {
        if (count <= 0) {
            return 0;
        }
        if (count <= 2) {
            return 1;
        }
        return count <= 5 ? 2 : 3;
    }

    /** 0 nothing, 1 for 1 to 3 Handling, 2 for 4 or more. */
    public static int handlingStep(int count) {
        if (count <= 0) {
            return 0;
        }
        return count <= 3 ? 1 : 2;
    }

    /** Whether the tier's hold is open, so its cargo shows on top: only the Mk1 crate. */
    public static boolean openHold(int tier) {
        return tier <= 1;
    }
}
