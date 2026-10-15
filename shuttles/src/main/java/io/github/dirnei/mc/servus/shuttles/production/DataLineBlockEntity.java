package io.github.dirnei.mc.servus.shuttles.production;

import io.github.dirnei.mc.servus.foundation.data.DataCarrier;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * A wooden block on the data line: shows the planks it was crafted from and
 * carries the data network to touching data nodes like a piece of cable.
 */
public abstract class DataLineBlockEntity extends BlockEntity implements WoodHolder {

    private static final int LAMP_TICKS = 20;

    private final DataCarrier carrier = new DataCarrier(this);
    private final WoodSlot woodSlot = new WoodSlot(this);
    private int lampTimer;

    protected DataLineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Whether the block carries the data network in that state; asked when it loads. */
    protected boolean carriesData(BlockState state) {
        return true;
    }

    protected final DataCarrier carrier() {
        return carrier;
    }

    /** Once a second: the state with its online lamp lit while a CPU or an online warehouse is on the line. */
    protected final BlockState withOnlineLamp(ServerLevel level, BlockState state) {
        if (++lampTimer < LAMP_TICKS || !state.hasProperty(StorageNodeBlocks.ONLINE)) {
            return state;
        }
        lampTimer = 0;
        return state.setValue(StorageNodeBlocks.ONLINE, ProductionView.online(level, worldPosition));
    }

    /** Writes the state of {@link #withOnlineLamp} when the lamp changed. */
    protected final void tickOnlineLamp(ServerLevel level, BlockState state) {
        BlockState next = withOnlineLamp(level, state);
        if (next != state) {
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        woodSlot.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        woodSlot.load(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        woodSlot.save(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        woodSlot.apply(input.get(WoodContent.WOOD.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        woodSlot.collect(builder);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        WoodSlot.removeFromTag(output);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        carrier.carry(carriesData(getBlockState()));
    }

    @Override
    public void setRemoved() {
        carrier.leave();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        carrier.leave();
    }
}
