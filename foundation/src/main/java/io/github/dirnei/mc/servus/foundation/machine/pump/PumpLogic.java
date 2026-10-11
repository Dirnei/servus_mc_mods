package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Shared by both pumps: the infinite water check below the pump, the water
 * buffer, the drain only handler offered to neighbours and the push into
 * touching fluid handlers. Only used on the server thread.
 */
public final class PumpLogic {

    /**
     * A pump has no side settings: front and bottom are fixed Off, every other
     * side is fixed Auto, which offers and pushes water and takes fuel.
     */
    public static void lockSides(SideConfig config) {
        config.lock(RelativeSide.BOTTOM, SideMode.DISABLED);
        for (RelativeSide side : new RelativeSide[] {RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.TOP}) {
            config.lock(side, SideMode.AUTO);
        }
    }

    private static final int PUSH_INTERVAL = 10;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final FluidTank tank;
    private final IFluidHandler drainOnly;
    private boolean sourceValid;
    /** Set on load and by neighbour changes: check the source on the next tick, whatever the progress. */
    private boolean checkRequested = true;
    private int ticksSinceCheck;
    private int pushCooldown;

    /**
     * @param capacity the buffer size until the stats are known
     * @param changed  called when the buffer changes, usually the block entity's {@code setChanged}
     */
    public PumpLogic(int capacity, Runnable changed) {
        this.tank = new FluidTank(capacity, stack -> stack.is(Fluids.WATER)) {
            @Override
            protected void onContentsChanged() {
                changed.run();
            }
        };
        this.drainOnly = new DrainOnlyHandler(tank);
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean sourceValid() {
        return sourceValid;
    }

    /** Checks the source again on the next tick, for example because a neighbour changed. */
    public void requestCheck() {
        checkRequested = true;
    }

    /**
     * Whether the pump can run this tick. Called once per tick by the machine. The source is read
     * again only at the start of a cycle (progress 0), at most once per work period, or when a
     * check was requested.
     */
    public boolean canWork(Level level, BlockPos pos, int progress, MachineStats stats) {
        tank.setCapacity(stats.fluidCapacity());
        ticksSinceCheck++;
        if (checkRequested || (progress == 0 && ticksSinceCheck >= stats.workTicks())) {
            boolean valid = checkSource(level, pos, stats.minSourceNeighbours());
            checkRequested = false;
            ticksSinceCheck = 0;
            sourceValid = valid;
        }
        return sourceValid && tank.getSpace() >= stats.fluidPerCycle();
    }

    /** One finished cycle: water into the buffer. */
    public void finishWork(MachineStats stats) {
        tank.fill(new FluidStack(Fluids.WATER, stats.fluidPerCycle()), IFluidHandler.FluidAction.EXECUTE);
    }

    /** Five block reads: the block below and its four horizontal neighbours. Nothing is written. */
    private static boolean checkSource(Level level, BlockPos pos, int minNeighbours) {
        if (level == null) {
            return false;
        }
        BlockPos below = pos.below();
        if (!isWaterSource(level.getFluidState(below))) {
            return false;
        }
        boolean[] neighbours = new boolean[HORIZONTAL.length];
        for (int i = 0; i < HORIZONTAL.length; i++) {
            neighbours[i] = isWaterSource(level.getFluidState(below.relative(HORIZONTAL[i])));
        }
        return PumpSource.isInfinite(true, neighbours, minNeighbours);
    }

    private static boolean isWaterSource(FluidState state) {
        return state.isSource() && state.is(FluidTags.WATER);
    }

    /** Every few ticks fills touching fluid handlers on sides whose mode pushes. */
    public void push(Level level, BlockPos pos, Function<Direction, SideMode> sideModes) {
        if (level == null || ++pushCooldown < PUSH_INTERVAL) {
            return;
        }
        pushCooldown = 0;
        for (Direction direction : Direction.values()) {
            if (tank.isEmpty()) {
                return;
            }
            if (!sideModes.apply(direction).pushes()) {
                continue;
            }
            IFluidHandler neighbour = level.getCapability(Capabilities.Fluid.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            int filled = neighbour.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** What a neighbour sees from {@code side}: the drain only buffer, or nothing on sides that do not give out. */
    public IFluidHandler sideHandler(SideMode mode) {
        return mode.allowsExtract() ? drainOnly : null;
    }

    /** Registry index of the buffered fluid, for menu sync. */
    public int fluidId() {
        return BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Water", tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("SourceValid", sourceValid);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        tank.readFromNBT(registries, tag.getCompound("Water"));
        sourceValid = tag.getBoolean("SourceValid");
        checkRequested = true;
    }

    /** The buffer as automation sees it: water can be drained, nothing can be filled. */
    private record DrainOnlyHandler(FluidTank tank) implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
