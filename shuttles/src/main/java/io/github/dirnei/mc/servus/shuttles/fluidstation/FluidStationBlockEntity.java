package io.github.dirnei.mc.servus.shuttles.fluidstation;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.material.MaterialContent;
import io.github.dirnei.mc.servus.foundation.material.MaterialHolder;
import io.github.dirnei.mc.servus.foundation.material.MaterialSlot;
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
import io.github.dirnei.mc.servus.shuttles.fluid.FluidLevel;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRole;
import io.github.dirnei.mc.servus.shuttles.network.FluidActivity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * The lower half of a fluid station: one buffer tank of
 * {@code fluid_station_tank_capacity} mB holding one fluid, behind a Fluids
 * side layer with the front fixed and no Items layer. Both halves share the
 * layer: their sides are the station's left, right and back, the upper
 * half's top its top and the lower half's bottom its bottom. It joins a
 * warehouse only through the rail in front of it, where its port sits, and
 * its tank is never stock. It remembers the wood it was crafted from and
 * copies its lamp state to the upper half, whose front shows it. Clients learn the tank's fluid and its fill
 * in {@code level_steps} of {@code fluid_rack.json}, and only when that changes.
 */
public abstract class FluidStationBlockEntity extends StorageNodeBlockEntity implements FluidSideBlock, MenuProvider, MaterialHolder {

    private static final String TANK = "Tank";
    private static final String FLUID_SIDES = "FluidSides";
    private static final String SHOWN_FLUID = "ShownFluid";
    private static final String SHOWN_STEP = "ShownStep";
    private static final String STEPS = "Steps";

    private final FluidSlot tank;
    private final List<SideTank> sideTanks;
    private final FluidSides fluidSides;
    /** The Items layer a block without one reports: every side locked Off. */
    private final SideConfig noItems = new SideConfig(List.of(SideMode.DISABLED), SideMode.DISABLED);
    private final MaterialSlot materialSlot = new MaterialSlot(this);
    private boolean dropOnRemove = true;
    private FluidActivity activity = FluidActivity.IDLE;
    /** Client: the fluid behind the tank label, as the server last sent it. */
    private List<Fluid> tankLabels = List.of();
    /** Server: what watching clients were last sent; the block is sent again only when it changes. */
    private @Nullable Shown sent;
    /** Client: what the glass shows, as the server last sent it. */
    private Shown shown = new Shown(Fluids.EMPTY, 0, 1);

    /** What clients draw: the tank's fluid and its fill step out of {@code steps}. */
    private record Shown(Fluid fluid, int step, int steps) {
    }

