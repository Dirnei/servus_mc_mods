package io.github.dirnei.mc.servus.foundation.machine.pump;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.RestartGate;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Shared by both pumps: the infinite water check below the pump, the water
 * buffer, the drain only handler offered to neighbours and the push into
 * touching fluid handlers, and the restart threshold of the buffer. Only used
 * on the server thread.
 */
public final class PumpLogic {

    private static final int PUSH_INTERVAL = 10;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final FluidSlot tank;
    private final ResourceHandler<FluidResource> drainOnly;
    private final RestartGate restart = new RestartGate();
    private boolean sourceValid;
    private boolean checkRequested = true;
    private int ticksSinceCheck;
    private int pushCooldown;

    /**
     * @param capacity the buffer size until the stats are known
     * @param changed  called when the buffer changes, usually the block entity's {@code setChanged}
     */
    public PumpLogic(int capacity, Runnable changed) {
        this.tank = new FluidSlot(capacity, stack -> stack.is(Fluids.WATER)) {
            @Override
            protected void onContentsChanged() {
                changed.run();
            }
        };
        this.drainOnly = new DrainOnlyHandler(tank);
    }

    /**
     * Locks a beam pump's sides: front, bottom and top (the upper half) Off, back and
     * sides Auto, which offers and pushes water and takes fuel.
     */
    public static void lockSides(SideConfig config) {
        config.lock(RelativeSide.BOTTOM, SideMode.DISABLED);
        config.lock(RelativeSide.TOP, SideMode.DISABLED);
        for (RelativeSide side : new RelativeSide[] {RelativeSide.BACK, RelativeSide.LEFT, RelativeSide.RIGHT}) {
            config.lock(side, SideMode.AUTO);
        }
    }

    public FluidSlot tank() {
        return tank;
    }

    public boolean sourceValid() {
        return sourceValid;
    }

    /** Whether the pump waits for its buffer to drop below the restart threshold. */
    public boolean restartWaiting() {
        return restart.waiting();
    }

    /** Checks the source again on the next tick, for example because a neighbour changed. */
    public void requestCheck() {
        checkRequested = true;
    }

    /**
     * Whether the pump can run this tick. Called once per tick by the machine. The source is read
     * again only at the start of a cycle (progress 0), at most once per work period, or when a
     * check was requested. Once the buffer cannot take a full cycle the pump waits until it drops below the
     * restart threshold.
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
        boolean bufferRoom = restart.allows(tank.getSpace() < stats.fluidPerCycle(), tank.getFluidAmount(),
                stats.fluidCapacity(), stats.restartBelow());
        return sourceValid && bufferRoom;
    }

    /** One finished cycle: water into the buffer. */
    public void finishWork(MachineStats stats) {
        tank.fill(new FluidStack(Fluids.WATER, stats.fluidPerCycle()), false);
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
            ResourceHandler<FluidResource> neighbour = level.getCapability(Capabilities.Fluid.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (neighbour == null) {
                continue;
            }
            int filled = Transfer.fill(neighbour, tank.getFluid().copy(), false);
            if (filled > 0) {
                tank.drain(filled, false);
            }
        }
    }

    /** What a neighbour sees from {@code side}: the drain only buffer, or nothing on sides that do not give out. */
    public ResourceHandler<FluidResource> sideHandler(SideMode mode) {
        return mode.allowsExtract() ? drainOnly : null;
    }

    /** Registry index of the buffered fluid, for menu sync. */
    public int fluidId() {
        return BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
    }

    /** The pump's menu data entries, or {@code fallback} for every other index. */
    public int data(int index, MachineStats stats, IntUnaryOperator fallback) {
        return switch (index) {
            case MachineBlockEntity.DATA_FLUID_AMOUNT -> tank.getFluidAmount();
            case MachineBlockEntity.DATA_FLUID_CAPACITY -> stats.fluidCapacity();
            case MachineBlockEntity.DATA_FLUID_ID -> fluidId();
            case MachineBlockEntity.DATA_SOURCE_VALID -> sourceValid ? 1 : 0;
            case MachineBlockEntity.DATA_RESTART_LEVEL -> (int) RestartGate.level(stats.fluidCapacity(), stats.restartBelow());
            default -> fallback.applyAsInt(index);
        };
    }

    public void save(ValueOutput output) {
        tank.save(output, "Water");
        output.putBoolean("SourceValid", sourceValid);
        output.putBoolean("RestartWaiting", restart.waiting());
    }

    public void load(ValueInput input) {
        tank.load(input, "Water");
        sourceValid = input.getBooleanOr("SourceValid", false);
        restart.setWaiting(input.getBooleanOr("RestartWaiting", false));
        checkRequested = true;
    }

    /** The buffer as automation sees it: water can be drained, nothing can be filled. */
    private static final class DrainOnlyHandler extends DelegatingResourceHandler<FluidResource> {

        private DrainOnlyHandler(FluidSlot tank) {
            super(tank);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isEmpty();
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
