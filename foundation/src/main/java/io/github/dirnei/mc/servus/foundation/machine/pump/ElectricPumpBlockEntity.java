package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.PoweredMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.ElectricPumpMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Spends FE and pumps water while it stands on an infinite water source. No item slots. */
public class ElectricPumpBlockEntity extends PoweredMachineBlockEntity implements PumpMachine {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "electric_pump");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final PumpLogic pump = new PumpLogic(DEFAULTS.fluidCapacity(), this::setChanged);

    public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.ELECTRIC_PUMP_ENTITY.get(), pos, state, 0);
        PumpLogic.lockSides(sideConfig());
    }

    @Override
    public PumpLogic pump() {
        return pump;
    }

    /** The fluid capability from {@code side}; null means no particular side. */
    public IFluidHandler fluidHandler(Direction side) {
        return pump.sideHandler(side == null ? SideMode.AUTO : sideMode(side));
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
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected boolean canWork() {
        return pump.canWork(level, worldPosition, progress, stats());
    }

    @Override
    protected void finishWork() {
        pump.finishWork(stats());
    }

    @Override
    protected boolean tick() {
        boolean working = super.tick();
        pump.push(level, worldPosition, this::sideMode);
        return working;
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT_FLUID;
    }

    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_FLUID_AMOUNT -> pump.tank().getFluidAmount();
            case DATA_FLUID_CAPACITY -> stats().fluidCapacity();
            case DATA_FLUID_ID -> pump.fluidId();
            case DATA_SOURCE_VALID -> pump.sourceValid() ? 1 : 0;
            default -> super.getData(index);
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        pump.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pump.load(tag, registries);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricPumpMenu(containerId, playerInventory, this);
    }
}
