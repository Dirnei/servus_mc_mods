package io.github.dirnei.mc.servus.storage.controller;

import io.github.dirnei.mc.servus.foundation.production.StockProvider;
import io.github.dirnei.mc.servus.foundation.production.StockSink;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.order.StockRules;
import io.github.dirnei.mc.servus.storage.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import java.util.Optional;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageBlockEntities;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import io.github.dirnei.mc.servus.foundation.wood.WoodContent;
import io.github.dirnei.mc.servus.foundation.wood.WoodHolder;
import io.github.dirnei.mc.servus.foundation.wood.WoodSlot;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Exactly one per network makes the warehouse online. Shuttles live in garages. */
public class WarehouseControllerBlockEntity extends StorageNodeBlockEntity implements WoodHolder {

    private final WoodSlot woodSlot = new WoodSlot(this);

    /** Shuttles saved by an older build, dropped as items on the first load. */
    private int legacyShuttles;
    private final StockRules rules = new StockRules();

    public WarehouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ServusStorageBlockEntities.WAREHOUSE_CONTROLLER.get(), pos, state);
        rules.onChanged(this::setChanged);
    }

    /** The stock rules of this warehouse. */
    public StockRules rules() {
        return rules;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (legacyShuttles > 0 && level instanceof ServerLevel) {
            Block.popResource(level, worldPosition, new ItemStack(ServusStorageItems.SHUTTLE_MK1.get(), legacyShuttles));
            legacyShuttles = 0;
            setChanged();
        }
    }

    private Optional<WarehouseNetwork> online() {
        return network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE);
    }

    /** The Lib stock provider view: counts from the index, instant extracts. Offline it has nothing. */
    public StockProvider stock() {
        return new StockProvider() {
            @Override
            public long count(ItemStack like) {
                return like.isEmpty() ? 0 : online().map(network -> network.index().count(ItemKey.of(like))).orElse(0L);
            }

            @Override
            public ItemStack extract(ItemStack like, int amount) {
                if (like.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                return online().map(network -> network.extract(ItemKey.of(like), amount)).orElse(ItemStack.EMPTY);
            }
        };
    }

    /** The Lib stock sink view: inserts by the warehouse's zone routing. Offline it takes nothing. */
    public StockSink sink() {
        return stack -> online().map(network -> network.insert(stack)).orElse(stack);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        woodSlot.save(tag);
        tag.put("StockRules", rules.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        woodSlot.load(tag);
        legacyShuttles = tag.getInt("Shuttles");
        if (tag.contains("StockRules")) {
            rules.load(tag.getCompound("StockRules"), registries);
        }
    }

    // Wood: the planks the block was crafted from, shown as its boards

    @Override
    public WoodSlot woodSlot() {
        return woodSlot;
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
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
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
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        WoodSlot.removeFromTag(tag);
    }

    @Override
    public ModelData getModelData() {
        return woodSlot.modelData();
    }
}
