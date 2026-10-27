package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.save.ValueIo;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideConfig;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import io.github.dirnei.mc.servus.foundation.side.TankRole;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSideBlock;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSideMover;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidRole;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryLook;
import io.github.dirnei.mc.servus.shuttles.network.FluidActivity;
import io.github.dirnei.mc.servus.shuttles.network.FluidActivityHolder;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * The lower half of a fluid station: {@code fluid_station_tanks} buffer
 * tanks of {@code fluid_station_tank_capacity} mB, each holding one fluid,
 * behind a Fluids side layer with the front fixed and no Items layer. Both
 * halves share the layer: their sides are the station's left, right and
 * back, the upper half's top its top and the lower half's bottom its bottom.
 * It joins a warehouse only through the rail in front of it, where its port
 * sits, and its tanks are never stock. Clients learn each tank's fluid and
 * its fill in {@code level_steps} of {@code fluid_rack.json}, and only when that changes.
 */
public abstract class FluidStationBlockEntity extends StorageNodeBlockEntity implements FluidSideBlock, MenuProvider, FluidActivityHolder {

    private static final String TANK = "Tank";
    private static final String FLUID_SIDES = "FluidSides";
    private static final String SHOWN_FLUIDS = "ShownFluids";
    private static final String SHOWN_STEPS = "ShownSteps";
    private static final String STEPS = "Steps";

    private final List<FluidSlot> tanks;
    private final List<SideTank> sideTanks;
    private final FluidSides fluidSides;
    /** The Items layer a block without one reports: every side locked Off. */
    private final SideConfig noItems = new SideConfig(List.of(SideMode.DISABLED), SideMode.DISABLED);
    private final FluidStationHandler manualHandler;
    private boolean dropOnRemove = true;
    private FluidActivity activity = FluidActivity.IDLE;
    /** Client: the fluids behind the tank labels, as the server last sent them. */
    private List<Fluid> tankLabels = List.of();
    /** Server: what watching clients were last sent; the block is sent again only when it changes. */
    private @Nullable Shown sent;
    /** Client: what the sight glasses show, as the server last sent it. */
    private Shown shown;

    /** What clients draw: each tank's fluid and its fill step out of {@code steps}. */
    private record Shown(List<Fluid> fluids, List<Integer> fill, int steps) {
    }

