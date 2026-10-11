package io.github.dirnei.mc.servus.storage.production;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.component.DataComponentGetter;
import io.github.dirnei.mc.servus.foundation.data.DataCarrier;
import io.github.dirnei.mc.servus.foundation.production.DataLink;
import io.github.dirnei.mc.servus.storage.menu.ProductionTerminalMenu;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.neoforged.neoforge.model.data.ModelData;

/** The production overview on the data line. Holds nothing; it only reads the CPUs and producers of its line. */
public class ProductionTerminalBlockEntity extends BlockEntity implements WoodHolder, MenuProvider, DataLink {

    /** Carries the data network to touching data nodes, like a piece of cable. */
    private final DataCarrier carrier = new DataCarrier(this);

    private final WoodSlot woodSlot = new WoodSlot(this);

    private static final int LAMP_TICKS = 20;

    private int ticks;

    public ProductionTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.PRODUCTION_TERMINAL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ProductionTerminalBlockEntity terminal) {
        if (level instanceof ServerLevel server && ++terminal.ticks % LAMP_TICKS == 0) {
            terminal.setOnline(ProductionView.online(server, pos));
        }
    }

    /** Lights or darkens the lamp. */
    public void setOnline(boolean online) {
        BlockState state = getBlockState();
        if (level != null && state.hasProperty(StorageNodeBlocks.ONLINE) && state.getValue(StorageNodeBlocks.ONLINE) != online) {
            level.setBlock(worldPosition, state.setValue(StorageNodeBlocks.ONLINE, online), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProductionTerminalMenu(containerId, playerInventory, this);
    }

    // Wood: the planks the block was crafted from, shown as its boards

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

    // Data carrier

    @Override
    public void onLoad() {
        super.onLoad();
        carrier.join();
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
