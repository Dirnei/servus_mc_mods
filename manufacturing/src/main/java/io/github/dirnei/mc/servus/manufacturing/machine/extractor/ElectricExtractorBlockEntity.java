package io.github.dirnei.mc.servus.manufacturing.machine.extractor;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.menu.ElectricExtractorMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Powered extractor: the deposit's yield every work period while energy lasts. */
public class ElectricExtractorBlockEntity extends PoweredMachineBlockEntity {

    public static final int SLOT_OUTPUT = 0;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "electric_extractor");
    private static final MachineStats DEFAULTS = MachineStats.powered(40, 20, 8000);

    public ElectricExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.ELECTRIC_EXTRACTOR.get(), pos, state, 1);
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

    @Override
    protected boolean canWork() {
        return DepositExtraction.depositBelow(level, worldPosition)
                .map(deposit -> canInsertOutput(SLOT_OUTPUT, deposit.copyYield()))
                .orElse(false);
    }

    @Override
    protected void finishWork() {
        DepositExtraction.depositBelow(level, worldPosition)
                .ifPresent(deposit -> insertOutput(SLOT_OUTPUT, deposit.copyYield()));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricExtractorMenu(containerId, playerInventory, this);
    }
}
