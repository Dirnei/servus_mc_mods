package io.github.dirnei.mc.servus.manufacturing.machine.extractor;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositDefinition;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositManager;
import io.github.dirnei.mc.servus.manufacturing.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineDefaults;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.menu.BurnerExtractorMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sits on a deposit and produces its yield while fueled. When the yield is
 * itself a furnace fuel, the first few pieces top up the fuel slot (the
 * reserve) and only the surplus goes to the output slot.
 */
public class BurnerExtractorBlockEntity extends BurnerMachineBlockEntity {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "burner_extractor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    public BurnerExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.BURNER_EXTRACTOR.get(), pos, state, 2);
        // The bottom always touches the deposit, so it is fixed Off like the front.
        sideConfig().lock(RelativeSide.BOTTOM, SideMode.DISABLED);
    }

    @Override
    protected ResourceLocation machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected int fuelSlot() {
        return SLOT_FUEL;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return false;
    }

    private Optional<DepositDefinition> depositBelow() {
        if (level == null) {
            return Optional.empty();
        }
        return DepositManager.INSTANCE.forBlock(level.getBlockState(worldPosition.below()).getBlock());
    }

    /** Whether the fuel slot wants this yield as reserve rather than letting it go to output. */
    private boolean reserveWants(ItemStack yield) {
        ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
        boolean yieldIsFuel = yield.getBurnTime(RecipeType.SMELTING) > 0;
        boolean emptyOrSame = fuel.isEmpty() || ItemStack.isSameItemSameComponents(fuel, yield);
        return FuelReserve.wantsYield(stats().fuelReserve(), yieldIsFuel, emptyOrSame, fuel.getCount());
    }

    @Override
    protected boolean canWork() {
        return depositBelow().map(deposit -> {
            ItemStack yield = deposit.copyYield();
            return reserveWants(yield) || canInsertOutput(SLOT_OUTPUT, yield);
        }).orElse(false);
    }

    @Override
    protected void finishWork() {
        depositBelow().ifPresent(deposit -> {
            ItemStack yield = deposit.copyYield();
            insertOutput(reserveWants(yield) ? SLOT_FUEL : SLOT_OUTPUT, yield);
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BurnerExtractorMenu(containerId, playerInventory, this);
    }
}
