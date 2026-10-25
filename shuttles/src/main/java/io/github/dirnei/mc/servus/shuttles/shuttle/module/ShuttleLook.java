package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import java.util.ArrayList;
import java.util.List;

/**
 * Which parts a shuttle shows. Counts fall into look steps so a few upgrades
 * more change the look only now and then; the last step is open ended, so
 * packs that raise the max counts still get sensible looks. Pure Java.
 */
public final class ShuttleLook {

    /** Look steps of the drive: plain wheels, then three bigger drives, the last with the spoiler. */
    public static final int DRIVE_STEPS = 4;
    /** Look steps of the Handling part, the first showing nothing. */
    public static final int HANDLING_STEPS = 3;

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

    /** Whether the hold is open, so its cargo shows on top: only the Mk1 crate, never the Lumber hold with its doors. */
    public static boolean openHold(int tier, String tool) {
        return tier <= 1 && !lumber(tool);
    }

    /** Whether the tool is the Lumber module, which brings its own hold. */
    public static boolean lumber(String tool) {
        return ModuleSpec.LUMBER.equals(tool);
    }

    /** The tier, kept within Mk1 to Mk3. */
    public static int tier(Key key) {
        return Math.max(1, Math.min(3, key.tier()));
    }

    /** The drive part: {@code drive_<step>}, on a Lumber shuttle the last step's spoiler without its middle struts. */
    public static String drivePart(Key key) {
        int step = Math.min(DRIVE_STEPS - 1, key.speedStep());
        return step == DRIVE_STEPS - 1 && lumber(key.tool()) ? "drive_" + step + "_lumber" : "drive_" + step;
    }

    /**
     * The part models a shuttle is composed of: the tier's body or Lumber
     * hold, the drive, the Handling part unless it is a Lumber shuttle, and
     * the tool.
     */
    public static List<String> parts(Key key) {
        boolean lumber = lumber(key.tool());
        List<String> names = new ArrayList<>();
        names.add((lumber ? "lumber_hold_mk" : "body_mk") + tier(key));
        names.add(drivePart(key));
        if (key.handlingStep() > 0 && !lumber) {
            names.add("handling_" + Math.min(HANDLING_STEPS - 1, key.handlingStep()));
        }
        if (!key.tool().isEmpty()) {
            names.add("tool_" + key.tool());
        }
        return names;
    }
}