    protected FluidStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SideMode> modes, SideMode fallback,
                                      TankRole role) {
        super(type, pos, state);
        ShuttlesStatsManager stats = ShuttlesStatsManager.INSTANCE;
        int count = stats.fluidStationTanks();
        List<FluidSlot> slots = new ArrayList<>(count);
        for (int tank = 0; tank < count; tank++) {
            slots.add(new FluidSlot(stats.fluidStationTankCapacity()) {
                @Override
                protected void onContentsChanged() {
                    contentsChanged();
                }
            });
        }
        tanks = List.copyOf(slots);
        sideTanks = Collections.nCopies(count, new SideTank(role));
        fluidSides = new FluidSides(count, modes, fallback);
        fluidSides.lock(RelativeSide.FRONT);
        for (RelativeSide side : RelativeSide.values()) {
            noItems.lock(side, SideMode.DISABLED);
        }
        shown = new Shown(Collections.nCopies(count, Fluids.EMPTY), Collections.nCopies(count, 0), 1);
        manualHandler = new FluidStationHandler(this);
    }

    /** What the station's tanks are for on a tanker run. */
    public abstract FluidRole role();

    /** The buffer tanks in index order. */
    public List<FluidSlot> tanks() {
        return tanks;
    }

    public FluidSlot tank(int tank) {
        return tanks.get(tank);
    }

    public int tankCount() {
        return tanks.size();
    }

    /** Whether every tank is empty. */
    public boolean isEmpty() {
        return tanks.stream().allMatch(FluidSlot::isEmpty);
    }

    /** The handler buckets use by hand on either half. */
    public LegacyFluidHandler manualHandler() {
        return manualHandler;
    }

    @Override
    public List<FluidSlot> sideSlots() {
        return tanks;
    }

    @Override
    public SideConfig sideConfig() {
        return noItems;
    }

    @Override
    public boolean hasItemsLayer() {
        return false;
    }

    @Override
    public FluidSides fluidSides() {
        return fluidSides;
    }

    @Override
    public List<SideTank> sideTanks() {
        return sideTanks;
    }

    @Override
    public Fluid sideTankFluid(int tank) {
        if (level != null && level.isClientSide()) {
            return SidedBlockEntity.labelFluid(tankLabels, tank);
        }
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getFluid().getFluid() : Fluids.EMPTY;
    }

    @Override
    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(FluidStationBlock.FACING) ? state.getValue(FluidStationBlock.FACING) : Direction.NORTH;
    }

    @Override
    public SideMode cycleSide(RelativeSide side) {
        return SideMode.DISABLED;
    }

    @Override
    public SideMode cycleFluidMode(RelativeSide side) {
        SideMode mode = fluidSides.cycleMode(side);
        sidesChanged();
        return mode;
    }

    @Override
    public int cycleFluidTank(RelativeSide side) {
        int tank = fluidSides.cycleTank(side);
        sidesChanged();
        return tank;
    }

    /** The upper half's position. */
    public BlockPos upperPos() {
        return worldPosition.above();
    }

    /** After a side changed: save, refresh the capability caches of both halves and show the markers. */
    private void sidesChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            level.invalidateCapabilities(upperPos());
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Called after a tank changed: save and send clients the new fill when its step moved. */
    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            sendShown();
        }
    }

    private void sendShown() {
        Shown now = shownNow();
        if (level != null && !now.equals(sent)) {
            sent = now;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private Shown shownNow() {
        int steps = ShuttlesStatsManager.INSTANCE.fluidRackLevelSteps();
        List<Fluid> fluids = new ArrayList<>(tanks.size());
        List<Integer> fill = new ArrayList<>(tanks.size());
        for (FluidSlot tank : tanks) {
            fluids.add(tank.getFluid().getFluid());
            fill.add(GantryLook.level(tank.getFluidAmount(), tank.getCapacity(), steps));
        }
        return new Shown(List.copyOf(fluids), List.copyOf(fill), steps);
    }

    /** Client: how many sight glasses the station shows, the server's tank count. */
    public int shownTanks() {
        return shown.fluids().size();
    }

    /** Client: the fluid a tank's sight glass shows, empty when it shows none. */
    public FluidStack shownFluid(int tank) {
        Fluid fluid = tank < shown.fluids().size() ? shown.fluids().get(tank) : Fluids.EMPTY;
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1000);
    }

    /** Client: how full a tank's sight glass shows, from 0 to 1 in the synced steps. */
    public float shownFill(int tank) {
        return tank < shown.fill().size() ? (float) shown.fill().get(tank) / Math.max(1, shown.steps()) : 0;
    }

    /** Server: pulls through Pull sides at {@code push_interval}, from the neighbours of both halves. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidStationBlockEntity station) {
        int interval = ShuttlesStatsManager.INSTANCE.pushInterval();
        FluidSideMover.tick(level, pos, station, interval);
        FluidSideMover.tick(level, station.upperPos(), station, interval);
    }

    @Override
    public void setFluidActivity(FluidActivity activity) {
        this.activity = activity;
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(StorageNodeBlocks.ACTIVITY) && state.getValue(StorageNodeBlocks.ACTIVITY) != activity.lamp()) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ACTIVITY, activity.lamp()),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public FluidActivity fluidActivity() {
        return activity;
    }

    /** The lang key suffix for a station without a working warehouse, empty while it is online. */
    public String offlineReason() {
        Optional<WarehouseNetwork> network = network();
        return network.isEmpty() ? "not_connected" : switch (network.get().status()) {
            case NO_CONTROLLER -> "no_controller";
            case MULTIPLE_CONTROLLERS -> "multiple_controllers";
            case ONLINE -> "";
        };
    }

    @Override
    public NodeKind kind() {
        return NodeKind.FLUID_STATION;
    }

    /** Only the front, where the port sits, takes a rail. */
    @Override
    public boolean acceptsRail(Side side) {
        return side == Conversions.toSide(facing());
    }

    /** The rail position in front of the port. */
    public BlockPos railPos() {
        return worldPosition.relative(facing());
    }

    /** What the server writes when a player opens the station's GUI: its position and its tank count. */
    public void writeOpening(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeVarInt(tanks.size());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    /** The station item with its tanks and sides. */
    public ItemStack dropItem() {
        ItemStack drop = new ItemStack(getBlockState().getBlock());
        if (level != null) {
            BlockEntityItems.saveToItem(this, drop, level.registryAccess());
        }
        return drop;
    }

    /** Removing the station drops no item: a creative player broke it. */
    public void dropNothing() {
        dropOnRemove = false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int tank = 0; tank < tanks.size(); tank++) {
            tanks.get(tank).save(output, TANK + tank);
        }
        if (!fluidSides.isDefault()) {
            ValueIo.putBytes(output, FLUID_SIDES, fluidSides.toBytes());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int tank = 0; tank < tanks.size(); tank++) {
            tanks.get(tank).load(input, TANK + tank);
        }
        fluidSides.fromBytes(ValueIo.getBytes(input, FLUID_SIDES));
        tankLabels = SidedBlockEntity.readTankLabels(input);
        input.read(SHOWN_FLUIDS, BuiltInRegistries.FLUID.byNameCodec().listOf()).ifPresent(fluids -> shown = new Shown(List.copyOf(fluids),
                input.getIntArray(SHOWN_STEPS).map(steps -> Arrays.stream(steps).boxed().toList()).orElse(List.of()),
                input.getIntOr(STEPS, 1)));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putIntArray(FLUID_SIDES, ValueIo.toInts(fluidSides.toBytes()));
        writeTankLabels(tag);
        Shown now = shownNow();
        sent = now;
        tag.store(SHOWN_FLUIDS, BuiltInRegistries.FLUID.byNameCodec().listOf(), now.fluids());
        tag.putIntArray(SHOWN_STEPS, now.fill().stream().mapToInt(Integer::intValue).toArray());
        tag.putInt(STEPS, now.steps());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Drops the station item with its tanks and sides when the lower half goes away, unless told not to. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && dropOnRemove) {
            dropOnRemove = false;
            Block.popResource(level, pos, dropItem());
        }
    }
}
