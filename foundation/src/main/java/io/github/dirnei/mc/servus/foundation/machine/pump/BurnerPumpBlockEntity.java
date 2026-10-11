package io.github.dirnei.mc.servus.foundation.machine.pump;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.menu.BurnerPumpMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** Burns furnace fuel and pumps water while it stands on an infinite water source. */
public class BurnerPumpBlockEntity extends BurnerMachineBlockEntity implements PumpMachine {

    public static final int SLOT_FUEL = 0;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "burner_pump");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final PumpLogic pump = new PumpLogic(DEFAULTS.fluidCapacity(), this::setChanged);
    /** Client only: how many ticks the beam has rocked, and the frame time it was advanced to. */
    public double beamPhase;
    public double beamTime = -1;

    public BurnerPumpBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.BURNER_PUMP_ENTITY.get(), pos, state, 1);
        PumpLogic.lockSides(sideConfig());
        // The upper half of the engine sits on top: nothing passes there.
        sideConfig().lock(RelativeSide.TOP, SideMode.DISABLED);
    }

    @Override
    public PumpLogic pump() {
        return pump;
    }

    /** The fluid capability from {@code side}; null means no particular side. */
    public ResourceHandler<FluidResource> fluidHandler(Direction side) {
        return pump.sideHandler(side == null ? SideMode.AUTO : sideMode(side));
    }

    @Override
    protected Identifier machineId() {
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        pump.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pump.load(input);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BurnerPumpMenu(containerId, playerInventory, this);
    }
}
