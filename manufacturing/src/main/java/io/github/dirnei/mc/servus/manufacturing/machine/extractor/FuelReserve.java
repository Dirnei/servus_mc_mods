package io.github.dirnei.mc.servus.manufacturing.machine.extractor;

/**
 * Decides whether a freshly mined yield goes into the fuel slot (the reserve)
 * or into the output slot. Pure logic, no Minecraft imports: the block entity
 * reads the three facts from the world and asks here.
 */
public final class FuelReserve {

    private FuelReserve() {
    }

    /**
     * @param target             reserve target from the machine stats, 0 disables the reserve
     * @param yieldIsFuel        whether the yield burns in a furnace
     * @param slotEmptyOrSameItem whether the fuel slot is empty or already holds the yield item
     * @param slotCount          how many items the fuel slot holds right now
     * @return true when the yield belongs in the fuel slot
     */
    public static boolean wantsYield(int target, boolean yieldIsFuel, boolean slotEmptyOrSameItem, int slotCount) {
        return target > 0 && yieldIsFuel && slotEmptyOrSameItem && slotCount < target;
    }
}
