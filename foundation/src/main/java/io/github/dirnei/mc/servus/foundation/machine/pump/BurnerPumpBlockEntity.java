package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.menu.BurnerPumpMenu;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** Burns furnace fuel and pumps water while it stands on an infinite water source. */
public class BurnerPumpBlockEntity extends BurnerMachineBlockEntity implements PumpMachine, BeamPumpEngine {

    public static final int SLOT_FUEL = 0;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "burner_pump");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final PumpLogic pump = new PumpLogic(DEFAULTS.fluidCapacity(), this::setChanged);
    private final StrokeClock strokeClock = new StrokeClock();

    public BurnerPumpBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.BURNER_PUMP_ENTITY.get(), pos, state, 1);
        PumpLogic.lockSides(sideConfig());
    }

    @Override
    public PumpLogic pump() {
        return pump;
    }

    @Override
    public StrokeClock strokeClock() {
        return strokeClock;
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
    protected boolean canWork() {
        return pump.canWork(level, worldPosition, progress, stats());
    }

    @Override
    protected boolean restartWaiting() {
        return pump.restartWaiting();
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
        return pump.data(index, stats(), super::getData);
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
