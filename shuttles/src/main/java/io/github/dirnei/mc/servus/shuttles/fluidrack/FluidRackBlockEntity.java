package io.github.dirnei.mc.servus.shuttles.fluidrack;

import io.github.dirnei.mc.servus.foundation.block.BlockEntityItems;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidLevel;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseConnectionRule.NodeKind;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseEvent;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesBlockEntities;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

/**
 * One bay of a Fluid Rack: its share of the shelf's fluid, its wood and
 * its zone. The shelf's tank is the sum of its bays' shares (see {@link
 * FluidRackShelfHandler}); clients learn only the shelf's fluid and its
 * fill in {@code level_steps}, and only when that changes.
 */
public class FluidRackBlockEntity extends StorageNodeBlockEntity {

    /** Boards of a rack that remembers no wood. */
    public static final Identifier DEFAULT_WOOD = HeavyRackBlockEntity.DEFAULT_WOOD;

    private static final String SHARE = "Share";
    private static final String MATERIAL = "Material";
    private static final String SHOWN_FLUID = "Fluid";
    private static final String SHOWN_LEVEL = "Level";
    private static final String STEPS = "Steps";

    private final FluidSlot share = new FluidSlot(ShuttlesStatsManager.INSTANCE.fluidRackCapacityPerBay()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidRackShelfHandler handler = new FluidRackShelfHandler(this);
    /** The planks block the rack was crafted from, or null for oak. */
    private @Nullable Identifier wood;
    private boolean dropOnRemove = true;
    /** Whether a rail stood in front of this bay at the last look; not saved, set on load. */
    private boolean frontRail;
    /** Server: what watching clients were last sent; the block is sent again only when it changes. */
    private @Nullable Shown sent;
    /** Client: what the glass front shows, as the server last sent it. */
    private Shown shown = Shown.EMPTY;

    /** What clients draw: the shelf's fluid and its fill step out of {@code steps}. */
    private record Shown(Fluid fluid, int step, int steps) {

        static final Shown EMPTY = new Shown(Fluids.EMPTY, 0, 1);
    }

    public FluidRackBlockEntity(BlockPos pos, BlockState state) {
        super(ServusShuttlesBlockEntities.FLUID_RACK.get(), pos, state);
    }

    /** The bay's own share of the shelf's fluid; change it only through the shelf. */
    public FluidStack share() {
        return share.getFluid();
    }

    void setShare(FluidStack fluid) {
        share.setFluid(fluid.copy());
    }

    /** The shelf's tank as pipes, buckets and the warehouse see it, from any of its bays. */
    public FluidRackShelfHandler shelfHandler() {
        return handler;
    }

    /**
     * Splits the bays' fluid evenly over them, the remainder on the left bays.
     * Bays holding another fluid than the first one keep theirs.
     */
    static void rebalance(List<FluidRackBlockEntity> bays) {
        FluidStack fluid = FluidStack.EMPTY;
        int total = 0;
        for (FluidRackBlockEntity bay : bays) {
            FluidStack held = bay.share();
            if (held.isEmpty()) {
                continue;
            }
            if (fluid.isEmpty()) {
                fluid = held;
            } else if (!FluidStack.isSameFluidSameComponents(fluid, held)) {
                return;
            }
            total += held.getAmount();
        }
        int[] shares = FluidRackShelf.shares(total, bays.size());
        FluidStack kind = fluid.copy();
        for (int i = 0; i < bays.size(); i++) {
            bays.get(i).setShare(shares[i] > 0 ? kind.copyWithAmount(shares[i]) : FluidStack.EMPTY);
        }
    }

    /**
     * The shelf changed (its fluid, zone or bays): tells the warehouse and
     * sends watching clients the new level when its step moved.
     */
    void shelfChanged() {
        setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        network().ifPresent(network -> network.post(new WarehouseEvent.FluidChanged(worldPosition)));
        Shown now = shownNow();
        if (!now.equals(sent)) {
            sent = now;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private Shown shownNow() {
        if (level == null) {
            return Shown.EMPTY;
        }
        FluidRackShelfHandler.Content content = handler.content();
        int steps = ShuttlesStatsManager.INSTANCE.fluidRackLevelSteps();
        return new Shown(content.fluid().getFluid(), FluidLevel.step(content.fluid().getAmount(), content.capacity(), steps), steps);
    }

    /** Client: the fluid the glass front shows, empty when it shows none. */
    public FluidStack shownFluid() {
        return shown.fluid() == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(shown.fluid(), 1000);
    }

    /** Client: how full the shelf shows, from 0 to 1 in the synced steps. */
    public float shownFill() {
        return (float) shown.step() / Math.max(1, shown.steps());
    }

    /** The fluids the shelf's zone lists; empty for an unzoned shelf or one that lists none. */
    public Set<Fluid> zoneFluids() {
        return zoneFilter().fluids();
    }

    @Override
    public boolean zonable() {
        return true;
    }

    @Override
    public NodeKind kind() {
        return NodeKind.FLUID_RACK;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            frontRail = FluidRackBlock.railInFront(level, worldPosition);
        }
    }

    /** Whether the rail in front of this bay changed since the last look; remembers the new state. */
    boolean frontRailChanged() {
        boolean now = level != null && FluidRackBlock.railInFront(level, worldPosition);
        if (now == frontRail) {
            return false;
        }
        frontRail = now;
        return true;
    }

    /**
     * Whether the shelf is a tank for pipes: only while no rail stands in
     * front of its port bay. Looked up live, so a stale flag never opens a
     * docked shelf.
     */
    public boolean standalone() {
        if (level == null) {
            return true;
        }
        BlockPos port = FluidRackBlock.port(level, worldPosition);
        return port == null || !FluidRackBlock.railInFront(level, port);
    }

    /** Whether this bay carries its shelf's port. */
    public boolean isPort() {
        BlockState state = level == null ? getBlockState() : level.getBlockState(worldPosition);
        return state.getBlock() instanceof FluidRackBlock && state.getValue(FluidRackBlock.PORT);
    }

    /** Rails link and dock only on the front of the port bay. */
    @Override
    public boolean acceptsRail(Side side) {
        BlockState state = level == null ? getBlockState() : level.getBlockState(worldPosition);
        return state.getBlock() instanceof FluidRackBlock && state.getValue(FluidRackBlock.PORT)
                && side == Conversions.toSide(state.getValue(FluidRackBlock.FACING));
    }

    /** Every bay holds a rail on its front, so rails run along the whole shelf. */
    @Override
    public boolean holdsRail(Side side) {
        BlockState state = level == null ? getBlockState() : level.getBlockState(worldPosition);
        return state.getBlock() instanceof FluidRackBlock && side == Conversions.toSide(state.getValue(FluidRackBlock.FACING));
    }

    /** Another Fluid Rack is linked only when this bay joins towards it and both face the same way. */
    @Override
    public boolean linksTowards(Side side, StorageNodeBlockEntity neighbour) {
        if (!(neighbour instanceof FluidRackBlockEntity)) {
            return true;
        }
        BlockState state = getBlockState();
        BlockState other = neighbour.getBlockState();
        if (!(state.getBlock() instanceof FluidRackBlock) || !(other.getBlock() instanceof FluidRackBlock)
                || state.getValue(FluidRackBlock.FACING) != other.getValue(FluidRackBlock.FACING)) {
            return false;
        }
        Direction facing = state.getValue(FluidRackBlock.FACING);
        Direction towards = Conversions.toDirection(side);
        if (towards == facing.getCounterClockWise()) {
            return state.getValue(FluidRackBlock.LEFT);
        }
        if (towards == facing.getClockWise()) {
            return state.getValue(FluidRackBlock.RIGHT);
        }
        return false;
    }

    /** The planks block id whose texture the boards show; oak when none or unknown. */
    public Identifier wood() {
        return orOak(wood);
    }

    /** The wood a rack item carries; oak when none or unknown. */
    public static Identifier woodOf(ItemStack stack) {
        return orOak(stack.get(ServusShuttlesComponents.RACK_WOOD.get()));
    }

    /** The fluid share a rack item carries, if any. */
    public static Optional<FluidStack> fluidOf(ItemStack stack) {
        SimpleFluidContent content = stack.get(ServusShuttlesComponents.RACK_FLUID.get());
        return content == null || content.isEmpty() ? Optional.empty() : Optional.of(content.copy());
    }

    private static Identifier orOak(@Nullable Identifier id) {
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? id : DEFAULT_WOOD;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(HeavyRackBlockEntity.WOOD, wood()).build();
    }

    /** The rack item with this bay's share, wood and zone. */
    public ItemStack dropItem() {
        ItemStack drop = new ItemStack(getBlockState().getBlock());
        if (level != null) {
            BlockEntityItems.saveToItem(this, drop, level.registryAccess());
        }
        return drop;
    }

    /** Removing the bay drops no item: a creative player broke it, or it was dismantled. */
    public void dropNothing() {
        dropOnRemove = false;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        wood = input.get(ServusShuttlesComponents.RACK_WOOD.get());
        SimpleFluidContent content = input.get(ServusShuttlesComponents.RACK_FLUID.get());
        share.setFluid(content == null ? FluidStack.EMPTY : content.copy());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (wood != null) {
            builder.set(ServusShuttlesComponents.RACK_WOOD.get(), wood);
        }
        if (!share.isEmpty()) {
            builder.set(ServusShuttlesComponents.RACK_FLUID.get(), SimpleFluidContent.copyOf(share.getFluid()));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(MATERIAL);
        output.discard(SHARE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        share.save(output, SHARE);
        if (wood != null) {
            output.putString(MATERIAL, wood.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        share.load(input, SHARE);
        Identifier loaded = input.getString(MATERIAL).map(Identifier::tryParse).orElse(null);
        boolean woodChanged = !Objects.equals(loaded, wood);
        wood = loaded;
        if (input.getString(SHOWN_FLUID).isPresent()) {
            Fluid fluid = input.getString(SHOWN_FLUID).map(Identifier::tryParse).flatMap(BuiltInRegistries.FLUID::getOptional)
                    .orElse(Fluids.EMPTY);
            shown = new Shown(fluid, input.getIntOr(SHOWN_LEVEL, 0), input.getIntOr(STEPS, 1));
        }
        if (woodChanged && level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeZone(tag);
        if (wood != null) {
            tag.putString(MATERIAL, wood.toString());
        }
        Shown now = shownNow();
        sent = now;
        tag.putString(SHOWN_FLUID, BuiltInRegistries.FLUID.getKey(now.fluid()).toString());
        tag.putInt(SHOWN_LEVEL, now.step());
        tag.putInt(STEPS, now.steps());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Drops the rack item with this bay's share, wood and zone when the bay goes away, unless told not to. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && dropOnRemove) {
            dropOnRemove = false;
            Block.popResource(level, pos, dropItem());
        }
    }
}
