package io.github.dirnei.mc.servus.manufacturing.machine.extractor;

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
import net.minecraft.world.level.block.state.BlockState;

/** Sits on a deposit and produces its yield while fueled. */
public class BurnerExtractorBlockEntity extends BurnerMachineBlockEntity {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "burner_extractor");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    public BurnerExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.BURNER_EXTRACTOR.get(), pos, state, 2);
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

    @Override
    protected boolean canWork() {
        return depositBelow().map(deposit -> canInsertOutput(SLOT_OUTPUT, deposit.copyYield())).orElse(false);
    }

    @Override
    protected void finishWork() {
        depositBelow().ifPresent(deposit -> insertOutput(SLOT_OUTPUT, deposit.copyYield()));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BurnerExtractorMenu(containerId, playerInventory, this);
    }
}
