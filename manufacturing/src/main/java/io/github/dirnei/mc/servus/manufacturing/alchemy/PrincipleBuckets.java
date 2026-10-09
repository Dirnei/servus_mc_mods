package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Buckets and other fluid containers on the alchemy machines: a click on a principle gauge
 * with a container on the cursor fills it from that tank or pours it into that tank, whichever
 * the tank's direction allows; a click on a machine block with a filled principle container
 * pours it into the matching tank.
 */
public final class PrincipleBuckets {

    /** Menu button of the first gauge; the others follow in principle order. */
    public static final int GAUGE_BUTTON_FIRST = 2000;

    private PrincipleBuckets() {
    }

    /** The principle a menu button stands for, or null when it is not a gauge button. */
    public static Principle gaugeOf(int buttonId) {
        int offset = buttonId - GAUGE_BUTTON_FIRST;
        return offset >= 0 && offset < Principle.values().length ? Principle.values()[offset] : null;
    }

    /**
     * Fills the container on the cursor from the principle's tank, or empties it into the tank.
     * One container of a stack is used; the result replaces the cursor or goes to the inventory.
     */
    public static boolean clickGauge(Player player, AbstractContainerMenu menu, PrincipleTanks tanks, Principle principle) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            return false;
        }
        IFluidHandler tank = tanks.view(principle);
        ItemStack one = carried.copyWithCount(1);
        FluidActionResult result = FluidUtil.tryFillContainer(one, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) {
            result = FluidUtil.tryEmptyContainer(one, tank, Integer.MAX_VALUE, player, true);
        }
        if (!result.isSuccess()) {
            return false;
        }
        if (carried.getCount() == 1) {
            menu.setCarried(result.getResult());
        } else {
            carried.shrink(1);
            if (!player.getInventory().add(result.getResult())) {
                player.drop(result.getResult(), false);
            }
        }
        return true;
    }

    /** Whether the stack holds a principle, so a click on a machine should pour it in. */
    public static boolean holdsPrinciple(ItemStack stack) {
        return FluidUtil.getFluidContained(stack).map(FluidStack::getFluid).map(PrincipleFluids::principleOf).isPresent();
    }
}
