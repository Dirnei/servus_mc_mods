package io.github.dirnei.mc.servus.foundation.logistics.tank;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.network.NetworkManager;
import io.github.dirnei.mc.servus.foundation.network.NetworkType;
import io.github.dirnei.mc.servus.foundation.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsComponents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One Fluid Tank block. Stores its share of the fluid and is a node of the
 * {@link TankNetwork}, which lays the shares of its tank out. Its fluid
 * handler acts on the tank (box) it belongs to.
 */
public class FluidTankBlockEntity extends NetworkBlockEntity<FluidTankBlockEntity> {

    private static final String TAG_FLUID = "fluid";
    private static final String TAG_CAPACITY = "capacity";
    private static final String TAG_BOX = "box";
    private static final String TAG_FLOOR = "floor";
    private Block floor = Blocks.COPPER_BLOCK;

    private FluidStack share = FluidStack.EMPTY;
    /** The tank box this block belongs to when it is part of a bigger tank, kept so a reload restores it. */
    private TankShape savedBox;
    private final TankHandler handler = new TankHandler();
    private long lastSync = Long.MIN_VALUE / 2;
    /** Capacity per block as the server told the client; the server reads the stats directly. */
    private int clientCapacity = TankStatsManager.DEFAULT_CAPACITY_PER_BLOCK;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ServusLogisticsBlockEntities.FLUID_TANK.get(), pos, state);
    }

    @Override
    public NetworkType<FluidTankBlockEntity, ?, ?> networkType() {
        return TankNetwork.TYPE;
    }

    @Override
    public FluidTankBlockEntity networkNode() {
        return this;
    }

    /** The tank network this block is part of, empty on the client and before it joined. */
    public Optional<TankNetwork> network() {
        if (!isInNetwork() || !(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return NetworkManager.of(server).set(TankNetwork.TYPE).networkAt(Conversions.toNodePos(worldPosition));
    }

    private NodePos nodePos() {
        return Conversions.toNodePos(worldPosition);
    }

    /** The network when this block is part of a tank of more than one block, else empty. */
    public Optional<TankNetwork> formedTank() {
        return network().filter(network -> network.isFormed(nodePos()));
    }

    /** The box saved with this block, or null. */
    public TankShape savedBox() {
        return savedBox;
    }

    /** Set by the network whenever the block's box changes. */
    void setSavedBox(TankShape box) {
        if (!java.util.Objects.equals(box, savedBox)) {
            savedBox = box;
            setChanged();
        }
    }

    // Share

    /** This block's fluid, current with any pending change of its tank. */
    public FluidStack share() {
        network().ifPresent(TankNetwork::flush);
        return share;
    }

    /** The stored share without asking the network. For the network and the client renderer. */
    public FluidStack rawShare() {
        return share;
    }

    /** Set by the network when it lays out a formed tank. */
    void setShareFromNetwork(FluidStack share) {
        this.share = share;
        setChanged();
    }

    /** Sets this block's share from outside the network (own handler, item, commands). */
    public void setShare(FluidStack share) {
        Optional<TankNetwork> network = network();
        network.ifPresent(TankNetwork::flush);
        this.share = share.isEmpty() ? FluidStack.EMPTY : share.copy();
        setChanged();
        if (network.isPresent()) {
            network.get().shareChangedOutside(this);
        } else {
            sendUpdate();
        }
    }

    /** Capacity of one block in mB. */
    public int capacityPerBlock() {
        if (level != null && level.isClientSide()) {
            return clientCapacity;
        }
        return TankStatsManager.INSTANCE.capacityPerBlock();
    }

    /** The content of the tank this block belongs to. */
    public FluidStack tankContent() {
        return network().map(network -> network.content(nodePos())).orElseGet(() -> share().copy());
    }

    /** The capacity of the tank this block belongs to. */
    public int tankCapacity() {
        return network().map(network -> network.capacity(nodePos())).orElseGet(this::capacityPerBlock);
    }

    public IFluidHandler fluidHandler() {
        return handler;
    }

    // Client sync

    /** Sends the share to clients unless the last update is less than {@code interval} ticks old. */
    boolean trySync(long now, int interval) {
        if (now - lastSync < interval) {
            return false;
        }
        lastSync = now;
        sendUpdate();
        return true;
    }

    private void sendUpdate() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // Save and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        FluidStack current = share();
        if (!current.isEmpty()) {
            tag.put(TAG_FLUID, current.save(registries));
        }
        tag.putString(TAG_FLOOR, BuiltInRegistries.BLOCK.getKey(floor).toString());
        if (savedBox != null) {
            tag.putIntArray(TAG_BOX, new int[] {savedBox.minX(), savedBox.minY(), savedBox.minZ(),
                    savedBox.maxX(), savedBox.maxY(), savedBox.maxZ()});
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        share = tag.contains(TAG_FLUID) ? FluidStack.parseOptional(registries, tag.getCompound(TAG_FLUID)) : FluidStack.EMPTY;
        int[] box = tag.getIntArray(TAG_BOX);
        savedBox = box.length == 6 ? TankShape.box(box[0], box[1], box[2], box[3], box[4], box[5]) : null;
        if (tag.contains(TAG_CAPACITY)) {
            clientCapacity = tag.getInt(TAG_CAPACITY);
        }
        floor = readFloor(tag);
    }

    private static Block readFloor(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_FLOOR));
        return id == null || !BuiltInRegistries.BLOCK.containsKey(id) ? Blocks.COPPER_BLOCK : BuiltInRegistries.BLOCK.get(id);
    }

    /** The block this tank block shows as its floor while nothing of its tank is below it. */
    public Block floor() {
        return floor;
    }

    public void setFloor(Block floor) {
        this.floor = floor;
        setChanged();
    }

    // Item components: the broken block's share travels in its item

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent content = input.get(ServusLogisticsComponents.FLUID);
        if (content != null && !content.isEmpty()) {
            setShare(content.copy());
        }
        Block itemFloor = input.get(ServusLogisticsComponents.TANK_FLOOR);
        if (itemFloor != null) {
            floor = itemFloor;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        FluidStack current = share();
        if (!current.isEmpty()) {
            components.set(ServusLogisticsComponents.FLUID.get(), SimpleFluidContent.copyOf(current));
        }
        components.set(ServusLogisticsComponents.TANK_FLOOR.get(), floor);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(TAG_FLUID);
        tag.remove(TAG_BOX);
        tag.remove(TAG_FLOOR);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        FluidStack current = share();
        if (!current.isEmpty()) {
            tag.put(TAG_FLUID, current.save(registries));
        }
        tag.putInt(TAG_CAPACITY, capacityPerBlock());
        tag.putString(TAG_FLOOR, BuiltInRegistries.BLOCK.getKey(floor).toString());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * The handler shown on every face. A formed tank acts on its summed
     * content; a block on its own acts on its share and accepts only the
     * fluid its connected group already holds.
     */
    private final class TankHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tankContent();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tankCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            Optional<TankNetwork> network = network();
            if (network.isPresent()) {
                return network.get().fill(nodePos(), resource, action);
            }
            if (resource.isEmpty()) {
                return 0;
            }
            FluidStack own = share();
            if (!own.isEmpty() && !FluidStack.isSameFluidSameComponents(own, resource)) {
                return 0;
            }
            int filled = Math.min(capacityPerBlock() - own.getAmount(), resource.getAmount());
            if (filled <= 0) {
                return 0;
            }
            if (action.execute()) {
                setShare(own.isEmpty() ? resource.copyWithAmount(filled) : own.copyWithAmount(own.getAmount() + filled));
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            Optional<TankNetwork> network = network();
            if (network.isPresent()) {
                return network.get().drain(nodePos(), resource, action);
            }
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(share(), resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            Optional<TankNetwork> network = network();
            if (network.isPresent()) {
                return network.get().drain(nodePos(), maxDrain, action);
            }
            FluidStack own = share();
            int drained = Math.min(maxDrain, own.getAmount());
            if (drained <= 0) {
                return FluidStack.EMPTY;
            }
            FluidStack result = own.copyWithAmount(drained);
            if (action.execute()) {
                int left = own.getAmount() - drained;
                setShare(left == 0 ? FluidStack.EMPTY : own.copyWithAmount(left));
            }
            return result;
        }
    }
}