    protected FluidStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SideMode> modes, SideMode fallback,
                                      TankRole role) {
        super(type, pos, state);
        tank = new FluidSlot(ShuttlesStatsManager.INSTANCE.fluidStationTankCapacity(), this::tankTakes) {
            @Override
            protected void onContentsChanged() {
                contentsChanged();
            }
        };
        sideTanks = List.of(new SideTank(role));
        fluidSides = new FluidSides(1, modes, fallback);
        fluidSides.lock(RelativeSide.FRONT);
        for (RelativeSide side : RelativeSide.values()) {
            noItems.lock(side, SideMode.DISABLED);
        }
    }

    /** What the station's tank is for on a tanker run. */
    public abstract FluidRole role();

    /** Whether the tank takes this fluid at all, by hand, from a side or from a tanker; any fluid by default. */
    protected boolean tankTakes(FluidStack stack) {
        return true;
    }

    @Override
    public boolean sideTankAccepts(int index, FluidResource fluid) {
        return tankTakes(fluid.toStack(1));
    }

    /** The buffer tank; it also takes buckets by hand on either half. */
    public FluidSlot tank() {
        return tank;
    }

    public boolean isEmpty() {
        return tank.isEmpty();
    }

    @Override
    public List<FluidSlot> sideSlots() {
        return List.of(tank);
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
    public Fluid sideTankFluid(int index) {
        if (level != null && level.isClientSide()) {
            return SidedBlockEntity.labelFluid(tankLabels, index);
        }
        return tank.getFluid().getFluid();
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
        return fluidSides.cycleTank(side);
    }

    /** The upper half's position. */
    public BlockPos upperPos() {
        return worldPosition.above();
    }

    /** After a side or the keep changed: save, refresh the capability caches of both halves and show the markers. */
    protected void sidesChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            level.invalidateCapabilities(upperPos());
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Called after the tank changed: save and send clients the new fill when its step moved. */
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
        return new Shown(tank.getFluid().getFluid(), FluidLevel.step(tank.getFluidAmount(), tank.getCapacity(), steps), steps);
    }

    /** Client: the fluid the glass shows, empty when it shows none. */
    public FluidStack shownFluid() {
        return shown.fluid() == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(shown.fluid(), 1000);
    }

    /** Client: how full the glass shows, from 0 to 1 in the synced steps. */
    public float shownFill() {
        return (float) shown.step() / Math.max(1, shown.steps());
    }

    /** Server: moves fluid through Push and Pull sides at {@code push_interval}, with the neighbours of both halves. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidStationBlockEntity station) {
        int interval = ShuttlesStatsManager.INSTANCE.pushInterval();
        FluidSideMover.tick(level, pos, station, interval);
        FluidSideMover.tick(level, station.upperPos(), station, interval);
    }

    /** Copies the lamp state to the upper half, whose item station front shows the lamps. */
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (level == null || level.isClientSide() || !state.hasProperty(StorageNodeBlocks.ONLINE)) {
            return;
        }
        BlockState upper = level.getBlockState(upperPos());
        if (!(upper.getBlock() instanceof FluidStationBlock) || upper.getValue(FluidStationBlock.HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        BlockState lit = upper.setValue(StorageNodeBlocks.ONLINE, state.getValue(StorageNodeBlocks.ONLINE))
                .setValue(StorageNodeBlocks.ACTIVITY, state.getValue(StorageNodeBlocks.ACTIVITY));
        if (lit != upper) {
            level.setBlock(upperPos(), lit, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Sets the activity lamp and the reasons the GUI names; server side, set by the warehouse only. */
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

    /** The lamp and reasons as the warehouse last set them. */
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

    /** What the server writes when a player opens the station's GUI: its position. */
    public void writeOpening(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public MaterialSlot materialSlot() {
        return materialSlot;
    }

    /** The station item with its tank, sides and wood. */
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
        tank.save(output, TANK);
        if (!fluidSides.isDefault()) {
            ValueIo.putBytes(output, FLUID_SIDES, fluidSides.toBytes());
        }
        materialSlot.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, TANK);
        fluidSides.fromBytes(ValueIo.getBytes(input, FLUID_SIDES));
        materialSlot.load(input);
        tankLabels = SidedBlockEntity.readTankLabels(input);
        input.read(SHOWN_FLUID, BuiltInRegistries.FLUID.byNameCodec()).ifPresent(fluid ->
                shown = new Shown(fluid, input.getIntOr(SHOWN_STEP, 0), input.getIntOr(STEPS, 1)));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putIntArray(FLUID_SIDES, ValueIo.toInts(fluidSides.toBytes()));
        writeTankLabels(tag);
        materialSlot.save(tag);
        Shown now = shownNow();
        sent = now;
        tag.store(SHOWN_FLUID, BuiltInRegistries.FLUID.byNameCodec(), now.fluid());
        tag.putInt(SHOWN_STEP, now.step());
        tag.putInt(STEPS, now.steps());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        materialSlot.apply(input.get(MaterialContent.MATERIAL.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        materialSlot.collect(builder);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        MaterialSlot.removeFromTag(output);
    }

    @Override
    public ModelData getModelData() {
        return materialSlot.modelData();
    }

    /** Drops the station item with its tank, sides and wood when the lower half goes away, unless told not to. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && dropOnRemove) {
            dropOnRemove = false;
            Block.popResource(level, pos, dropItem());
        }
    }
}
