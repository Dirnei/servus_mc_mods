package io.github.dirnei.mc.servus.logistics.tank;

import io.github.dirnei.mc.servus.lib.network.Conversions;
import io.github.dirnei.mc.servus.lib.network.NetworkManager;
import io.github.dirnei.mc.servus.lib.network.NetworkType;
import io.github.dirnei.mc.servus.lib.network.NetworkBlockEntity;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsBlockEntities;
import io.github.dirnei.mc.servus.logistics.registry.ServusLogisticsComponents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One Fluid Tank block. Stores its share of the fluid and is a node of the
 * {@link TankNetwork}, which lays the shares of a formed tank out. Its fluid
 * handler acts on the formed tank when there is one, else on its own share.
 */
public class FluidTankBlockEntity extends NetworkBlockEntity<FluidTankBlockEntity> {

    private static final String TAG_FLUID = "fluid";
    private static final String TAG_CAPACITY = "capacity";

    private FluidStack share = FluidStack.EMPTY;
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

    /** The formed tank this block belongs to, empty when it works on its own. */
    public Optional<TankNetwork> formedTank() {
        return network().filter(TankNetwork::isFormed);
    }

    // Share

    /** This block's fluid, current with any pending change of its tank. */
    public FluidStack share() {
        network().ifPresent(TankNetwork::flush);
        return share;
    }

    /** The stored share without asking the network. For the network and the client. */
    FluidStack rawShare() {
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

    /** The content of the tank this block belongs to: the formed tank's, or its own. */
    public FluidStack tankContent() {
        return formedTank().map(TankNetwork::content).orElseGet(() -> share().copy());
    }

    /** The capacity of the tank this block belongs to. */
    public int tankCapacity() {
        return formedTank().map(TankNetwork::capacity).orElseGet(this::capacityPerBlock);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        share = tag.contains(TAG_FLUID) ? FluidStack.parseOptional(registries, tag.getCompound(TAG_FLUID)) : FluidStack.EMPTY;
        if (tag.contains(TAG_CAPACITY)) {
            clientCapacity = tag.getInt(TAG_CAPACITY);
        }
    }

    // Item components: the broken block's share travels in its item

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent content = input.get(ServusLogisticsComponents.FLUID);
        if (content != null && !content.isEmpty()) {
            setShare(content.copy());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        FluidStack current = share();
        if (!current.isEmpty()) {
            components.set(ServusLogisticsComponents.FLUID.get(), SimpleFluidContent.copyOf(current));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(TAG_FLUID);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        FluidStack current = share();
        if (!current.isEmpty()) {
            tag.put(TAG_FLUID, current.save(registries));
        }
        tag.putInt(TAG_CAPACITY, capacityPerBlock());
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
            Optional<TankNetwork> formed = formedTank();
            if (formed.isPresent()) {
                return formed.get().fill(resource, action);
            }
            if (resource.isEmpty()) {
                return 0;
            }
            FluidStack own = share();
            FluidStack group = network().map(TankNetwork::groupFluid).orElse(own);
            if (!group.isEmpty() && !FluidStack.isSameFluidSameComponents(group, resource)) {
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
            Optional<TankNetwork> formed = formedTank();
            if (formed.isPresent()) {
                return formed.get().drain(resource, action);
            }
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(share(), resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            Optional<TankNetwork> formed = formedTank();
            if (formed.isPresent()) {
                return formed.get().drain(maxDrain, action);
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
