package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.Principles;

/** How much one drip of a Principle Dropper moves into the Hollow below. Pure Java. */
public final class PrincipleDrip {

    private PrincipleDrip() {
    }

    /**
     * The amounts of one drip: of every principle the vein's step takes, as much as the Dropper holds, the Hollow has
     * room for and one drip carries; nothing of a principle the step does not take.
     *
     * @param held     what the Dropper's tanks hold
     * @param stepCost what one ripening step of the vein takes
     * @param room     free space in each of the Hollow's tanks
     * @param perDrip  mB of each principle one drip carries at most
     */
    public static Principles plan(Principles held, Principles stepCost, Principles room, int perDrip) {
        Principles drip = Principles.NONE;
        for (Principle principle : Principle.values()) {
            if (stepCost.get(principle) > 0) {
                int amount = Math.min(Math.max(0, perDrip), Math.min(held.get(principle), room.get(principle)));
                drip = drip.with(principle, amount);
            }
        }
        return drip;
    }
}
